package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.MaterialResponse;
import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.KanbanItemState;
import br.edu.ifsp.tcc.application.entity.Material;
import br.edu.ifsp.tcc.application.entity.TeamMember;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.service.BoardService;
import br.edu.ifsp.tcc.application.service.MaterialService;
import br.edu.ifsp.tcc.application.service.PlanService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UploadMaterialUseCaseTest {

    @Mock private TeamService teamService;
    @Mock private BoardService boardService;
    @Mock private MaterialService materialService;
    @Mock private PlanService planService;
    @InjectMocks private UploadMaterialUseCase useCase;

    private final MockMultipartFile file = new MockMultipartFile("file", "a.pdf", "application/pdf", "abc".getBytes());

    private User editor() {
        User user = new User();
        user.setId(1L);
        TeamMember member = new TeamMember();
        member.setUser(user);
        when(teamService.requireRole(10L, 1L, TeamRole.ADMIN, TeamRole.CREATOR)).thenReturn(member);
        return user;
    }

    private KanbanItem card(KanbanItemState state) {
        KanbanItem card = new KanbanItem();
        card.setId(3L);
        card.setState(state);
        return card;
    }

    @Test
    void execute_shouldAttachTheFileToTheProject() {
        User user = editor();
        KanbanItem card = card(KanbanItemState.IN_PRODUCTION);
        Material material = new Material();
        MaterialResponse response = mock(MaterialResponse.class);
        when(boardService.findCard(10L, 3L)).thenReturn(card);
        when(materialService.attach(card, user, file)).thenReturn(material);
        when(materialService.toResponse(material, 10L)).thenReturn(response);

        assertSame(response, useCase.execute(10L, 1L, 3L, file));
    }

    @Test
    void execute_shouldRefuseIdeas() {
        editor();
        when(boardService.findCard(10L, 3L)).thenReturn(card(KanbanItemState.IDEATION));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> useCase.execute(10L, 1L, 3L, file));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Converta a ideia em projeto antes de anexar materiais.", ex.getReason());
        verifyNoInteractions(materialService);
    }

    @Test
    void execute_shouldRefuseAMissingFile() {
        editor();
        when(boardService.findCard(10L, 3L)).thenReturn(card(KanbanItemState.IN_PRODUCTION));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> useCase.execute(10L, 1L, 3L, null));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Envie um arquivo não vazio no campo \"file\".", ex.getReason());
        verifyNoInteractions(materialService);
    }

    @Test
    void execute_shouldRefuseAnEmptyFile() {
        editor();
        when(boardService.findCard(10L, 3L)).thenReturn(card(KanbanItemState.IN_PRODUCTION));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> useCase.execute(10L, 1L, 3L, new MockMultipartFile("file", "a.pdf", "application/pdf", new byte[0])));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(materialService, never()).attach(any(), any(), any());
    }

    @Test
    void execute_shouldBeForbiddenForReviewers() {
        when(teamService.requireRole(10L, 1L, TeamRole.ADMIN, TeamRole.CREATOR))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Seu papel na equipe não permite esta ação."));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> useCase.execute(10L, 1L, 3L, file));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verifyNoInteractions(boardService, materialService);
    }

    @Test
    void execute_shouldBeNotFoundForACardOfAnotherTeam() {
        editor();
        when(boardService.findCard(10L, 3L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Card não encontrado."));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> useCase.execute(10L, 1L, 3L, file));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verifyNoInteractions(materialService);
    }


    @Test
    void execute_shouldRefuseAnUnsupportedTypeBeforeTheStorageCheck() {
        editor();
        KanbanItem card = card(KanbanItemState.IN_PRODUCTION);
        when(boardService.findCard(10L, 3L)).thenReturn(card);
        MockMultipartFile exe = new MockMultipartFile("file", "a.exe", "application/x-msdownload", "abc".getBytes());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> useCase.execute(10L, 1L, 3L, exe));
        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ex.getStatusCode());
        verifyNoInteractions(planService);
    }

    @Test
    void execute_shouldStopWith402WhenTheStorageOfThePlanWouldBeExceeded() {
        editor();
        KanbanItem card = card(KanbanItemState.IN_PRODUCTION);
        br.edu.ifsp.tcc.application.entity.Team team = new br.edu.ifsp.tcc.application.entity.Team();
        card.setTeam(team);
        when(boardService.findCard(10L, 3L)).thenReturn(card);
        doThrow(new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "limite")).when(planService).requireStorage(team, 3L);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> useCase.execute(10L, 1L, 3L, file));
        assertEquals(HttpStatus.PAYMENT_REQUIRED, ex.getStatusCode());
        verifyNoInteractions(materialService);
    }
}
