package br.edu.ifsp.tcc.application.service;

import br.edu.ifsp.tcc.application.dto.BoardColumnResponse;
import br.edu.ifsp.tcc.application.dto.CardTaskResponse;
import br.edu.ifsp.tcc.application.entity.BoardColumn;
import br.edu.ifsp.tcc.application.entity.CardTask;
import br.edu.ifsp.tcc.application.entity.IdeaVote;
import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.KanbanItemState;
import br.edu.ifsp.tcc.application.entity.User;
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
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BoardServiceMoreTest {

    @Mock private BoardColumnRepository boardColumnRepository;
    @Mock private CardTaskRepository cardTaskRepository;
    @Mock private IdeaVoteRepository ideaVoteRepository;
    @Mock private KanbanItemService kanbanItemService;
    @InjectMocks private BoardService service;

    private BoardColumn column(Long id) {
        BoardColumn column = new BoardColumn();
        column.setId(id);
        column.setName("Pesquisa");
        column.setSortOrder(0);
        return column;
    }

    private KanbanItem card(Long id) {
        KanbanItem item = new KanbanItem();
        item.setId(id);
        return item;
    }

    private CardTask task(Long id, KanbanItem item, boolean completed) {
        CardTask task = new CardTask();
        task.setId(id);
        task.setKanbanItem(item);
        task.setTitle("Tarefa " + id);
        task.setCompleted(completed);
        task.setSortOrder(0);
        return task;
    }

    @Test
    void findColumns_shouldReturnTeamColumns() {
        List<BoardColumn> columns = List.of(column(1L));
        when(boardColumnRepository.findByTeam_IdOrderBySortOrderAscIdAsc(10L)).thenReturn(columns);

        assertSame(columns, service.findColumns(10L));
    }

    @Test
    void findColumn_shouldReturnColumnOfTeam() {
        BoardColumn column = column(1L);
        when(boardColumnRepository.findByIdAndTeam_Id(1L, 10L)).thenReturn(Optional.of(column));

        assertSame(column, service.findColumn(10L, 1L));
    }

    @Test
    void resolveColumn_shouldReturnGivenColumn() {
        BoardColumn column = column(1L);
        when(boardColumnRepository.findByIdAndTeam_Id(1L, 10L)).thenReturn(Optional.of(column));

        assertSame(column, service.resolveColumn(10L, 1L));
    }

    @Test
    void resolveColumn_shouldReturnFirstColumnWhenNoneGiven() {
        BoardColumn first = column(1L);
        when(boardColumnRepository.findByTeam_IdOrderBySortOrderAscIdAsc(10L)).thenReturn(List.of(first, column(2L)));

        assertSame(first, service.resolveColumn(10L, null));
    }

    @Test
    void resolveColumn_shouldRejectBoardWithoutColumns() {
        when(boardColumnRepository.findByTeam_IdOrderBySortOrderAscIdAsc(10L)).thenReturn(List.of());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.resolveColumn(10L, null));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void countColumns_shouldReturnRepositoryCount() {
        when(boardColumnRepository.countByTeam_Id(10L)).thenReturn(4);

        assertEquals(4, service.countColumns(10L));
    }

    @Test
    void saveColumn_shouldDelegateToRepository() {
        BoardColumn column = column(null);
        when(boardColumnRepository.save(column)).thenReturn(column);

        assertSame(column, service.saveColumn(column));
    }

    @Test
    void deleteColumn_shouldDelegateToRepository() {
        BoardColumn column = column(1L);

        service.deleteColumn(column);

        verify(boardColumnRepository).delete(column);
    }

    @Test
    void findCard_shouldReturnCardOfTeam() {
        KanbanItem item = card(5L);
        when(kanbanItemService.findByIdAndTeamId(5L, 10L)).thenReturn(Optional.of(item));

        assertSame(item, service.findCard(10L, 5L));
    }

    @Test
    void findCard_shouldReturnNotFoundWhenMissing() {
        when(kanbanItemService.findByIdAndTeamId(5L, 10L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.findCard(10L, 5L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void nextSortOrder_shouldCountCardsOfColumn() {
        KanbanItem inColumn = card(1L);
        BoardColumn column = column(2L);
        inColumn.setColumn(column);
        KanbanItem elsewhere = card(3L);
        when(kanbanItemService.findByTeamId(10L)).thenReturn(List.of(inColumn, elsewhere));

        assertEquals(1, service.nextSortOrder(10L, 2L));
    }

    @Test
    void findTasksByCard_shouldReturnEmptyMapWithoutItems() {
        assertEquals(Map.of(), service.findTasksByCard(List.of()));
        verifyNoInteractions(cardTaskRepository);
    }

    @Test
    void findTasksByCard_shouldGroupTasksByCard() {
        KanbanItem item = card(5L);
        CardTask task = task(6L, item, false);
        when(cardTaskRepository.findByKanbanItem_IdInOrderBySortOrderAscIdAsc(List.of(5L))).thenReturn(List.of(task));

        assertEquals(List.of(task), service.findTasksByCard(List.of(item)).get(5L));
    }

    @Test
    void findTasks_shouldReturnTasksOfCard() {
        KanbanItem item = card(5L);
        CardTask task = task(6L, item, true);
        when(cardTaskRepository.findByKanbanItem_IdInOrderBySortOrderAscIdAsc(List.of(5L))).thenReturn(List.of(task));

        assertEquals(List.of(task), service.findTasks(item));
    }

    @Test
    void findTasks_shouldReturnEmptyWhenCardHasNoTasks() {
        KanbanItem item = card(5L);
        when(cardTaskRepository.findByKanbanItem_IdInOrderBySortOrderAscIdAsc(List.of(5L))).thenReturn(List.of());

        assertEquals(List.of(), service.findTasks(item));
    }

    @Test
    void findTask_shouldReturnTaskOfCard() {
        CardTask task = task(6L, card(5L), false);
        when(cardTaskRepository.findByIdAndKanbanItem_Id(6L, 5L)).thenReturn(Optional.of(task));

        assertSame(task, service.findTask(5L, 6L));
    }

    @Test
    void findTask_shouldReturnNotFoundWhenMissing() {
        when(cardTaskRepository.findByIdAndKanbanItem_Id(6L, 5L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.findTask(5L, 6L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void countTasks_shouldReturnRepositoryCount() {
        when(cardTaskRepository.countByKanbanItem_Id(5L)).thenReturn(3);

        assertEquals(3, service.countTasks(5L));
    }

    @Test
    void saveTask_shouldDelegateToRepository() {
        CardTask task = task(null, card(5L), false);
        when(cardTaskRepository.save(task)).thenReturn(task);

        assertSame(task, service.saveTask(task));
    }

    @Test
    void deleteTask_shouldDelegateToRepository() {
        CardTask task = task(6L, card(5L), false);

        service.deleteTask(task);

        verify(cardTaskRepository).delete(task);
    }

    @Test
    void findVotes_shouldReturnEmptyWithoutItems() {
        assertEquals(List.of(), service.findVotes(List.of()));
        verifyNoInteractions(ideaVoteRepository);
    }

    @Test
    void findVotes_shouldReturnVotesOfItems() {
        List<IdeaVote> votes = List.of(new IdeaVote());
        when(ideaVoteRepository.findByKanbanItem_IdIn(List.of(5L))).thenReturn(votes);

        assertSame(votes, service.findVotes(List.of(card(5L))));
    }

    @Test
    void findVote_shouldReturnVoteOfUser() {
        IdeaVote vote = new IdeaVote();
        when(ideaVoteRepository.findByKanbanItem_IdAndUser_Id(5L, 1L)).thenReturn(Optional.of(vote));

        assertEquals(Optional.of(vote), service.findVote(5L, 1L));
    }

    @Test
    void countVotes_shouldReturnRepositoryCount() {
        when(ideaVoteRepository.countByKanbanItem_Id(5L)).thenReturn(7);

        assertEquals(7, service.countVotes(5L));
    }

    @Test
    void saveVote_shouldDelegateToRepository() {
        IdeaVote vote = new IdeaVote();
        when(ideaVoteRepository.save(vote)).thenReturn(vote);

        assertSame(vote, service.saveVote(vote));
    }

    @Test
    void deleteVote_shouldDelegateToRepository() {
        IdeaVote vote = new IdeaVote();

        service.deleteVote(vote);

        verify(ideaVoteRepository).delete(vote);
    }

    @Test
    void progressOf_shouldReturnFullProgressWhenAllTasksDone() {
        KanbanItem item = card(5L);

        assertEquals(100, BoardService.progressOf(List.of(task(6L, item, true))));
    }

    @Test
    void toTaskResponse_shouldMapDefaultsForMissingValues() {
        CardTask task = task(6L, card(5L), false);
        task.setCompleted(null);
        task.setSortOrder(null);

        CardTaskResponse response = service.toTaskResponse(task);

        assertEquals(6L, response.getId());
        assertFalse(response.getCompleted());
        assertEquals(0, response.getSortOrder());
    }

    @Test
    void toColumnResponse_shouldMapColumn() {
        BoardColumn column = column(2L);

        BoardColumnResponse response = service.toColumnResponse(column, List.of());

        assertEquals(2L, response.getId());
        assertEquals("Pesquisa", response.getName());
        assertEquals(List.of(), response.getCards());
    }

    @Test
    void toCardResponse_shouldMapIdeaState() {
        KanbanItem item = card(5L);
        item.setState(KanbanItemState.IDEATION);
        item.setSortOrder(null);
        item.setTeam(null);

        var response = service.toCardResponse(item, 2L, List.of());

        assertEquals("IDEA", response.getType());
        assertEquals(0, response.getSortOrder());
        assertEquals(0, response.getTasksTotal());
    }
}
