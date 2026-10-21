package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.IdeaVoteResponse;
import br.edu.ifsp.tcc.application.entity.IdeaVote;
import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.KanbanItemState;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.service.BoardService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ToggleIdeaVoteUseCaseTest {

    @Mock private TeamService teamService;
    @Mock private BoardService boardService;
    @InjectMocks private ToggleIdeaVoteUseCase useCase;

    private User user() {
        User user = new User();
        user.setId(2L);
        return user;
    }

    private KanbanItem card(KanbanItemState state) {
        KanbanItem item = new KanbanItem();
        item.setId(1L);
        item.setState(state);
        return item;
    }

    @Test
    void execute_shouldAddVoteWhenUserHasNotVoted() {
        User rui = user();
        KanbanItem idea = card(KanbanItemState.IDEATION);
        when(boardService.findCard(10L, 1L)).thenReturn(idea);
        when(boardService.findVote(1L, 2L)).thenReturn(Optional.empty());
        when(boardService.countVotes(1L)).thenReturn(3);

        IdeaVoteResponse response = useCase.execute(10L, rui, 1L);

        ArgumentCaptor<IdeaVote> saved = ArgumentCaptor.forClass(IdeaVote.class);
        verify(boardService).saveVote(saved.capture());
        assertSame(rui, saved.getValue().getUser());
        assertSame(idea, saved.getValue().getKanbanItem());
        assertEquals(3, response.getVotes());
        assertTrue(response.getVotedByMe());
        verify(teamService).requireMember(10L, 2L);
    }

    @Test
    void execute_shouldRemoveVoteWhenUserVotesAgain() {
        IdeaVote existing = new IdeaVote();
        when(boardService.findCard(10L, 1L)).thenReturn(card(KanbanItemState.IDEATION));
        when(boardService.findVote(1L, 2L)).thenReturn(Optional.of(existing));
        when(boardService.countVotes(1L)).thenReturn(2);

        IdeaVoteResponse response = useCase.execute(10L, user(), 1L);

        verify(boardService).deleteVote(existing);
        verify(boardService, never()).saveVote(any());
        assertEquals(2, response.getVotes());
        assertFalse(response.getVotedByMe());
    }

    @Test
    void execute_shouldRejectVoteOnProject() {
        when(boardService.findCard(10L, 1L)).thenReturn(card(KanbanItemState.IN_PRODUCTION));

        assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, user(), 1L));
        verify(boardService, never()).saveVote(any());
    }
}
