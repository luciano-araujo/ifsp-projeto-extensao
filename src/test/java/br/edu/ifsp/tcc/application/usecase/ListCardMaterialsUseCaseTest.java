package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.MaterialResponse;
import br.edu.ifsp.tcc.application.entity.Material;
import br.edu.ifsp.tcc.application.service.BoardService;
import br.edu.ifsp.tcc.application.service.MaterialService;
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
class ListCardMaterialsUseCaseTest {

    @Mock private TeamService teamService;
    @Mock private BoardService boardService;
    @Mock private MaterialService materialService;
    @InjectMocks private ListCardMaterialsUseCase useCase;

    @Test
    void execute_shouldMapTheMaterialsOfTheCardInTheOrderGiven() {
        Material newer = new Material();
        Material older = new Material();
        MaterialResponse newerResponse = mock(MaterialResponse.class);
        MaterialResponse olderResponse = mock(MaterialResponse.class);
        when(materialService.findByCard(3L)).thenReturn(List.of(newer, older));
        when(materialService.toResponse(newer, 10L)).thenReturn(newerResponse);
        when(materialService.toResponse(older, 10L)).thenReturn(olderResponse);

        List<MaterialResponse> result = useCase.execute(10L, 1L, 3L);

        assertEquals(List.of(newerResponse, olderResponse), result);
        verify(teamService).requireMember(10L, 1L);
        verify(boardService).findCard(10L, 3L);
    }

    @Test
    void execute_shouldReturnAnEmptyListWhenThereAreNoMaterials() {
        when(materialService.findByCard(3L)).thenReturn(List.of());

        assertTrue(useCase.execute(10L, 1L, 3L).isEmpty());
    }

    @Test
    void execute_shouldBeNotFoundForACardOfAnotherTeam() {
        when(boardService.findCard(10L, 3L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Card não encontrado."));

        assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, 1L, 3L));

        verifyNoInteractions(materialService);
    }

    @Test
    void execute_shouldBeForbiddenForNonMembers() {
        when(teamService.requireMember(10L, 1L))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não faz parte desta equipe."));

        assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, 1L, 3L));

        verifyNoInteractions(boardService, materialService);
    }
}
