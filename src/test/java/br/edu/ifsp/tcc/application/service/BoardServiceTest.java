package br.edu.ifsp.tcc.application.service;

import br.edu.ifsp.tcc.application.dto.CardResponse;
import br.edu.ifsp.tcc.application.dto.ReviewStatus;
import br.edu.ifsp.tcc.application.entity.CardTask;
import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.KanbanItemState;
import br.edu.ifsp.tcc.application.entity.Tag;
import br.edu.ifsp.tcc.application.repository.BoardColumnRepository;
import br.edu.ifsp.tcc.application.repository.CardTaskRepository;
import br.edu.ifsp.tcc.application.repository.IdeaVoteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BoardServiceTest {

    @Mock private BoardColumnRepository boardColumnRepository;
    @Mock private CardTaskRepository cardTaskRepository;
    @Mock private IdeaVoteRepository ideaVoteRepository;
    @Mock private KanbanItemService kanbanItemService;
    @InjectMocks private BoardService boardService;

    private CardTask task(KanbanItem item, boolean completed) {
        CardTask task = new CardTask();
        task.setKanbanItem(item);
        task.setCompleted(completed);
        return task;
    }

    @Test
    void toCardResponse_shouldCarryTheMaterialsCountAndDefaultToZero() {
        KanbanItem project = new KanbanItem();
        project.setId(1L);
        project.setState(KanbanItemState.IN_PRODUCTION);

        CardResponse full = boardService.toCardResponse(project, 100L, List.of(), null, 3, ReviewStatus.APPROVED);
        assertEquals(3, full.getMaterialsCount());
        assertEquals(ReviewStatus.APPROVED, full.getReviewStatus());
        assertEquals(ReviewStatus.NONE, boardService.toCardResponse(project, 100L, List.of()).getReviewStatus());
        assertEquals(0, boardService.toCardResponse(project, 100L, List.of(), null).getMaterialsCount());
        assertEquals(0, boardService.toCardResponse(project, 100L, List.of()).getMaterialsCount());
    }

    @Test
    void progressOf_shouldBeZeroWithoutTasksAndRoundedPercentageOtherwise() {
        KanbanItem item = new KanbanItem();
        assertEquals(0, BoardService.progressOf(List.of()));
        assertEquals(33, BoardService.progressOf(List.of(task(item, true), task(item, false), task(item, false))));
        assertEquals(100, BoardService.progressOf(List.of(task(item, true))));
    }

    @Test
    void toCardResponse_shouldCountTasksAndMarkIdeasAndProjects() {
        KanbanItem project = new KanbanItem();
        project.setId(1L);
        project.setState(KanbanItemState.IN_PRODUCTION);

        CardResponse response = boardService.toCardResponse(project, 100L,
                List.of(task(project, true), task(project, false)));

        assertEquals("PROJECT", response.getType());
        assertEquals(2, response.getTasksTotal());
        assertEquals(1, response.getTasksDone());
        assertEquals(50, response.getProgress());

        KanbanItem idea = new KanbanItem();
        idea.setState(KanbanItemState.IDEATION);
        Tag podcast = new Tag();
        podcast.setId(4L);
        podcast.setName("Podcast");
        podcast.setColor("#1565C0");
        idea.setTag(podcast);
        idea.setDueDate(java.time.LocalDate.of(2026, 11, 30));
        CardResponse ideaResponse = boardService.toCardResponse(idea, null, List.of());
        assertEquals("IDEA", ideaResponse.getType());
        assertEquals("Podcast", ideaResponse.getTag());
        assertEquals(4L, ideaResponse.getTagId());
        assertEquals("#1565C0", ideaResponse.getTagColor());
        assertNull(ideaResponse.getColumnSince());
        assertNull(boardService.toCardResponse(project, 100L, List.of()).getTagId());
        assertEquals(java.time.LocalDate.of(2026, 11, 30), ideaResponse.getDueDate());
        assertEquals(0, ideaResponse.getVotes());
    }

    @Test
    void findColumn_shouldReturnNotFoundForColumnOfAnotherTeam() {
        when(boardColumnRepository.findByIdAndTeam_Id(999L, 10L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> boardService.findColumn(10L, 999L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }
}
