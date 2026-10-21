package br.edu.ifsp.tcc.application.service;

import br.edu.ifsp.tcc.application.dto.ConvertCardDTO;
import br.edu.ifsp.tcc.application.dto.TeamResponses.BoardResponse;
import br.edu.ifsp.tcc.application.dto.TeamResponses.CardResponse;
import br.edu.ifsp.tcc.application.entity.*;
import br.edu.ifsp.tcc.application.repository.BoardColumnRepository;
import br.edu.ifsp.tcc.application.repository.CardTaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BoardServiceTest {

    @Mock private TeamService teamService;
    @Mock private KanbanItemService kanbanItemService;
    @Mock private BoardColumnRepository boardColumnRepository;
    @Mock private CardTaskRepository cardTaskRepository;
    @InjectMocks private BoardService boardService;

    private Team team;
    private BoardColumn pesquisa;
    private BoardColumn roteiro;

    @BeforeEach
    void setUp() {
        team = new Team();
        team.setId(10L);
        team.setName("Equipe");
        pesquisa = column(100L, "Pesquisa", 0);
        roteiro = column(101L, "Roteiro", 1);

        TeamMember admin = new TeamMember();
        admin.setTeam(team);
        admin.setRole(TeamRole.ADMIN);
        when(teamService.requireMember(eq10(), any())).thenReturn(admin);
        when(teamService.requireRole(eq10(), any(), any())).thenReturn(admin);
        when(teamService.requireRole(eq10(), any(), any(), any())).thenReturn(admin);
        when(boardColumnRepository.findByTeam_IdOrderBySortOrderAscIdAsc(10L)).thenReturn(List.of(pesquisa, roteiro));
        when(boardColumnRepository.findByIdAndTeam_Id(100L, 10L)).thenReturn(Optional.of(pesquisa));
        when(boardColumnRepository.findByIdAndTeam_Id(101L, 10L)).thenReturn(Optional.of(roteiro));
        when(kanbanItemService.save(any(KanbanItem.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static Long eq10() {
        return org.mockito.ArgumentMatchers.eq(10L);
    }

    private BoardColumn column(Long id, String name, int order) {
        BoardColumn column = new BoardColumn();
        column.setId(id);
        column.setTeam(team);
        column.setName(name);
        column.setSortOrder(order);
        return column;
    }

    private KanbanItem card(Long id, KanbanItemState state, BoardColumn column) {
        KanbanItem item = new KanbanItem();
        item.setId(id);
        item.setTitle("Card " + id);
        item.setState(state);
        item.setTeam(team);
        item.setColumn(column);
        return item;
    }

    private CardTask task(Long id, KanbanItem item, boolean completed) {
        CardTask task = new CardTask();
        task.setId(id);
        task.setKanbanItem(item);
        task.setTitle("Tarefa " + id);
        task.setCompleted(completed);
        return task;
    }

    // --- RN01 on conversion ---

    @Test
    void convertIdea_shouldRejectIdeaWithoutAudienceAndObjective() {
        KanbanItem idea = card(1L, KanbanItemState.IDEATION, null);
        when(kanbanItemService.findByIdAndTeamId(1L, 10L)).thenReturn(Optional.of(idea));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> boardService.convertIdea(10L, 1L, 1L, null, null));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals(KanbanItemState.IDEATION, idea.getState());
        verify(kanbanItemService, never()).save(any());
    }

    @Test
    void convertIdea_shouldUseBodyFieldsAndPlaceCardInTargetColumn() {
        KanbanItem idea = card(1L, KanbanItemState.IDEATION, null);
        when(kanbanItemService.findByIdAndTeamId(1L, 10L)).thenReturn(Optional.of(idea));
        ConvertCardDTO dto = new ConvertCardDTO();
        dto.setTargetAudience("Iniciantes");
        dto.setPedagogicalObjective("Entender BFS");

        CardResponse response = boardService.convertIdea(10L, 1L, 1L, 101L, dto);

        assertEquals(KanbanItemState.IN_PRODUCTION, idea.getState());
        assertSame(roteiro, idea.getColumn());
        assertEquals("Iniciantes", idea.getTargetAudience());
        assertEquals("PROJECT", response.type());
        assertEquals(101L, response.columnId());
    }

    @Test
    void convertIdea_shouldUseFirstColumnWhenNoneIsGiven() {
        KanbanItem idea = card(1L, KanbanItemState.IDEATION, null);
        idea.setTargetAudience("Iniciantes");
        idea.setPedagogicalObjective("Entender BFS");
        when(kanbanItemService.findByIdAndTeamId(1L, 10L)).thenReturn(Optional.of(idea));

        boardService.convertIdea(10L, 1L, 1L, null, null);

        assertSame(pesquisa, idea.getColumn());
    }

    @Test
    void convertIdea_shouldRejectCardThatIsAlreadyAProject() {
        KanbanItem project = card(1L, KanbanItemState.IN_PRODUCTION, pesquisa);
        when(kanbanItemService.findByIdAndTeamId(1L, 10L)).thenReturn(Optional.of(project));

        assertThrows(ResponseStatusException.class, () -> boardService.convertIdea(10L, 1L, 1L, null, null));
    }

    // --- Board ---

    @Test
    void getBoard_shouldGroupProjectsByColumnAndLeaveIdeasOut() {
        KanbanItem idea = card(1L, KanbanItemState.IDEATION, null);
        KanbanItem inPesquisa = card(2L, KanbanItemState.IN_PRODUCTION, pesquisa);
        KanbanItem withoutColumn = card(3L, KanbanItemState.IN_PRODUCTION, null);
        KanbanItem inRoteiro = card(4L, KanbanItemState.IN_PRODUCTION, roteiro);
        when(kanbanItemService.findByTeamId(10L)).thenReturn(List.of(idea, inPesquisa, withoutColumn, inRoteiro));
        when(cardTaskRepository.findByKanbanItem_IdInOrderBySortOrderAscIdAsc(anyList()))
                .thenReturn(List.of(task(1L, inRoteiro, true), task(2L, inRoteiro, false)));

        BoardResponse board = boardService.getBoard(10L, 1L);

        assertEquals(List.of(2L, 3L), board.columns().get(0).cards().stream().map(CardResponse::id).toList());
        CardResponse roteiroCard = board.columns().get(1).cards().get(0);
        assertEquals(4L, roteiroCard.id());
        assertEquals(2, roteiroCard.tasksTotal());
        assertEquals(1, roteiroCard.tasksDone());
        assertEquals(50, roteiroCard.progress());
    }

    // --- Moving cards ---

    @Test
    void moveCard_shouldRejectIdeaStillInInbox() {
        KanbanItem idea = card(1L, KanbanItemState.IDEATION, null);
        when(kanbanItemService.findByIdAndTeamId(1L, 10L)).thenReturn(Optional.of(idea));

        assertThrows(ResponseStatusException.class, () -> boardService.moveCard(10L, 1L, 1L, 101L, 0));
    }

    @Test
    void moveCard_shouldRejectColumnFromAnotherTeam() {
        KanbanItem project = card(1L, KanbanItemState.IN_PRODUCTION, pesquisa);
        when(kanbanItemService.findByIdAndTeamId(1L, 10L)).thenReturn(Optional.of(project));
        when(boardColumnRepository.findByIdAndTeam_Id(999L, 10L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> boardService.moveCard(10L, 1L, 1L, 999L, 0));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertSame(pesquisa, project.getColumn());
    }

    @Test
    void moveCard_shouldChangeColumn() {
        KanbanItem project = card(1L, KanbanItemState.IN_PRODUCTION, pesquisa);
        when(kanbanItemService.findByIdAndTeamId(1L, 10L)).thenReturn(Optional.of(project));

        boardService.moveCard(10L, 1L, 1L, 101L, 0);

        assertSame(roteiro, project.getColumn());
    }

    // --- Tasks and progress ---

    @Test
    void progressOf_shouldBeZeroWithoutTasksAndRoundedPercentageOtherwise() {
        KanbanItem item = card(1L, KanbanItemState.IN_PRODUCTION, pesquisa);
        assertEquals(0, BoardService.progressOf(List.of()));
        assertEquals(33, BoardService.progressOf(List.of(task(1L, item, true), task(2L, item, false), task(3L, item, false))));
        assertEquals(100, BoardService.progressOf(List.of(task(1L, item, true))));
    }

    @Test
    void toggleTask_shouldFlipTaskAndStoreComputedProgress() {
        KanbanItem project = card(1L, KanbanItemState.IN_PRODUCTION, pesquisa);
        CardTask pending = task(7L, project, false);
        when(kanbanItemService.findByIdAndTeamId(1L, 10L)).thenReturn(Optional.of(project));
        when(cardTaskRepository.findByIdAndKanbanItem_Id(7L, 1L)).thenReturn(Optional.of(pending));
        when(cardTaskRepository.save(any(CardTask.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cardTaskRepository.findByKanbanItem_IdInOrderBySortOrderAscIdAsc(anyList())).thenReturn(List.of(pending));

        boardService.toggleTask(10L, 1L, 1L, 7L);

        assertTrue(pending.getCompleted());
        assertEquals(100, project.getProgress());
    }

    @Test
    void createIdea_shouldBeBlockedWhenRoleIsNotAllowed() {
        User reviewer = new User();
        reviewer.setId(2L);
        when(teamService.requireRole(eq10(), org.mockito.ArgumentMatchers.eq(2L), any(), any()))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Seu papel na equipe nao permite esta acao."));

        var dto = new br.edu.ifsp.tcc.application.dto.CreateCardDTO();
        dto.setTitle("Ideia");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> boardService.createIdea(10L, reviewer, dto));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(kanbanItemService, never()).save(any());
    }

    // --- Columns ---

    @Test
    void deleteColumn_shouldMoveCardsToTargetColumn() {
        KanbanItem inPesquisa = card(1L, KanbanItemState.IN_PRODUCTION, pesquisa);
        when(kanbanItemService.findByTeamId(10L)).thenReturn(List.of(inPesquisa));

        boardService.deleteColumn(10L, 1L, 100L, 101L);

        assertSame(roteiro, inPesquisa.getColumn());
        verify(boardColumnRepository).delete(pesquisa);
    }

    @Test
    void deleteColumn_shouldKeepAtLeastOneColumn() {
        when(boardColumnRepository.findByTeam_IdOrderBySortOrderAscIdAsc(10L)).thenReturn(List.of(pesquisa));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> boardService.deleteColumn(10L, 1L, 100L, null));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(boardColumnRepository, never()).delete(any());
    }
}
