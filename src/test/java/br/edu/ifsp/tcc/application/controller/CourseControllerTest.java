package br.edu.ifsp.tcc.application.controller;

import br.edu.ifsp.tcc.application.dto.*;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.usecase.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseControllerTest {

    @Mock private ListCoursesUseCase listCoursesUseCase;
    @Mock private GetCourseUseCase getCourseUseCase;
    @Mock private CreateCourseUseCase createCourseUseCase;
    @Mock private DeleteCourseUseCase deleteCourseUseCase;
    @Mock private CreateCourseModuleUseCase createCourseModuleUseCase;
    @Mock private DeleteCourseModuleUseCase deleteCourseModuleUseCase;
    @Mock private CreateCourseLessonUseCase createCourseLessonUseCase;
    @Mock private UpdateCourseLessonUseCase updateCourseLessonUseCase;
    @Mock private DeleteCourseLessonUseCase deleteCourseLessonUseCase;
    @Mock private LinkLessonMaterialUseCase linkLessonMaterialUseCase;
    @Mock private UnlinkLessonMaterialUseCase unlinkLessonMaterialUseCase;
    @Mock private UpdateCourseUseCase updateCourseUseCase;
    @Mock private UpdateCourseModuleUseCase updateCourseModuleUseCase;
    @Mock private ReorderCourseModulesUseCase reorderCourseModulesUseCase;
    @Mock private ReorderCourseLessonsUseCase reorderCourseLessonsUseCase;
    @Mock private GetPublicationCheckUseCase getPublicationCheckUseCase;
    @Mock private PublishCourseUseCase publishCourseUseCase;
    @Mock private UnpublishCourseUseCase unpublishCourseUseCase;
    @Mock private ExportCourseUseCase exportCourseUseCase;
    @InjectMocks private CourseController controller;

    @Test
    void update_shouldReturnTheCourse() {
        UpdateCourseDTO dto = new UpdateCourseDTO();
        CourseResponse updated = mock(CourseResponse.class);
        when(updateCourseUseCase.execute(10L, 1L, 2L, dto)).thenReturn(updated);

        assertSame(updated, controller.update(user, 10L, 2L, dto).getBody());
    }

    @Test
    void updateModule_shouldReturnTheModule() {
        UpdateModuleDTO dto = new UpdateModuleDTO();
        CourseModuleResponse updated = mock(CourseModuleResponse.class);
        when(updateCourseModuleUseCase.execute(10L, 1L, 2L, 3L, dto)).thenReturn(updated);

        assertSame(updated, controller.updateModule(user, 10L, 2L, 3L, dto).getBody());
    }

    @Test
    void reorderModules_shouldReturnTheOrderedList() {
        ReorderModulesDTO dto = new ReorderModulesDTO();
        List<CourseModuleResponse> ordered = List.of(mock(CourseModuleResponse.class));
        when(reorderCourseModulesUseCase.execute(10L, 1L, 2L, dto)).thenReturn(ordered);

        assertSame(ordered, controller.reorderModules(user, 10L, 2L, dto).getBody());
    }

    @Test
    void reorderLessons_shouldReturnTheOrderedList() {
        ReorderLessonsDTO dto = new ReorderLessonsDTO();
        List<CourseLessonResponse> ordered = List.of(mock(CourseLessonResponse.class));
        when(reorderCourseLessonsUseCase.execute(10L, 1L, 2L, 3L, dto)).thenReturn(ordered);

        assertSame(ordered, controller.reorderLessons(user, 10L, 2L, 3L, dto).getBody());
    }

    @Test
    void publicationCheck_publish_unpublish_shouldDelegate() {
        PublicationCheckResponse check = mock(PublicationCheckResponse.class);
        CourseResponse published = mock(CourseResponse.class);
        CourseResponse draft = mock(CourseResponse.class);
        when(getPublicationCheckUseCase.execute(10L, 1L, 2L)).thenReturn(check);
        when(publishCourseUseCase.execute(10L, 1L, 2L)).thenReturn(published);
        when(unpublishCourseUseCase.execute(10L, 1L, 2L)).thenReturn(draft);

        assertSame(check, controller.publicationCheck(user, 10L, 2L).getBody());
        assertSame(published, controller.publish(user, 10L, 2L).getBody());
        assertSame(draft, controller.unpublish(user, 10L, 2L).getBody());
    }

    @Test
    void export_shouldSetContentTypeAndDisposition() {
        byte[] csv = {1};
        when(exportCourseUseCase.execute(10L, 1L, 2L, "csv"))
                .thenReturn(new CourseExportFile("curso-2.csv", "text/csv; charset=UTF-8", csv));

        ResponseEntity<Object> response = controller.export(user, 10L, 2L, "csv");

        assertSame(csv, response.getBody());
        assertEquals("text/csv;charset=UTF-8", response.getHeaders().getContentType().toString());
        assertEquals("attachment; filename=\"curso-2.csv\"; filename*=UTF-8''curso-2.csv",
                response.getHeaders().getFirst("Content-Disposition"));
    }

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
    }

    @Test
    void list_shouldReturnCourses() {
        List<CourseResponse> courses = List.of(mock(CourseResponse.class));
        when(listCoursesUseCase.execute(10L, 1L)).thenReturn(courses);

        ResponseEntity<List<CourseResponse>> response = controller.list(user, 10L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(courses, response.getBody());
    }

    @Test
    void get_shouldReturnCourseDetail() {
        CourseDetailResponse detail = mock(CourseDetailResponse.class);
        when(getCourseUseCase.execute(10L, 1L, 2L)).thenReturn(detail);

        ResponseEntity<CourseDetailResponse> response = controller.get(user, 10L, 2L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(detail, response.getBody());
    }

    @Test
    void create_shouldReturnCreatedCourse() {
        CreateCourseDTO dto = new CreateCourseDTO();
        CourseResponse created = mock(CourseResponse.class);
        when(createCourseUseCase.execute(10L, 1L, dto)).thenReturn(created);

        ResponseEntity<CourseResponse> response = controller.create(user, 10L, dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(created, response.getBody());
    }

    @Test
    void delete_shouldReturnNoContent() {
        ResponseEntity<Void> response = controller.delete(user, 10L, 2L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(deleteCourseUseCase).execute(10L, 1L, 2L);
    }

    @Test
    void createModule_shouldReturnCreatedModule() {
        CreateModuleDTO dto = new CreateModuleDTO();
        CourseModuleResponse created = mock(CourseModuleResponse.class);
        when(createCourseModuleUseCase.execute(10L, 1L, 2L, dto)).thenReturn(created);

        ResponseEntity<CourseModuleResponse> response = controller.createModule(user, 10L, 2L, dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(created, response.getBody());
    }

    @Test
    void deleteModule_shouldReturnNoContent() {
        ResponseEntity<Void> response = controller.deleteModule(user, 10L, 2L, 3L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(deleteCourseModuleUseCase).execute(10L, 1L, 2L, 3L);
    }

    @Test
    void createLesson_shouldReturnCreatedLesson() {
        CreateCourseLessonDTO dto = new CreateCourseLessonDTO();
        CourseLessonResponse created = mock(CourseLessonResponse.class);
        when(createCourseLessonUseCase.execute(10L, 1L, 2L, 3L, dto)).thenReturn(created);

        ResponseEntity<CourseLessonResponse> response = controller.createLesson(user, 10L, 2L, 3L, dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(created, response.getBody());
    }

    @Test
    void updateLesson_shouldReturnUpdatedLesson() {
        UpdateCourseLessonDTO dto = new UpdateCourseLessonDTO();
        CourseLessonResponse updated = mock(CourseLessonResponse.class);
        when(updateCourseLessonUseCase.execute(10L, 1L, 2L, 3L, 4L, dto)).thenReturn(updated);

        ResponseEntity<CourseLessonResponse> response = controller.updateLesson(user, 10L, 2L, 3L, 4L, dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(updated, response.getBody());
    }

    @Test
    void deleteLesson_shouldReturnNoContent() {
        ResponseEntity<Void> response = controller.deleteLesson(user, 10L, 2L, 3L, 4L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(deleteCourseLessonUseCase).execute(10L, 1L, 2L, 3L, 4L);
    }

    @Test
    void linkMaterial_shouldReturnTheLesson() {
        LinkLessonMaterialDTO dto = new LinkLessonMaterialDTO();
        CourseLessonResponse lesson = mock(CourseLessonResponse.class);
        when(linkLessonMaterialUseCase.execute(10L, 1L, 2L, 3L, 4L, dto)).thenReturn(lesson);

        ResponseEntity<CourseLessonResponse> response = controller.linkMaterial(user, 10L, 2L, 3L, 4L, dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(lesson, response.getBody());
    }

    @Test
    void unlinkMaterial_shouldReturnNoContent() {
        ResponseEntity<Void> response = controller.unlinkMaterial(user, 10L, 2L, 3L, 4L, 5L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(unlinkLessonMaterialUseCase).execute(10L, 1L, 2L, 3L, 4L, 5L);
    }
}
