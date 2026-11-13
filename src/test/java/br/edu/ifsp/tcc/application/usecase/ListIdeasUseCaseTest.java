package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CardResponse;
import br.edu.ifsp.tcc.application.dto.ReviewStatus;
import br.edu.ifsp.tcc.application.entity.IdeaVote;
import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.KanbanItemState;
import br.edu.ifsp.tcc.application.entity.Tag;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.service.BoardService;
import br.edu.ifsp.tcc.application.service.KanbanItemService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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

    private KanbanItem idea(Long id, String title, String description, Long tagId) {
        KanbanItem item = item(id, KanbanItemState.IDEATION);
        item.setTitle(title);
        item.setDescription(description);
        if (tagId != null) {
            Tag tag = new Tag();
            tag.setId(tagId);
            item.setTag(tag);
        }
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
        return new CardResponse(id, "uuid-" + id, 10L, "Ideia", null, "IDEA", null, 0, 0, 0, 0, null, null,
                List.of(), null, null, 0, false, null, null, null, null, null, ReviewStatus.NONE);
    }

    private void givenResponses() {
        when(boardService.toCardResponse(any(KanbanItem.class), isNull(), anyList()))
                .thenAnswer(inv -> emptyResponse(((KanbanItem) inv.getArgument(0)).getId()));
    }

    private List<Long> ids(List<CardResponse> responses) {
        return responses.stream().map(CardResponse::getId).toList();
    }

    @Test
    void execute_shouldListOnlyIdeasNewestFirstWithVotes() {
        KanbanItem older = item(1L, KanbanItemState.IDEATION);
        KanbanItem newer = item(2L, KanbanItemState.IDEATION);
        KanbanItem project = item(3L, KanbanItemState.IN_PRODUCTION);
        when(kanbanItemService.findByTeamId(10L)).thenReturn(List.of(older, newer, project));
        when(boardService.findVotes(List.of(older, newer)))
                .thenReturn(List.of(vote(older, 1L), vote(older, 2L), vote(newer, 2L)));
        givenResponses();

        List<CardResponse> ideas = useCase.execute(10L, 1L, null, null, null);

        assertEquals(List.of(2L, 1L), ids(ideas));
        assertEquals(1, ideas.get(0).getVotes());
        assertFalse(ideas.get(0).getVotedByMe());
        assertEquals(2, ideas.get(1).getVotes());
        assertTrue(ideas.get(1).getVotedByMe());
        verify(teamService).requireMember(10L, 1L);
    }

    @Test
    void execute_shouldSortByVotesThenNewest() {
        KanbanItem a = item(1L, KanbanItemState.IDEATION);
        KanbanItem b = item(2L, KanbanItemState.IDEATION);
        KanbanItem c = item(3L, KanbanItemState.IDEATION);
        when(kanbanItemService.findByTeamId(10L)).thenReturn(List.of(a, b, c));
        // a has 2 votes, b and c have 1: a first, then the newer of the tie (c before b)
        when(boardService.findVotes(List.of(a, b, c)))
                .thenReturn(List.of(vote(a, 1L), vote(a, 2L), vote(b, 1L), vote(c, 2L)));
        givenResponses();

        List<CardResponse> ideas = useCase.execute(10L, 1L, null, null, "votes");

        assertEquals(List.of(1L, 3L, 2L), ids(ideas));
    }

    @Test
    void execute_shouldAcceptRecentSortInAnyCase() {
        KanbanItem older = item(1L, KanbanItemState.IDEATION);
        KanbanItem newer = item(2L, KanbanItemState.IDEATION);
        when(kanbanItemService.findByTeamId(10L)).thenReturn(List.of(older, newer));
        when(boardService.findVotes(anyList())).thenReturn(List.of());
        givenResponses();

        assertEquals(List.of(2L, 1L), ids(useCase.execute(10L, 1L, "  ", null, " Recent ")));
    }

    @Test
    void execute_shouldSearchTitleAndDescriptionIgnoringCase() {
        KanbanItem inTitle = idea(1L, "Planilhas com PROCV", null, null);
        KanbanItem inDescription = idea(2L, "Outra", "usa o procv e o procx", null);
        KanbanItem noMatch = idea(3L, "Podcast", "sem relaÃ§Ã£o", null);
        KanbanItem noText = idea(4L, "Sem descriÃ§Ã£o", null, null);
        when(kanbanItemService.findByTeamId(10L)).thenReturn(List.of(inTitle, inDescription, noMatch, noText));
        when(boardService.findVotes(List.of(inTitle, inDescription))).thenReturn(List.of());
        givenResponses();

        List<CardResponse> ideas = useCase.execute(10L, 1L, " ProcV ", null, null);

        assertEquals(List.of(2L, 1L), ids(ideas));
    }

    @Test
    void execute_shouldFilterByTag() {
        KanbanItem tagged = idea(1L, "A", null, 3L);
        KanbanItem otherTag = idea(2L, "B", null, 4L);
        KanbanItem untagged = idea(3L, "C", null, null);
        when(kanbanItemService.findByTeamId(10L)).thenReturn(List.of(tagged, otherTag, untagged));
        when(boardService.findVotes(List.of(tagged))).thenReturn(List.of());
        givenResponses();

        assertEquals(List.of(1L), ids(useCase.execute(10L, 1L, null, 3L, null)));
    }

    @Test
    void execute_shouldRejectInvalidSort() {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> useCase.execute(10L, 1L, null, null, "alfabetica"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verifyNoInteractions(kanbanItemService);
    }
}
