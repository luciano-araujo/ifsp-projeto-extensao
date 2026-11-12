package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.TagResponse;
import br.edu.ifsp.tcc.application.entity.Tag;
import br.edu.ifsp.tcc.application.service.TagService;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListTagsUseCaseTest {

    @Mock private TeamService teamService;
    @Mock private TagService tagService;
    @InjectMocks private ListTagsUseCase useCase;

    @Test
    void execute_shouldListTagsForAnyMember() {
        Tag tutorial = new Tag();
        TagResponse mapped = new TagResponse(1L, "Tutorial", "#2E7D32");
        when(tagService.findTags(10L)).thenReturn(List.of(tutorial));
        when(tagService.toTagResponse(tutorial)).thenReturn(mapped);

        List<TagResponse> result = useCase.execute(10L, 1L);

        assertEquals(List.of(mapped), result);
        verify(teamService).requireMember(10L, 1L);
    }

    @Test
    void execute_shouldBlockNonMembers() {
        when(teamService.requireMember(10L, 9L))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não faz parte desta equipe."));

        assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, 9L));
        verifyNoInteractions(tagService);
    }
}
