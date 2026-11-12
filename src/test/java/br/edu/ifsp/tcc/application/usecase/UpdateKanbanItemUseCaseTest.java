package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.UpdateKanbanItemDTO;
import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.Team;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.service.KanbanItemService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateKanbanItemUseCaseTest {

    @Mock private KanbanItemService kanbanItemService;
    @Mock private TeamService teamService;
    @InjectMocks private UpdateKanbanItemUseCase useCase;

    private KanbanItem existingItem() {
        Team team = new Team();
        team.setId(10L);
        KanbanItem item = new KanbanItem();
        item.setId(1L);
        item.setTitle("Original");
        item.setDescription("Original desc");
        item.setTeam(team);
        return item;
    }

    @Test
    void execute_shouldUpdateOnlyProvidedFields() {
        KanbanItem item = existingItem();
        when(kanbanItemService.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(item));
        when(kanbanItemService.save(item)).thenReturn(item);

        UpdateKanbanItemDTO dto = new UpdateKanbanItemDTO();
        dto.setTitle("Updated Title");
        dto.setTargetAudience("Alunos");
        dto.setPedagogicalObjective("Ensinar o basico");

        KanbanItem result = useCase.execute(1L, 1L, dto);

        assertEquals("Updated Title", result.getTitle());
        assertEquals("Original desc", result.getDescription());
        assertEquals("Alunos", result.getTargetAudience());
        assertEquals("Ensinar o basico", result.getPedagogicalObjective());
        verify(teamService).requireRole(10L, 1L, TeamRole.ADMIN, TeamRole.CREATOR);
    }

    @Test
    void execute_shouldUpdateDescription() {
        KanbanItem item = existingItem();
        when(kanbanItemService.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(item));
        when(kanbanItemService.save(item)).thenReturn(item);

        UpdateKanbanItemDTO dto = new UpdateKanbanItemDTO();
        dto.setDescription("Nova descricao");

        assertEquals("Nova descricao", useCase.execute(1L, 1L, dto).getDescription());
    }

    @Test
    void execute_shouldSetAndClearDueDate() {
        KanbanItem item = existingItem();
        when(kanbanItemService.findByIdAndUserId(1L, 1L)).thenReturn(Optional.of(item));
        when(kanbanItemService.save(item)).thenReturn(item);

        UpdateKanbanItemDTO setDate = new UpdateKanbanItemDTO();
        setDate.setDueDate("2026-11-30");
        assertEquals(LocalDate.of(2026, 11, 30), useCase.execute(1L, 1L, setDate).getDueDate());

        UpdateKanbanItemDTO keepDate = new UpdateKanbanItemDTO();
        keepDate.setTitle("Outro titulo");
        assertEquals(LocalDate.of(2026, 11, 30), useCase.execute(1L, 1L, keepDate).getDueDate());

        UpdateKanbanItemDTO clearDate = new UpdateKanbanItemDTO();
        clearDate.setDueDate("");
        assertNull(useCase.execute(1L, 1L, clearDate).getDueDate());
    }

    @Test
    void execute_shouldRejectReviewer() {
        KanbanItem item = existingItem();
        when(kanbanItemService.findByIdAndUserId(1L, 3L)).thenReturn(Optional.of(item));
        when(teamService.requireRole(10L, 3L, TeamRole.ADMIN, TeamRole.CREATOR))
                .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Seu papel na equipe não permite esta ação."));

        UpdateKanbanItemDTO dto = new UpdateKanbanItemDTO();
        dto.setTitle("x");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> useCase.execute(1L, 3L, dto));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        verify(kanbanItemService, never()).save(any());
        assertEquals("Original", item.getTitle());
    }

    @Test
    void execute_shouldThrowWhenItemNotFound() {
        when(kanbanItemService.findByIdAndUserId(1L, 99L)).thenReturn(Optional.empty());

        UpdateKanbanItemDTO dto = new UpdateKanbanItemDTO();
        dto.setTitle("x");

        assertThrows(RuntimeException.class, () -> useCase.execute(1L, 99L, dto));
        verifyNoInteractions(teamService);
    }
}
