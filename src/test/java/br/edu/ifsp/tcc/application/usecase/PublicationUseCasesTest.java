package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CourseDetailResponse;
import br.edu.ifsp.tcc.application.dto.CourseExportFile;
import br.edu.ifsp.tcc.application.dto.CourseResponse;
import br.edu.ifsp.tcc.application.dto.PublicationCheckResponse;
import br.edu.ifsp.tcc.application.entity.Course;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.service.CourseExportService;
import br.edu.ifsp.tcc.application.service.CoursePublicationService;
import br.edu.ifsp.tcc.application.service.CourseService;
import br.edu.ifsp.tcc.application.service.MaterialService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Check, publish, unpublish, export and archive use cases (each is a thin orchestration over the services). */
@ExtendWith(MockitoExtension.class)
class PublicationUseCasesTest {

    @Mock private TeamService teamService;
    @Mock private CourseService courseService;
    @Mock private CoursePublicationService publicationService;
    @Mock private CourseExportService exportService;
    @Mock private MaterialService materialService;

    private Course course() {
        Course course = new Course();
        course.setId(20L);
        when(courseService.findCourse(10L, 20L)).thenReturn(course);
        return course;
    }

    @Test
    void check_shouldAllowAnyMember() {
        Course course = course();
        PublicationCheckResponse check = new PublicationCheckResponse(true, java.util.List.of());
        when(publicationService.check(10L, course)).thenReturn(check);

        assertSame(check, new GetPublicationCheckUseCase(teamService, courseService, publicationService).execute(10L, 1L, 20L));

        verify(teamService).requireMember(10L, 1L);
    }

    @Test
    void publish_shouldRequireAnEditorAndReturnTheCourse() {
        Course course = course();
        CourseResponse response = mock(CourseResponse.class);
        when(publicationService.publish(10L, course)).thenReturn(course);
        when(courseService.toCourseResponse(course)).thenReturn(response);

        assertSame(response, new PublishCourseUseCase(teamService, courseService, publicationService).execute(10L, 1L, 20L));

        verify(teamService).requireRole(10L, 1L, TeamRole.ADMIN, TeamRole.CREATOR);
    }

    @Test
    void publish_shouldPropagateTheProblems() {
        Course course = course();
        when(publicationService.publish(10L, course)).thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "x"));

        assertThrows(ResponseStatusException.class,
                () -> new PublishCourseUseCase(teamService, courseService, publicationService).execute(10L, 1L, 20L));
    }

    @Test
    void unpublish_shouldRequireAnEditorAndReturnTheCourse() {
        Course course = course();
        CourseResponse response = mock(CourseResponse.class);
        when(publicationService.unpublish(course)).thenReturn(course);
        when(courseService.toCourseResponse(course)).thenReturn(response);

        assertSame(response, new UnpublishCourseUseCase(teamService, courseService, publicationService).execute(10L, 1L, 20L));

        verify(teamService).requireRole(10L, 1L, TeamRole.ADMIN, TeamRole.CREATOR);
    }

    @Test
    void export_shouldReturnTheTreeAsJsonByDefault() {
        Course course = course();
        CourseDetailResponse tree = mock(CourseDetailResponse.class);
        when(courseService.toDetailResponse(10L, course)).thenReturn(tree);

        CourseExportFile file = new ExportCourseUseCase(teamService, courseService, exportService).execute(10L, 1L, 20L, null);

        assertEquals("curso-20.json", file.getFileName());
        assertEquals("application/json", file.getContentType());
        assertSame(tree, file.getBody());
        verify(teamService).requireMember(10L, 1L);
    }

    @Test
    void export_shouldReturnCsvBytes() {
        Course course = course();
        CourseDetailResponse tree = mock(CourseDetailResponse.class);
        when(courseService.toDetailResponse(10L, course)).thenReturn(tree);
        byte[] csv = {1, 2};
        when(exportService.toCsv(tree)).thenReturn(csv);

        CourseExportFile file = new ExportCourseUseCase(teamService, courseService, exportService).execute(10L, 1L, 20L, " CSV ");

        assertEquals("curso-20.csv", file.getFileName());
        assertEquals("text/csv; charset=UTF-8", file.getContentType());
        assertSame(csv, file.getBody());
    }

    @Test
    void export_shouldRefuseAnUnknownFormat() {
        ExportCourseUseCase useCase = new ExportCourseUseCase(teamService, courseService, exportService);

        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, 1L, 20L, "xml"));

        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
        assertEquals("Formato inválido. Use json ou csv.", e.getReason());
        verify(courseService, never()).findCourse(any(), any());
    }

    @Test
    void archive_shouldUseTheLimitOfThirtyDaysAgo() {
        when(materialService.archiveDueMaterials(any(LocalDateTime.class))).thenReturn(3);
        LocalDateTime before = LocalDateTime.now().minusDays(30);

        int archived = new ArchiveMaterialsUseCase(materialService, 30).execute();

        assertEquals(3, archived);
        org.mockito.ArgumentCaptor<LocalDateTime> limit = org.mockito.ArgumentCaptor.forClass(LocalDateTime.class);
        verify(materialService).archiveDueMaterials(limit.capture());
        assertFalse(limit.getValue().isBefore(before));
        assertFalse(limit.getValue().isAfter(LocalDateTime.now().minusDays(30)));
    }
}
