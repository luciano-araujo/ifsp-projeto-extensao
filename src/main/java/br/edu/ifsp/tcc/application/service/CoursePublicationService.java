package br.edu.ifsp.tcc.application.service;

import br.edu.ifsp.tcc.application.dto.MaterialResponse;
import br.edu.ifsp.tcc.application.dto.PublicationCheckResponse;
import br.edu.ifsp.tcc.application.dto.PublicationProblemResponse;
import br.edu.ifsp.tcc.application.entity.Course;
import br.edu.ifsp.tcc.application.entity.CourseLesson;
import br.edu.ifsp.tcc.application.entity.CourseModule;
import br.edu.ifsp.tcc.application.entity.CourseStatus;
import br.edu.ifsp.tcc.application.entity.PublicationProblemType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Publication rules of a course: RN03 (modules, lessons with material or link) and RN06 (valid lesson links),
 * and the status change. The use cases decide who may call it.
 */
@Service
public class CoursePublicationService {

    // RN06 (product decision 7): the implementation must use exactly these patterns
    static final Pattern YOUTUBE = Pattern.compile(
            "^https://(www\\.|m\\.)?(youtube\\.com/(watch\\?v=|embed/|shorts/)|youtu\\.be/)[A-Za-z0-9_-]{11}([?&#].*)?$",
            Pattern.CASE_INSENSITIVE);
    static final Pattern VIMEO = Pattern.compile(
            "^https://(www\\.|player\\.)?vimeo\\.com/(video/)?[0-9]+([/?#].*)?$", Pattern.CASE_INSENSITIVE);

    private final CourseService courseService;
    /** Null when no LMS domain is configured: then only YouTube and Vimeo links are valid. */
    private final Pattern lms;

    public CoursePublicationService(CourseService courseService,
                                    @Value("${app.publication.lms-domains:classroom.google.com}") String lmsDomains) {
        this.courseService = courseService;
        this.lms = lmsPattern(lmsDomains);
    }

    static Pattern lmsPattern(String domains) {
        String alternatives = Arrays.stream(domains == null ? new String[0] : domains.split(","))
                .map(String::trim).filter(d -> !d.isEmpty()).map(Pattern::quote).collect(Collectors.joining("|"));
        return alternatives.isEmpty() ? null
                : Pattern.compile("^https://([a-z0-9-]+\\.)*(" + alternatives + ")(/.*)?$", Pattern.CASE_INSENSITIVE);
    }

    /** True when the link is a YouTube, Vimeo or configured LMS link (https only). */
    public boolean isValidLink(String url) {
        return YOUTUBE.matcher(url).matches() || VIMEO.matcher(url).matches()
                || (lms != null && lms.matcher(url).matches());
    }

    public PublicationCheckResponse check(Long teamId, Course course) {
        List<PublicationProblemResponse> problems = findProblems(teamId, course);
        return new PublicationCheckResponse(problems.isEmpty(), problems);
    }

    List<PublicationProblemResponse> findProblems(Long teamId, Course course) {
        List<PublicationProblemResponse> problems = new ArrayList<>();
        List<CourseModule> modules = courseService.findModules(course.getId());
        if (modules.isEmpty()) {
            problems.add(new PublicationProblemResponse(PublicationProblemType.NO_MODULES, List.of(), List.of(),
                    "O curso precisa ter pelo menos um módulo."));
            return problems;
        }
        Map<Long, List<CourseLesson>> lessonsByModule = courseService.findLessonsByModule(modules);
        Map<Long, List<MaterialResponse>> materialsByLesson = courseService.findMaterialsByLesson(teamId, lessonsByModule);

        List<CourseModule> emptyModules = modules.stream()
                .filter(m -> lessonsByModule.getOrDefault(m.getId(), List.of()).isEmpty()).toList();
        List<CourseLesson> withoutMaterial = new ArrayList<>();
        List<CourseLesson> invalidLink = new ArrayList<>();
        for (CourseModule module : modules) {
            for (CourseLesson lesson : lessonsByModule.getOrDefault(module.getId(), List.of())) {
                boolean hasLink = lesson.getContentUrl() != null && !lesson.getContentUrl().isBlank();
                if (!hasLink && !materialsByLesson.containsKey(lesson.getId())) withoutMaterial.add(lesson);
                if (hasLink && !isValidLink(lesson.getContentUrl().trim())) invalidLink.add(lesson);
            }
        }
        if (!emptyModules.isEmpty()) {
            problems.add(problem(PublicationProblemType.EMPTY_MODULE, emptyModules.stream().map(CourseModule::getId).toList(),
                    emptyModules.stream().map(CourseModule::getTitle).toList(), "Módulos sem aulas"));
        }
        if (!withoutMaterial.isEmpty()) {
            problems.add(problem(PublicationProblemType.LESSON_WITHOUT_MATERIAL,
                    withoutMaterial.stream().map(CourseLesson::getId).toList(),
                    withoutMaterial.stream().map(CourseLesson::getTitle).toList(), "Aulas sem material nem link"));
        }
        if (!invalidLink.isEmpty()) {
            problems.add(problem(PublicationProblemType.INVALID_LINK, invalidLink.stream().map(CourseLesson::getId).toList(),
                    invalidLink.stream().map(CourseLesson::getTitle).toList(),
                    "Aulas com link inválido (use YouTube, Vimeo ou um LMS permitido, com https)"));
        }
        return problems;
    }

    private PublicationProblemResponse problem(PublicationProblemType type, List<Long> ids, List<String> titles, String label) {
        return new PublicationProblemResponse(type, ids, titles, label + ": " + String.join(", ", titles) + ".");
    }

    /** Publishes the course; 400 listing what is missing when it is not ready. Already published: nothing changes. */
    public Course publish(Long teamId, Course course) {
        if (course.getStatus() == CourseStatus.PUBLISHED) return course;
        List<PublicationProblemResponse> problems = findProblems(teamId, course);
        if (!problems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O curso não pode ser publicado. "
                    + problems.stream().map(PublicationProblemResponse::getMessage).collect(Collectors.joining(" ")));
        }
        course.setStatus(CourseStatus.PUBLISHED);
        course.setPublishedAt(LocalDateTime.now());
        return courseService.saveCourse(course);
    }

    /** Back to draft: the publication date is cleared (a new publication starts the 30 days of RN04 again). */
    public Course unpublish(Course course) {
        if (course.getStatus() == CourseStatus.DRAFT) return course;
        course.setStatus(CourseStatus.DRAFT);
        course.setPublishedAt(null);
        return courseService.saveCourse(course);
    }
}
