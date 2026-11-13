package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CourseDetailResponse;
import br.edu.ifsp.tcc.application.dto.CourseExportFile;
import br.edu.ifsp.tcc.application.entity.Course;
import br.edu.ifsp.tcc.application.service.CourseExportService;
import br.edu.ifsp.tcc.application.service.CourseService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExportCourseUseCaseTest {

    @Mock private TeamService teamService;
    @Mock private CourseService courseService;
    @Mock private CourseExportService exportService;
    @InjectMocks private ExportCourseUseCase useCase;

    private CourseDetailResponse tree() {
        Course course = new Course();
        course.setId(20L);
        CourseDetailResponse tree = mock(CourseDetailResponse.class);
        when(courseService.findCourse(10L, 20L)).thenReturn(course);
        when(courseService.toDetailResponse(10L, course)).thenReturn(tree);
        return tree;
    }

    @Test
    void execute_shouldReturnTheTreeAsJsonByDefault() {
        CourseDetailResponse tree = tree();

        CourseExportFile file = useCase.execute(10L, 1L, 20L, null);

        assertEquals("curso-20.json", file.getFileName());
        assertEquals("application/json", file.getContentType());
        assertSame(tree, file.getBody());
        verify(teamService).requireMember(10L, 1L);
    }

    @Test
    void execute_shouldReturnCsvBytes() {
        CourseDetailResponse tree = tree();
        byte[] csv = {1, 2};
        when(exportService.toCsv(tree)).thenReturn(csv);

        CourseExportFile file = useCase.execute(10L, 1L, 20L, " CSV ");

        assertEquals("curso-20.csv", file.getFileName());
        assertEquals("text/csv; charset=UTF-8", file.getContentType());
        assertSame(csv, file.getBody());
    }

    @Test
    void execute_shouldRefuseAnUnknownFormat() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, 1L, 20L, "xml"));

        assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
        assertEquals("Formato inválido. Use json ou csv.", e.getReason());
        verify(courseService, never()).findCourse(any(), any());
    }
}
