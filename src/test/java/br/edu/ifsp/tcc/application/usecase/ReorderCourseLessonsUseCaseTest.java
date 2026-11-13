package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CourseLessonResponse;
import br.edu.ifsp.tcc.application.dto.MaterialResponse;
import br.edu.ifsp.tcc.application.dto.ReorderLessonsDTO;
import br.edu.ifsp.tcc.application.entity.CourseLesson;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.service.CourseService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReorderCourseLessonsUseCaseTest {

    @Mock private TeamService teamService;
    @Mock private CourseService courseService;
    @InjectMocks private ReorderCourseLessonsUseCase useCase;

    private CourseLesson lesson(Long id) {
        CourseLesson lesson = new CourseLesson();
        lesson.setId(id);
        return lesson;
    }

    private ReorderLessonsDTO dto(Long... ids) {
        ReorderLessonsDTO dto = new ReorderLessonsDTO();
        dto.setLessonIds(List.of(ids));
        return dto;
    }

    @Test
    void execute_shouldRenumberTheLessonsAndReturnThemInOrder() {
        CourseLesson a = lesson(1L);
        CourseLesson b = lesson(2L);
        when(courseService.findLessons(30L)).thenReturn(List.of(a, b));
        when(courseService.saveLesson(any(CourseLesson.class))).thenAnswer(inv -> inv.getArgument(0));
        MaterialResponse material = mock(MaterialResponse.class);
        when(courseService.findMaterialsByLesson(10L, Map.of(30L, List.of(b, a)))).thenReturn(Map.of(1L, List.of(material)));
        CourseLessonResponse first = mock(CourseLessonResponse.class);
        CourseLessonResponse second = mock(CourseLessonResponse.class);
        when(courseService.toLessonResponse(b, List.of())).thenReturn(first);
        when(courseService.toLessonResponse(a, List.of(material))).thenReturn(second);

        assertEquals(List.of(first, second), useCase.execute(10L, 1L, 20L, 30L, dto(2L, 1L)));

        assertEquals(0, b.getSortOrder());
        assertEquals(1, a.getSortOrder());
        verify(teamService).requireRole(10L, 1L, TeamRole.ADMIN, TeamRole.CREATOR);
        verify(courseService).findCourse(10L, 20L);
        verify(courseService).findModule(20L, 30L);
    }

    @Test
    void execute_shouldNotSaveWhenTheListIsWrong() {
        when(courseService.findLessons(30L)).thenReturn(List.of(lesson(1L)));
        doThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "x")).when(courseService)
                .requireExactIds(anyList(), anyList(), anyString());

        assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, 1L, 20L, 30L, dto(1L, 9L)));

        verify(courseService, never()).saveLesson(any());
    }
}
