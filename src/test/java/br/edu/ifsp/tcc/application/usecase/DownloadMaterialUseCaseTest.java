package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.MaterialDownload;
import br.edu.ifsp.tcc.application.entity.Material;
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
class DownloadMaterialUseCaseTest {

    @Mock private TeamService teamService;
    @Mock private MaterialService materialService;
    @InjectMocks private DownloadMaterialUseCase useCase;

    @Test
    void execute_shouldOpenTheMaterialOfTheTeamAsAttachment() {
        Material material = new Material();
        MaterialDownload download = mock(MaterialDownload.class);
        when(materialService.findMaterial(10L, 7L)).thenReturn(material);
        when(materialService.open(material, false)).thenReturn(download);

        assertSame(download, useCase.execute(10L, 1L, 7L, false));

        verify(teamService).requireMember(10L, 1L);
    }

    @Test
    void execute_shouldOpenInlineWhenAsked() {
        Material material = new Material();
        MaterialDownload download = mock(MaterialDownload.class);
        when(materialService.findMaterial(10L, 7L)).thenReturn(material);
        when(materialService.open(material, true)).thenReturn(download);

        assertSame(download, useCase.execute(10L, 1L, 7L, true));
    }

    @Test
    void execute_shouldBeNotFoundForAMaterialOfAnotherTeam() {
        when(materialService.findMaterial(10L, 7L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Material não encontrado."));

        assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, 1L, 7L, false));

        verify(materialService, never()).open(any(), anyBoolean());
    }

    @Test
    void execute_shouldBeForbiddenForNonMembers() {
        when(teamService.requireMember(10L, 1L))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não faz parte desta equipe."));

        assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, 1L, 7L, false));

        verifyNoInteractions(materialService);
    }
}
