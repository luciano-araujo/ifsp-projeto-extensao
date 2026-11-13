package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.entity.*;
import br.edu.ifsp.tcc.application.service.KanbanItemService;
import br.edu.ifsp.tcc.application.service.MaterialService;
import br.edu.ifsp.tcc.application.service.ScriptService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeleteKanbanItemUseCaseTest {

    @Mock private KanbanItemService kanbanItemService;
    @Mock private TeamService teamService;
    @Mock private MaterialService materialService;
    @InjectMocks private DeleteKanbanItemUseCase useCase;

    private static final String FORBIDDEN = "Seu papel na equipe não permite esta ação.";

    private KanbanItem card() {
        Team team = new Team();
        team.setId(10L);
        KanbanItem item = new KanbanItem();
        item.setId(1L);
        item.setTeam(team);
        return item;
    }

    private void notFound(Long userId) {
        when(kanbanItemService.findCardOfMember(1L, userId))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Card não encontrado."));
    }

    private void reviewer(KanbanItem item, Long userId) {
        when(kanbanItemService.findCardOfMember(1L, userId)).thenReturn(item);
        when(teamService.requireRole(10L, userId, TeamRole.ADMIN, TeamRole.CREATOR))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, FORBIDDEN));
    }

    @Test
    void execute_shouldDeleteWhenEditor() {
        when(kanbanItemService.findCardOfMember(1L, 2L)).thenReturn(card());

        useCase.execute(1L, 2L);

        verify(teamService).requireRole(10L, 2L, TeamRole.ADMIN, TeamRole.CREATOR);
        verify(materialService).scheduleFileRemovalOfCard(1L);
        verify(kanbanItemService).deleteById(1L);
    }

    @Test
    void execute_shouldReturn404ForMissingCardOrOtherTeam() {
        notFound(99L);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> useCase.execute(1L, 99L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("Card não encontrado.", ex.getReason());
        verify(kanbanItemService, never()).deleteById(any());
    }

    @Test
    void execute_shouldReturn403ForReviewer() {
        reviewer(card(), 3L);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> useCase.execute(1L, 3L));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertEquals(FORBIDDEN, ex.getReason());
        verify(kanbanItemService, never()).deleteById(any());
    }
}
