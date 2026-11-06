package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.entity.Course;
import br.edu.ifsp.tcc.application.entity.CourseLesson;
import br.edu.ifsp.tcc.application.entity.CourseModule;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.service.CourseService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteCourseLessonUseCaseTest {

    @Mock private TeamService teamService;
    @Mock private CourseService courseService;
    @InjectMocks private DeleteCourseLessonUseCase useCase;

    @Test
    void execute_shouldDeleteLessonOfModule() {
        CourseLesson lesson = new CourseLesson();
        when(courseService.findLesson(3L, 4L)).thenReturn(lesson);

        useCase.execute(10L, 1L, 2L, 3L, 4L);

        verify(teamService).requireRole(10L, 1L, TeamRole.ADMIN, TeamRole.CREATOR);
        verify(courseService).findCourse(10L, 2L);
        verify(courseService).findModule(2L, 3L);
        verify(courseService).deleteLesson(lesson);
    }
}
