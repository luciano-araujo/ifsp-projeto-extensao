package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.UpdateCourseLessonDTO;
import br.edu.ifsp.tcc.application.entity.CourseLesson;
import br.edu.ifsp.tcc.application.entity.LessonType;
import br.edu.ifsp.tcc.application.service.CourseService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateCourseLessonUseCaseTest {

    @Mock private TeamService teamService;
    @Mock private CourseService courseService;
    @InjectMocks private UpdateCourseLessonUseCase useCase;

    @Test
    void execute_shouldPublishWithoutChangingOtherFields() {
        CourseLesson lesson = new CourseLesson();
        lesson.setTitle("BFS");
        lesson.setType(LessonType.VIDEO);
        when(courseService.findLesson(30L, 40L)).thenReturn(lesson);
        UpdateCourseLessonDTO dto = new UpdateCourseLessonDTO();
        dto.setPublished(true);

        useCase.execute(10L, 1L, 20L, 30L, 40L, dto);

        assertTrue(lesson.getPublished());
        assertEquals("BFS", lesson.getTitle());
        assertEquals(LessonType.VIDEO, lesson.getType());
        verify(courseService).saveLesson(lesson);
        verify(courseService, never()).findProject(10L, null);
    }
}
