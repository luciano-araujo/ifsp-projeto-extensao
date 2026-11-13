package br.edu.ifsp.tcc.application.service;

import br.edu.ifsp.tcc.application.dto.MaterialResponse;
import br.edu.ifsp.tcc.application.dto.PublicationCheckResponse;
import br.edu.ifsp.tcc.application.dto.PublicationProblemResponse;
import br.edu.ifsp.tcc.application.entity.Course;
import br.edu.ifsp.tcc.application.entity.CourseLesson;
import br.edu.ifsp.tcc.application.entity.CourseModule;
import br.edu.ifsp.tcc.application.entity.CourseStatus;
import br.edu.ifsp.tcc.application.entity.PublicationProblemType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CoursePublicationServiceTest {

    @Mock private CourseService courseService;

    private CoursePublicationService service(String domains) {
        return new CoursePublicationService(courseService, domains);
    }

    private Course course(CourseStatus status) {
        Course course = new Course();
        course.setId(20L);
        course.setStatus(status);
        return course;
    }

    private CourseModule module(Long id, String title) {
        CourseModule module = new CourseModule();
        module.setId(id);
        module.setTitle(title);
        return module;
    }

    private CourseLesson lesson(Long id, String title, String url) {
        CourseLesson lesson = new CourseLesson();
        lesson.setId(id);
        lesson.setTitle(title);
        lesson.setContentUrl(url);
        return lesson;
    }

    private void tree(List<CourseModule> modules, Map<Long, List<CourseLesson>> lessons, Map<Long, List<MaterialResponse>> materials) {
        when(courseService.findModules(20L)).thenReturn(modules);
        when(courseService.findLessonsByModule(modules)).thenReturn(lessons);
        when(courseService.findMaterialsByLesson(10L, lessons)).thenReturn(materials);
    }

    @Test
    void isValidLink_shouldAcceptTheRn06Examples() {
        CoursePublicationService s = service("classroom.google.com, moodle.ifsp.edu.br");

        assertTrue(s.isValidLink("https://www.youtube.com/watch?v=dQw4w9WgXcQ"));
        assertTrue(s.isValidLink("https://youtu.be/dQw4w9WgXcQ?t=10"));
        assertTrue(s.isValidLink("HTTPS://YOUTUBE.COM/shorts/dQw4w9WgXcQ"));
        assertTrue(s.isValidLink("https://m.youtube.com/embed/dQw4w9WgXcQ"));
        assertTrue(s.isValidLink("https://vimeo.com/123456789"));
        assertTrue(s.isValidLink("https://player.vimeo.com/video/123456?h=abc"));
        assertTrue(s.isValidLink("https://classroom.google.com/c/abc"));
        assertTrue(s.isValidLink("https://aulas.moodle.ifsp.edu.br/course/view.php?id=2"));
    }

    @Test
    void isValidLink_shouldRefuseOthers() {
        CoursePublicationService s = service("classroom.google.com");

        assertFalse(s.isValidLink("http://www.youtube.com/watch?v=dQw4w9WgXcQ"));
        assertFalse(s.isValidLink("https://www.youtube.com/watch?v=curto"));
        assertFalse(s.isValidLink("https://vimeo.com/abc"));
        assertFalse(s.isValidLink("https://evilclassroom.google.com/x"));
        assertFalse(s.isValidLink("https://classroom.google.com.evil.com/x"));
        assertFalse(s.isValidLink("https://exemplo.com/video"));
        assertFalse(s.isValidLink("https://moodle.ifsp.edu.br/x"));
    }

    @Test
    void isValidLink_shouldHaveNoLmsWhenNoDomainIsConfigured() {
        assertFalse(service(" , ").isValidLink("https://classroom.google.com/c/abc"));
        assertFalse(service(null).isValidLink("https://classroom.google.com/c/abc"));
        assertTrue(service(null).isValidLink("https://vimeo.com/1"));
    }

    @Test
    void check_shouldReportCourseWithoutModules() {
        when(courseService.findModules(20L)).thenReturn(List.of());

        PublicationCheckResponse check = service("a.com").check(10L, course(CourseStatus.DRAFT));

        assertFalse(check.isPublishable());
        assertEquals(1, check.getProblems().size());
        assertEquals(PublicationProblemType.NO_MODULES, check.getProblems().get(0).getType());
        assertTrue(check.getProblems().get(0).getIds().isEmpty());
    }

    @Test
    void check_shouldReportEmptyModulesLessonsWithoutMaterialAndInvalidLinks() {
        CourseModule empty = module(1L, "Vazio");
        CourseModule full = module(2L, "Cheio");
        CourseLesson noMaterial = lesson(10L, "Sem nada", null);
        CourseLesson blankLink = lesson(11L, "Link vazio", "  ");
        CourseLesson withMaterial = lesson(12L, "Com arquivo", null);
        CourseLesson badLink = lesson(13L, "Link ruim", "https://exemplo.com/x");
        CourseLesson goodLink = lesson(14L, "Link bom", "https://vimeo.com/99");
        tree(List.of(empty, full), Map.of(2L, List.of(noMaterial, blankLink, withMaterial, badLink, goodLink)),
                Map.of(12L, List.of(mock(MaterialResponse.class))));

        PublicationCheckResponse check = service("classroom.google.com").check(10L, course(CourseStatus.DRAFT));

        assertFalse(check.isPublishable());
        assertEquals(3, check.getProblems().size());
        PublicationProblemResponse emptyModule = check.getProblems().get(0);
        assertEquals(PublicationProblemType.EMPTY_MODULE, emptyModule.getType());
        assertEquals(List.of(1L), emptyModule.getIds());
        assertEquals(List.of("Vazio"), emptyModule.getTitles());
        PublicationProblemResponse noMat = check.getProblems().get(1);
        assertEquals(PublicationProblemType.LESSON_WITHOUT_MATERIAL, noMat.getType());
        assertEquals(List.of(10L, 11L), noMat.getIds());
        PublicationProblemResponse invalid = check.getProblems().get(2);
        assertEquals(PublicationProblemType.INVALID_LINK, invalid.getType());
        assertEquals(List.of(13L), invalid.getIds());
        assertTrue(invalid.getMessage().contains("Link ruim"));
    }

    @Test
    void check_shouldBePublishableWhenEverythingIsFine() {
        CourseModule module = module(1L, "M");
        tree(List.of(module), Map.of(1L, List.of(lesson(10L, "A", "https://youtu.be/dQw4w9WgXcQ"), lesson(11L, "B", null))),
                Map.of(11L, List.of(mock(MaterialResponse.class))));

        PublicationCheckResponse check = service("classroom.google.com").check(10L, course(CourseStatus.DRAFT));

        assertTrue(check.isPublishable());
        assertTrue(check.getProblems().isEmpty());
    }

    @Test
    void publish_shouldSetStatusAndDate() {
        CourseModule module = module(1L, "M");
        tree(List.of(module), Map.of(1L, List.of(lesson(10L, "A", "https://vimeo.com/5"))), Map.of());
        Course course = course(CourseStatus.DRAFT);
        when(courseService.saveCourse(course)).thenReturn(course);

        Course saved = service("a.com").publish(10L, course);

        assertEquals(CourseStatus.PUBLISHED, saved.getStatus());
        assertNotNull(saved.getPublishedAt());
    }

    @Test
    void publish_shouldRefuseWithTheProblemsInTheMessage() {
        when(courseService.findModules(20L)).thenReturn(List.of());
        Course course = course(CourseStatus.DRAFT);

        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> service("a.com").publish(10L, course));

        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
        assertTrue(e.getReason().startsWith("O curso não pode ser publicado."));
        assertTrue(e.getReason().contains("pelo menos um módulo"));
        assertEquals(CourseStatus.DRAFT, course.getStatus());
        verify(courseService, never()).saveCourse(any());
    }

    @Test
    void publish_shouldKeepTheDateOfAnAlreadyPublishedCourse() {
        Course course = course(CourseStatus.PUBLISHED);

        assertSame(course, service("a.com").publish(10L, course));

        verifyNoInteractions(courseService);
    }

    @Test
    void unpublish_shouldGoBackToDraftAndClearTheDate() {
        Course course = course(CourseStatus.PUBLISHED);
        course.setPublishedAt(java.time.LocalDateTime.now());
        when(courseService.saveCourse(course)).thenReturn(course);

        Course saved = service("a.com").unpublish(course);

        assertEquals(CourseStatus.DRAFT, saved.getStatus());
        assertNull(saved.getPublishedAt());
    }

    @Test
    void unpublish_shouldDoNothingOnADraft() {
        Course course = course(CourseStatus.DRAFT);

        assertSame(course, service("a.com").unpublish(course));

        verifyNoInteractions(courseService);
    }
}
