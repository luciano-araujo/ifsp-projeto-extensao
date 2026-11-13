package br.edu.ifsp.tcc.application.service;

import br.edu.ifsp.tcc.application.dto.CourseLessonResponse;
import br.edu.ifsp.tcc.application.dto.CourseModuleResponse;
import br.edu.ifsp.tcc.application.dto.CourseResponse;
import br.edu.ifsp.tcc.application.dto.MaterialResponse;
import br.edu.ifsp.tcc.application.entity.Course;
import br.edu.ifsp.tcc.application.entity.CourseLesson;
import br.edu.ifsp.tcc.application.entity.CourseModule;
import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.LessonType;
import br.edu.ifsp.tcc.application.entity.Team;
import br.edu.ifsp.tcc.application.repository.CourseLessonRepository;
import br.edu.ifsp.tcc.application.repository.CourseModuleRepository;
import br.edu.ifsp.tcc.application.repository.CourseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

    @Mock private CourseRepository courseRepository;
    @Mock private CourseModuleRepository moduleRepository;
    @Mock private CourseLessonRepository lessonRepository;
    @Mock private KanbanItemService kanbanItemService;
    @Mock private MaterialService materialService;
    @InjectMocks private CourseService service;

    private Course course(Long id) {
        Course course = new Course();
        course.setId(id);
        return course;
    }

    private CourseModule module(Long id, Course course) {
        CourseModule module = new CourseModule();
        module.setId(id);
        module.setCourse(course);
        module.setTitle("Modulo " + id);
        module.setSortOrder(0);
        return module;
    }

    private CourseLesson lesson(Long id, CourseModule module) {
        CourseLesson lesson = new CourseLesson();
        lesson.setId(id);
        lesson.setModule(module);
        lesson.setTitle("Aula " + id);
        lesson.setType(LessonType.VIDEO);
        lesson.setPublished(true);
        lesson.setSortOrder(null);
        return lesson;
    }

    @Test
    void findCourses_shouldReturnTeamCourses() {
        List<Course> courses = List.of(course(1L));
        when(courseRepository.findByTeam_IdOrderByCreatedAtDesc(10L)).thenReturn(courses);

        assertSame(courses, service.findCourses(10L));
    }

    @Test
    void findCourse_shouldReturnCourseOfTeam() {
        Course course = course(2L);
        when(courseRepository.findByIdAndTeam_Id(2L, 10L)).thenReturn(Optional.of(course));

        assertSame(course, service.findCourse(10L, 2L));
    }

    @Test
    void findCourse_shouldReturnNotFoundWhenMissing() {
        when(courseRepository.findByIdAndTeam_Id(2L, 10L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.findCourse(10L, 2L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void saveCourse_shouldDelegateToRepository() {
        Course course = course(null);
        when(courseRepository.save(course)).thenReturn(course);

        assertSame(course, service.saveCourse(course));
    }

    @Test
    void deleteCourse_shouldDelegateToRepository() {
        Course course = course(2L);

        service.deleteCourse(course);

        verify(courseRepository).delete(course);
    }

    @Test
    void findModules_shouldReturnModulesOfCourse() {
        List<CourseModule> modules = List.of(module(3L, course(2L)));
        when(moduleRepository.findByCourse_IdOrderBySortOrderAscIdAsc(2L)).thenReturn(modules);

        assertSame(modules, service.findModules(2L));
    }

    @Test
    void findModule_shouldReturnModuleOfCourse() {
        CourseModule module = module(3L, course(2L));
        when(moduleRepository.findByIdAndCourse_Id(3L, 2L)).thenReturn(Optional.of(module));

        assertSame(module, service.findModule(2L, 3L));
    }

    @Test
    void findModule_shouldReturnNotFoundWhenMissing() {
        when(moduleRepository.findByIdAndCourse_Id(3L, 2L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.findModule(2L, 3L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void countModules_shouldReturnRepositoryCount() {
        when(moduleRepository.countByCourse_Id(2L)).thenReturn(4);

        assertEquals(4, service.countModules(2L));
    }

    @Test
    void saveModule_shouldDelegateToRepository() {
        CourseModule module = module(null, course(2L));
        when(moduleRepository.save(module)).thenReturn(module);

        assertSame(module, service.saveModule(module));
    }

    @Test
    void deleteModule_shouldDelegateToRepository() {
        CourseModule module = module(3L, course(2L));

        service.deleteModule(module);

        verify(moduleRepository).delete(module);
    }

    @Test
    void findLessonsByModule_shouldReturnEmptyMapWithoutModules() {
        assertEquals(Map.of(), service.findLessonsByModule(List.of()));
        verifyNoInteractions(lessonRepository);
    }

    @Test
    void findLessonsByModule_shouldGroupLessonsByModule() {
        CourseModule module = module(3L, course(2L));
        CourseLesson lesson = lesson(4L, module);
        when(lessonRepository.findByModule_IdInOrderBySortOrderAscIdAsc(List.of(3L))).thenReturn(List.of(lesson));

        Map<Long, List<CourseLesson>> result = service.findLessonsByModule(List.of(module));

        assertEquals(List.of(lesson), result.get(3L));
    }

    @Test
    void findLesson_shouldReturnLessonOfModule() {
        CourseLesson lesson = lesson(4L, module(3L, course(2L)));
        when(lessonRepository.findByIdAndModule_Id(4L, 3L)).thenReturn(Optional.of(lesson));

        assertSame(lesson, service.findLesson(3L, 4L));
    }

    @Test
    void findLesson_shouldReturnNotFoundWhenMissing() {
        when(lessonRepository.findByIdAndModule_Id(4L, 3L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.findLesson(3L, 4L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void countLessons_shouldReturnRepositoryCount() {
        when(lessonRepository.countByModule_Id(3L)).thenReturn(2);

        assertEquals(2, service.countLessons(3L));
    }

    @Test
    void saveLesson_shouldDelegateToRepository() {
        CourseLesson lesson = lesson(null, module(3L, course(2L)));
        when(lessonRepository.save(lesson)).thenReturn(lesson);

        assertSame(lesson, service.saveLesson(lesson));
    }

    @Test
    void deleteLesson_shouldDelegateToRepository() {
        CourseLesson lesson = lesson(4L, module(3L, course(2L)));

        service.deleteLesson(lesson);

        verify(lessonRepository).delete(lesson);
    }

    @Test
    void findProject_shouldReturnNullWithoutProjectId() {
        assertNull(service.findProject(10L, null));
        verifyNoInteractions(kanbanItemService);
    }

    @Test
    void findProject_shouldReturnProjectOfTeam() {
        KanbanItem project = new KanbanItem();
        when(kanbanItemService.findByIdAndTeamId(7L, 10L)).thenReturn(Optional.of(project));

        assertSame(project, service.findProject(10L, 7L));
    }

    @Test
    void findProject_shouldRejectProjectOfAnotherTeam() {
        when(kanbanItemService.findByIdAndTeamId(7L, 10L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.findProject(10L, 7L));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void toCourseResponse_shouldCountModules() {
        Team team = new Team();
        team.setId(10L);
        Course course = course(2L);
        course.setTeam(team);
        course.setTitle("Curso");
        when(moduleRepository.countByCourse_Id(2L)).thenReturn(3);

        CourseResponse response = service.toCourseResponse(course);

        assertEquals(2L, response.getId());
        assertEquals(10L, response.getTeamId());
        assertEquals("Curso", response.getTitle());
        assertEquals(3, response.getModulesCount());
    }

    @Test
    void toModuleResponse_shouldMapLessons() {
        CourseModule module = module(3L, course(2L));
        CourseLesson lesson = lesson(4L, module);

        CourseModuleResponse response = service.toModuleResponse(module, List.of(lesson));

        assertEquals(3L, response.getId());
        assertEquals(1, response.getLessons().size());
        assertEquals(4L, response.getLessons().get(0).getId());
    }

    @Test
    void toLessonResponse_shouldDefaultSortOrderAndPublishedFlag() {
        CourseLesson lesson = lesson(4L, module(3L, course(2L)));
        lesson.setPublished(null);

        CourseLessonResponse response = service.toLessonResponse(lesson);

        assertEquals(4L, response.getId());
        assertEquals(0, response.getSortOrder());
        assertFalse(response.getPublished());
    }

    @Test
    void toLessonResponse_shouldMapPublishedLesson() {
        CourseLesson lesson = lesson(4L, module(3L, course(2L)));
        lesson.setSortOrder(2);

        CourseLessonResponse response = service.toLessonResponse(lesson);

        assertTrue(response.getPublished());
        assertEquals(2, response.getSortOrder());
    }

    @Test
    void toLessonResponse_shouldHaveNoMaterialsByDefault() {
        CourseLesson lesson = lesson(4L, module(3L, course(2L)));

        assertTrue(service.toLessonResponse(lesson).getMaterials().isEmpty());
    }

    @Test
    void toModuleResponse_shouldAttachTheMaterialsOfEachLesson() {
        CourseModule module = module(3L, course(2L));
        CourseLesson withMaterial = lesson(4L, module);
        CourseLesson without = lesson(5L, module);
        MaterialResponse material = mock(MaterialResponse.class);

        CourseModuleResponse response = service.toModuleResponse(module, List.of(withMaterial, without),
                Map.of(4L, List.of(material)));

        assertEquals(List.of(material), response.getLessons().get(0).getMaterials());
        assertTrue(response.getLessons().get(1).getMaterials().isEmpty());
    }

    @Test
    void findMaterialsByLesson_shouldLoadTheMaterialsOfAllTheLessonsInOneCall() {
        CourseModule module = module(3L, course(2L));
        Map<Long, List<CourseLesson>> lessons = Map.of(3L, List.of(lesson(4L, module), lesson(5L, module)));
        Map<Long, List<MaterialResponse>> materials = Map.of(4L, List.of(mock(MaterialResponse.class)));
        when(materialService.findResponsesByLesson(eq(10L), argThat(ids -> List.copyOf(ids).equals(List.of(4L, 5L)))))
                .thenReturn(materials);

        assertSame(materials, service.findMaterialsByLesson(10L, lessons));
    }

    @Test
    void toLessonResponseWithMaterials_shouldLoadTheMaterialsOfTheLesson() {
        CourseLesson lesson = lesson(4L, module(3L, course(2L)));
        MaterialResponse material = mock(MaterialResponse.class);
        when(materialService.findResponsesByLesson(10L, List.of(4L))).thenReturn(Map.of(4L, List.of(material)));

        assertEquals(List.of(material), service.toLessonResponseWithMaterials(10L, lesson).getMaterials());
    }

    @Test
    void toLessonResponseWithMaterials_shouldBeEmptyWhenNothingIsLinked() {
        CourseLesson lesson = lesson(4L, module(3L, course(2L)));
        when(materialService.findResponsesByLesson(10L, List.of(4L))).thenReturn(Map.of());

        assertTrue(service.toLessonResponseWithMaterials(10L, lesson).getMaterials().isEmpty());
    }

    @Test
    void toDetailResponse_shouldReturnTheTreeWithStatusAndLessonFields() {
        Course course = course(2L);
        course.setTitle("Curso");
        course.setStatus(br.edu.ifsp.tcc.application.entity.CourseStatus.PUBLISHED);
        CourseModule module = module(3L, course);
        CourseLesson lesson = lesson(4L, module);
        lesson.setDescription("Desc");
        lesson.setContentUrl("https://vimeo.com/1");
        when(moduleRepository.findByCourse_IdOrderBySortOrderAscIdAsc(2L)).thenReturn(List.of(module));
        when(lessonRepository.findByModule_IdInOrderBySortOrderAscIdAsc(List.of(3L))).thenReturn(List.of(lesson));
        when(materialService.findResponsesByLesson(10L, List.of(4L))).thenReturn(Map.of());

        var detail = service.toDetailResponse(10L, course);

        assertEquals(br.edu.ifsp.tcc.application.entity.CourseStatus.PUBLISHED, detail.getStatus());
        assertEquals(1, detail.getModulesCount());
        assertEquals("Desc", detail.getModules().get(0).getLessons().get(0).getDescription());
        assertEquals("https://vimeo.com/1", detail.getModules().get(0).getLessons().get(0).getContentUrl());
    }

    @Test
    void toModuleResponses_shouldBeEmptyWithoutModules() {
        assertTrue(service.toModuleResponses(10L, List.of()).isEmpty());
    }

    @Test
    void findLessons_shouldReadTheLessonsOfOneModule() {
        CourseLesson lesson = lesson(4L, module(3L, course(2L)));
        when(lessonRepository.findByModule_IdInOrderBySortOrderAscIdAsc(List.of(3L))).thenReturn(List.of(lesson));

        assertEquals(List.of(lesson), service.findLessons(3L));
    }

    @Test
    void requireExactIds_shouldAcceptTheSameIdsInAnyOrder() {
        assertDoesNotThrow(() -> service.requireExactIds(List.of(3L, 1L, 2L), List.of(1L, 2L, 3L), "x"));
    }

    @Test
    void requireExactIds_shouldRefuseMissingExtraOrRepeatedIds() {
        for (List<Long> requested : List.of(List.of(1L, 2L), List.of(1L, 2L, 3L, 4L), List.of(1L, 1L, 2L), List.of(1L, 2L, 9L))) {
            ResponseStatusException e = assertThrows(ResponseStatusException.class,
                    () -> service.requireExactIds(requested, List.of(1L, 2L, 3L), "mensagem"));
            assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
            assertEquals("mensagem", e.getReason());
        }
    }

    @Test
    void blankToNull_shouldTrimOrClear() {
        assertNull(CourseService.blankToNull(null));
        assertNull(CourseService.blankToNull("  "));
        assertEquals("a", CourseService.blankToNull(" a "));
    }
}
