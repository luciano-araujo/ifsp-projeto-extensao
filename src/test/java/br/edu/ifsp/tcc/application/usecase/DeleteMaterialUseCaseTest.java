package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.entity.Material;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.service.MaterialService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteMaterialUseCaseTest {

    @Mock private TeamService teamService;
    @Mock private MaterialService materialService;
    @InjectMocks private DeleteMaterialUseCase useCase;

    @Test
    void execute_shouldDeleteTheMaterialOfTheTeam() {
        Material material = new Material();
        when(materialService.findMaterial(10L, 7L)).thenReturn(material);

        useCase.execute(10L, 1L, 7L);

        verify(teamService).requireRole(10L, 1L, TeamRole.ADMIN, TeamRole.CREATOR);
        verify(materialService).delete(material);
    }

    @Test
    void execute_shouldBeNotFoundForAMaterialOfAnotherTeam() {
        when(materialService.findMaterial(10L, 7L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Material não encontrado."));

        assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, 1L, 7L));

        verify(materialService, never()).delete(any());
    }

    @Test
    void execute_shouldBeForbiddenForReviewers() {
        when(teamService.requireRole(10L, 1L, TeamRole.ADMIN, TeamRole.CREATOR))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Seu papel na equipe não permite esta ação."));

        assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, 1L, 7L));

        verifyNoInteractions(materialService);
    }
}
