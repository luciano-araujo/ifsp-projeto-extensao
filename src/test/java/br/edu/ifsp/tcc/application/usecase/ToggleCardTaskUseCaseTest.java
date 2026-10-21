package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.entity.CardTask;
import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.service.BoardService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ToggleCardTaskUseCaseTest {

    @Mock private TeamService teamService;
    @Mock private BoardService boardService;
    @InjectMocks private ToggleCardTaskUseCase useCase;

    @Test
    void execute_shouldFlipTaskAndRecomputeProgressForAnyMember() {
        KanbanItem card = new KanbanItem();
        CardTask task = new CardTask();
        task.setCompleted(false);
        when(boardService.findCard(10L, 1L)).thenReturn(card);
        when(boardService.findTask(1L, 7L)).thenReturn(task);
        when(boardService.saveTask(task)).thenReturn(task);

        useCase.execute(10L, 2L, 1L, 7L);

        assertTrue(task.getCompleted());
        verify(teamService).requireMember(10L, 2L);
        verify(boardService).refreshProgress(card);
    }
}
