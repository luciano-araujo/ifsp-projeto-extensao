package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CardResponse;
import br.edu.ifsp.tcc.application.entity.IdeaVote;
import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.KanbanItemState;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.service.BoardService;
import br.edu.ifsp.tcc.application.service.KanbanItemService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListIdeasUseCaseTest {

    @Mock private TeamService teamService;
    @Mock private BoardService boardService;
    @Mock private KanbanItemService kanbanItemService;
    @InjectMocks private ListIdeasUseCase useCase;

    private KanbanItem item(Long id, KanbanItemState state) {
        KanbanItem item = new KanbanItem();
        item.setId(id);
        item.setState(state);
        return item;
    }

    private IdeaVote vote(KanbanItem item, Long userId) {
        User user = new User();
        user.setId(userId);
        IdeaVote vote = new IdeaVote();
        vote.setKanbanItem(item);
        vote.setUser(user);
        return vote;
    }

    private CardResponse emptyResponse(Long id) {
        return new CardResponse(id, 10L, "Ideia", null, "IDEA", null, 0, 0, 0, 0, null, null,
                List.of(), null, null, 0, false, null, null);
    }

    @Test
    void execute_shouldListOnlyIdeasNewestFirstWithVotes() {
        KanbanItem older = item(1L, KanbanItemState.IDEATION);
        KanbanItem newer = item(2L, KanbanItemState.IDEATION);
        KanbanItem project = item(3L, KanbanItemState.IN_PRODUCTION);
        when(kanbanItemService.findByTeamId(10L)).thenReturn(List.of(older, newer, project));
        when(boardService.findVotes(List.of(newer, older)))
                .thenReturn(List.of(vote(older, 1L), vote(older, 2L), vote(newer, 2L)));
        when(boardService.toCardResponse(any(KanbanItem.class), isNull(), anyList()))
                .thenAnswer(inv -> emptyResponse(((KanbanItem) inv.getArgument(0)).getId()));

        List<CardResponse> ideas = useCase.execute(10L, 1L);

        assertEquals(List.of(2L, 1L), ideas.stream().map(CardResponse::getId).toList());
        assertEquals(1, ideas.get(0).getVotes());
        assertFalse(ideas.get(0).getVotedByMe());
        assertEquals(2, ideas.get(1).getVotes());
        assertTrue(ideas.get(1).getVotedByMe());
        verify(teamService).requireMember(10L, 1L);
    }
}
