package br.edu.ifsp.tcc.application.service;

import br.edu.ifsp.tcc.application.dto.CardMovementResponse;
import br.edu.ifsp.tcc.application.dto.CardResponse;
import br.edu.ifsp.tcc.application.dto.CardTaskResponse;
import br.edu.ifsp.tcc.application.entity.*;
import br.edu.ifsp.tcc.application.repository.CardMovementRepository;
import br.edu.ifsp.tcc.application.repository.CardTaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Card history, reordering checks and the task/card mappings added with tags, assignees and columnSince. */
@ExtendWith(MockitoExtension.class)
class BoardServiceHistoryTest {

    @Mock private CardMovementRepository cardMovementRepository;
    @Mock private CardTaskRepository cardTaskRepository;
    @InjectMocks private BoardService service;

    private KanbanItem card(Long id) {
        KanbanItem item = new KanbanItem();
        item.setId(id);
        return item;
    }

    private User user(Long id, String name) {
        User user = new User();
        user.setId(id);
        user.setName(name);
        return user;
    }

    private CardMovementRepository.ColumnSince since(Long cardId, LocalDateTime enteredAt) {
        CardMovementRepository.ColumnSince row = mock(CardMovementRepository.ColumnSince.class);
        when(row.getCardId()).thenReturn(cardId);
        when(row.getEnteredAt()).thenReturn(enteredAt);
        return row;
    }

    @Test
    void recordMovement_shouldSaveSnapshotOfTheColumnAndAuthor() {
        KanbanItem item = card(5L);
        BoardColumn column = new BoardColumn();
        column.setId(2L);
        column.setName("Roteiro");
        User author = user(1L, "Ana");
        when(cardMovementRepository.save(any(CardMovement.class))).thenAnswer(inv -> inv.getArgument(0));

        CardMovement saved = service.recordMovement(item, column, author);

        ArgumentCaptor<CardMovement> captor = ArgumentCaptor.forClass(CardMovement.class);
        verify(cardMovementRepository).save(captor.capture());
        assertSame(saved, captor.getValue());
        assertSame(item, saved.getKanbanItem());
        assertEquals(2L, saved.getColumnId());
        assertEquals("Roteiro", saved.getColumnName());
        assertSame(author, saved.getMovedBy());
        assertNotNull(saved.getEnteredAt());
        assertEquals(0, saved.getEnteredAt().getNano(), "stored with second precision so responses match the history");
    }

    @Test
    void findHistory_shouldReturnMovementsNewestFirst() {
        List<CardMovement> history = List.of(new CardMovement());
        when(cardMovementRepository.findHistory(5L)).thenReturn(history);

        assertSame(history, service.findHistory(5L));
    }

    @Test
    void findColumnSince_shouldReturnEmptyMapWithoutCards() {
        assertEquals(Map.of(), service.findColumnSince(List.of()));
        verifyNoInteractions(cardMovementRepository);
    }

    @Test
    void findColumnSince_shouldLoadAllCardsInOneQuery() {
        LocalDateTime first = LocalDateTime.of(2026, 11, 1, 14, 10);
        LocalDateTime second = LocalDateTime.of(2026, 11, 2, 9, 0);
        CardMovementRepository.ColumnSince row5 = since(5L, first);
        CardMovementRepository.ColumnSince row6 = since(6L, second);
        when(cardMovementRepository.findColumnSince(List.of(5L, 6L))).thenReturn(List.of(row5, row6));

        Map<Long, LocalDateTime> result = service.findColumnSince(List.of(card(5L), card(6L)));

        assertEquals(Map.of(5L, first, 6L, second), result);
        verify(cardMovementRepository, times(1)).findColumnSince(any());
    }

    @Test
    void requireExactIds_shouldAcceptSameIdsInAnyOrder() {
        assertDoesNotThrow(() -> service.requireExactIds(List.of(3L, 1L, 2L), List.of(1L, 2L, 3L), "erro"));
    }

    @Test
    void requireExactIds_shouldRejectMissingExtraOrRepeatedIds() {
        List<Long> existing = List.of(1L, 2L, 3L);

        for (List<Long> requested : List.of(List.of(1L, 2L), List.of(1L, 2L, 3L, 4L), List.of(1L, 1L, 2L),
                List.of(1L, 2L, 9L))) {
            ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                    () -> service.requireExactIds(requested, existing, "A lista está incompleta."));
            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
            assertEquals("A lista está incompleta.", ex.getReason());
        }
    }

    @Test
    void toMovementResponse_shouldExposeAuthorOrNullWhenAccountWasDeleted() {
        CardMovement movement = new CardMovement();
        BoardColumn column = new BoardColumn();
        column.setId(2L);
        movement.setColumn(column);
        movement.setColumnName("Roteiro");
        movement.setEnteredAt(LocalDateTime.of(2026, 11, 1, 14, 10));
        movement.setMovedBy(user(1L, "Ana Admin"));

        CardMovementResponse response = service.toMovementResponse(movement);

        assertEquals(2L, response.getColumnId());
        assertEquals("Roteiro", response.getColumnName());
        assertEquals(1L, response.getMovedBy().getUserId());
        assertEquals("Ana Admin", response.getMovedBy().getName());

        movement.setMovedBy(null);
        movement.setColumn(null);
        CardMovementResponse orphan = service.toMovementResponse(movement);
        assertNull(orphan.getMovedBy());
        assertNull(orphan.getColumnId());
        assertEquals("Roteiro", orphan.getColumnName());
    }

    @Test
    void toTaskResponse_shouldIncludeAssigneeAndDueDate() {
        KanbanItem item = card(14L);
        TeamMember member = new TeamMember();
        member.setId(5L);
        member.setUser(user(3L, "Bia Criadora"));
        CardTask task = new CardTask();
        task.setId(33L);
        task.setKanbanItem(item);
        task.setTitle("Gravar");
        task.setAssignee(member);
        task.setDueDate(LocalDate.of(2026, 11, 5));

        CardTaskResponse response = service.toTaskResponse(task);

        assertEquals(5L, response.getAssignee().getMemberId());
        assertEquals(3L, response.getAssignee().getUserId());
        assertEquals("Bia Criadora", response.getAssignee().getName());
        assertEquals(LocalDate.of(2026, 11, 5), response.getDueDate());

        task.setAssignee(null);
        task.setDueDate(null);
        CardTaskResponse bare = service.toTaskResponse(task);
        assertNull(bare.getAssignee());
        assertNull(bare.getDueDate());
    }

    @Test
    void toCardResponse_shouldCarryColumnSinceAndTagFields() {
        KanbanItem project = card(14L);
        project.setState(KanbanItemState.IN_PRODUCTION);
        LocalDateTime since = LocalDateTime.of(2026, 11, 1, 14, 10);

        CardResponse response = service.toCardResponse(project, 6L, List.of(), since);

        assertEquals(since, response.getColumnSince());
        assertNull(response.getTag());
        assertNull(response.getTagColor());
    }
}
