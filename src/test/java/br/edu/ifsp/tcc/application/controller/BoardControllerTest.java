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
class BoardControllerTest {

    @Mock private GetBoardUseCase getBoardUseCase;
    @Mock private ListIdeasUseCase listIdeasUseCase;
    @Mock private CreateIdeaUseCase createIdeaUseCase;
    @Mock private CreateCardUseCase createCardUseCase;
    @Mock private ConvertIdeaUseCase convertIdeaUseCase;
    @Mock private MoveCardUseCase moveCardUseCase;
    @Mock private DeleteCardUseCase deleteCardUseCase;
    @Mock private CreateColumnUseCase createColumnUseCase;
    @Mock private UpdateColumnUseCase updateColumnUseCase;
    @Mock private DeleteColumnUseCase deleteColumnUseCase;
    @Mock private CreateCardTaskUseCase createCardTaskUseCase;
    @Mock private ToggleCardTaskUseCase toggleCardTaskUseCase;
    @Mock private DeleteCardTaskUseCase deleteCardTaskUseCase;
    @Mock private ToggleIdeaVoteUseCase toggleIdeaVoteUseCase;
    @Mock private UpdateCardTaskUseCase updateCardTaskUseCase;
    @Mock private ReorderCardTasksUseCase reorderCardTasksUseCase;
    @Mock private ReorderColumnsUseCase reorderColumnsUseCase;
    @Mock private GetCardHistoryUseCase getCardHistoryUseCase;
    @InjectMocks private BoardController controller;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
    }

    @Test
    void board_shouldReturnBoard() {
        BoardResponse board = mock(BoardResponse.class);
        when(getBoardUseCase.execute(10L, 1L)).thenReturn(board);

        ResponseEntity<BoardResponse> response = controller.board(user, 10L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(board, response.getBody());
    }

    @Test
    void ideas_shouldReturnIdeas() {
        List<CardResponse> ideas = List.of(mock(CardResponse.class));
        when(listIdeasUseCase.execute(10L, 1L, "excel", 3L, "votes")).thenReturn(ideas);

        ResponseEntity<List<CardResponse>> response = controller.ideas(user, 10L, "excel", 3L, "votes");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(ideas, response.getBody());
    }

    @Test
    void createIdea_shouldReturnCreatedIdea() {
        CreateCardDTO dto = new CreateCardDTO();
        CardResponse created = mock(CardResponse.class);
        when(createIdeaUseCase.execute(10L, user, dto)).thenReturn(created);

        ResponseEntity<CardResponse> response = controller.createIdea(user, 10L, dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(created, response.getBody());
    }

    @Test
    void vote_shouldReturnVoteResponse() {
        IdeaVoteResponse vote = mock(IdeaVoteResponse.class);
        when(toggleIdeaVoteUseCase.execute(10L, user, 3L)).thenReturn(vote);

        ResponseEntity<IdeaVoteResponse> response = controller.vote(user, 10L, 3L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(vote, response.getBody());
    }

    @Test
    void createCard_shouldReturnCreatedCard() {
        CreateCardDTO dto = new CreateCardDTO();
        CardResponse created = mock(CardResponse.class);
        when(createCardUseCase.execute(10L, user, dto)).thenReturn(created);

        ResponseEntity<CardResponse> response = controller.createCard(user, 10L, dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(created, response.getBody());
    }

    @Test
    void convert_shouldReturnConvertedCard() {
        ConvertCardDTO dto = new ConvertCardDTO();
        CardResponse converted = mock(CardResponse.class);
        when(convertIdeaUseCase.execute(10L, 1L, 3L, 101L, dto)).thenReturn(converted);

        ResponseEntity<CardResponse> response = controller.convert(user, 10L, 3L, 101L, dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(converted, response.getBody());
    }

    @Test
    void move_shouldReturnMovedCard() {
        MoveCardDTO dto = new MoveCardDTO();
        CardResponse moved = mock(CardResponse.class);
        when(moveCardUseCase.execute(10L, 1L, 3L, dto)).thenReturn(moved);

        ResponseEntity<CardResponse> response = controller.move(user, 10L, 3L, dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(moved, response.getBody());
    }

    @Test
    void deleteCard_shouldReturnNoContent() {
        ResponseEntity<Void> response = controller.deleteCard(user, 10L, 3L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(deleteCardUseCase).execute(10L, 1L, 3L);
    }

    @Test
    void addColumn_shouldReturnCreatedColumn() {
        BoardColumnDTO dto = new BoardColumnDTO();
        BoardColumnResponse created = mock(BoardColumnResponse.class);
        when(createColumnUseCase.execute(10L, 1L, dto)).thenReturn(created);

        ResponseEntity<BoardColumnResponse> response = controller.addColumn(user, 10L, dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(created, response.getBody());
    }

    @Test
    void updateColumn_shouldReturnUpdatedColumn() {
        BoardColumnDTO dto = new BoardColumnDTO();
        BoardColumnResponse updated = mock(BoardColumnResponse.class);
        when(updateColumnUseCase.execute(10L, 1L, 5L, dto)).thenReturn(updated);

        ResponseEntity<BoardColumnResponse> response = controller.updateColumn(user, 10L, 5L, dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(updated, response.getBody());
    }

    @Test
    void deleteColumn_shouldReturnNoContent() {
        ResponseEntity<Void> response = controller.deleteColumn(user, 10L, 5L, 6L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(deleteColumnUseCase).execute(10L, 1L, 5L, 6L);
    }

    @Test
    void createTask_shouldReturnCreatedTask() {
        CreateTaskDTO dto = new CreateTaskDTO();
        CardTaskResponse created = mock(CardTaskResponse.class);
        when(createCardTaskUseCase.execute(10L, 1L, 3L, dto)).thenReturn(created);

        ResponseEntity<CardTaskResponse> response = controller.createTask(user, 10L, 3L, dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(created, response.getBody());
    }

    @Test
    void toggleTask_shouldReturnToggledTask() {
        CardTaskResponse toggled = mock(CardTaskResponse.class);
        when(toggleCardTaskUseCase.execute(10L, 1L, 3L, 4L)).thenReturn(toggled);

        ResponseEntity<CardTaskResponse> response = controller.toggleTask(user, 10L, 3L, 4L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(toggled, response.getBody());
    }

    @Test
    void deleteTask_shouldReturnNoContent() {
        ResponseEntity<Void> response = controller.deleteTask(user, 10L, 3L, 4L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(deleteCardTaskUseCase).execute(10L, 1L, 3L, 4L);
    }

    @Test
    void updateTask_shouldReturnUpdatedTask() {
        UpdateTaskDTO dto = new UpdateTaskDTO();
        CardTaskResponse updated = mock(CardTaskResponse.class);
        when(updateCardTaskUseCase.execute(10L, 1L, 3L, 4L, dto)).thenReturn(updated);

        ResponseEntity<CardTaskResponse> response = controller.updateTask(user, 10L, 3L, 4L, dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(updated, response.getBody());
    }

    @Test
    void reorderTasks_shouldReturnOrderedTasks() {
        ReorderTasksDTO dto = new ReorderTasksDTO();
        List<CardTaskResponse> ordered = List.of(mock(CardTaskResponse.class));
        when(reorderCardTasksUseCase.execute(10L, 1L, 3L, dto)).thenReturn(ordered);

        ResponseEntity<List<CardTaskResponse>> response = controller.reorderTasks(user, 10L, 3L, dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(ordered, response.getBody());
    }

    @Test
    void reorderColumns_shouldReturnOrderedColumns() {
        ReorderColumnsDTO dto = new ReorderColumnsDTO();
        List<BoardColumnResponse> ordered = List.of(mock(BoardColumnResponse.class));
        when(reorderColumnsUseCase.execute(10L, 1L, dto)).thenReturn(ordered);

        ResponseEntity<List<BoardColumnResponse>> response = controller.reorderColumns(user, 10L, dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(ordered, response.getBody());
    }

    @Test
    void history_shouldReturnMovements() {
        List<CardMovementResponse> history = List.of(mock(CardMovementResponse.class));
        when(getCardHistoryUseCase.execute(10L, 1L, 3L)).thenReturn(history);

        ResponseEntity<List<CardMovementResponse>> response = controller.history(user, 10L, 3L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(history, response.getBody());
    }
}
