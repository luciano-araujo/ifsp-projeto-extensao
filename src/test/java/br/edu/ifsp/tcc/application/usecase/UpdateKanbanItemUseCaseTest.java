package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.UpdateKanbanItemDTO;
import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.KanbanItemState;
import br.edu.ifsp.tcc.application.entity.Team;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.service.KanbanItemService;
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

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UpdateKanbanItemUseCaseTest {

    @Mock private KanbanItemService kanbanItemService;
    @Mock private TeamService teamService;
    @Mock private TagService tagService;
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
        when(kanbanItemService.findCardOfMember(1L, 1L)).thenReturn(item);
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
        when(kanbanItemService.findCardOfMember(1L, 1L)).thenReturn(item);
        when(kanbanItemService.save(item)).thenReturn(item);

        UpdateKanbanItemDTO dto = new UpdateKanbanItemDTO();
        dto.setDescription("Nova descricao");

        assertEquals("Nova descricao", useCase.execute(1L, 1L, dto).getDescription());
    }

    @Test
    void execute_shouldSetAndClearDueDate() {
        KanbanItem item = existingItem();
        when(kanbanItemService.findCardOfMember(1L, 1L)).thenReturn(item);
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
    void execute_shouldRejectTitleShorterThanThreeCharacters() {
        KanbanItem idea = existingItem();
        idea.setState(KanbanItemState.IDEATION);
        UpdateKanbanItemDTO dto = new UpdateKanbanItemDTO();
        dto.setTitle(" ab ");
        assertBadRequest(idea, dto, "O título deve ter pelo menos 3 caracteres.");
        assertEquals("Original", idea.getTitle());
    }

    @Test
    void execute_shouldEditIdeaWithEmptyAudienceAndObjectiveStoredAsNull() {
        KanbanItem idea = existingItem();
        idea.setState(KanbanItemState.IDEATION);
        idea.setTargetAudience("Antigo");
        idea.setPedagogicalObjective("Objetivo antigo");
        when(kanbanItemService.findCardOfMember(1L, 1L)).thenReturn(idea);
        when(kanbanItemService.save(idea)).thenReturn(idea);

        UpdateKanbanItemDTO dto = new UpdateKanbanItemDTO();
        dto.setTitle("  Ideia nova ");
        dto.setDescription("Outra descricao");
        dto.setTargetAudience("   ");
        dto.setPedagogicalObjective("");

        KanbanItem result = useCase.execute(1L, 1L, dto);

        assertEquals("Ideia nova", result.getTitle());
        assertEquals("Outra descricao", result.getDescription());
        assertNull(result.getTargetAudience());
        assertNull(result.getPedagogicalObjective());
        verify(teamService).requireRole(10L, 1L, TeamRole.ADMIN, TeamRole.CREATOR);
    }

    @Test
    void execute_shouldChangeKeepAndRemoveTag() {
        KanbanItem item = existingItem();
        Tag old = new Tag();
        old.setId(5L);
        item.setTag(old);
        Tag podcast = new Tag();
        podcast.setId(7L);
        when(kanbanItemService.findCardOfMember(1L, 1L)).thenReturn(item);
        when(kanbanItemService.save(item)).thenReturn(item);
        when(tagService.resolveTag(10L, 7L, null)).thenReturn(podcast);

        UpdateKanbanItemDTO keep = new UpdateKanbanItemDTO();
        keep.setTitle("Mesmo titulo");
        assertSame(old, useCase.execute(1L, 1L, keep).getTag());
        verifyNoInteractions(tagService);

        UpdateKanbanItemDTO change = new UpdateKanbanItemDTO();
        change.setTagId(7L);
        KanbanItem changed = useCase.execute(1L, 1L, change);
        assertSame(podcast, changed.getTag());
        assertEquals(7L, changed.getTagId());

        UpdateKanbanItemDTO remove = new UpdateKanbanItemDTO();
        remove.setTagId(0L);
        assertNull(useCase.execute(1L, 1L, remove).getTag());
        verify(tagService, times(1)).resolveTag(anyLong(), anyLong(), any());
    }

    @Test
    void execute_shouldRejectTagOfAnotherTeam() {
        KanbanItem item = existingItem();
        when(kanbanItemService.findCardOfMember(1L, 1L)).thenReturn(item);
        when(tagService.resolveTag(10L, 99L, null))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Etiqueta inválida para esta equipe."));

        UpdateKanbanItemDTO dto = new UpdateKanbanItemDTO();
        dto.setTagId(99L);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> useCase.execute(1L, 1L, dto));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(kanbanItemService, never()).save(any());
        assertNull(item.getTag());
    }

    @Test
    void execute_shouldRejectReviewer() {
        KanbanItem item = existingItem();
        when(kanbanItemService.findCardOfMember(1L, 3L)).thenReturn(item);
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
    void execute_shouldReturn404ForMissingCardOrOtherTeam() {
        when(kanbanItemService.findCardOfMember(1L, 99L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Card não encontrado."));

        UpdateKanbanItemDTO dto = new UpdateKanbanItemDTO();
        dto.setTitle("x");

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> useCase.execute(1L, 99L, dto));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verifyNoInteractions(teamService);
    }

    private KanbanItem project() {
        KanbanItem item = existingItem();
        item.setState(KanbanItemState.IN_PRODUCTION);
        item.setTargetAudience("Estudantes iniciantes");
        item.setPedagogicalObjective("Ensinar o básico de lógica");
        return item;
    }

    private void assertBadRequest(KanbanItem item, UpdateKanbanItemDTO dto, String reason) {
        when(kanbanItemService.findCardOfMember(1L, 1L)).thenReturn(item);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> useCase.execute(1L, 1L, dto));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals(reason, ex.getReason());
        verify(kanbanItemService, never()).save(any());
    }

    @Test
    void execute_shouldRejectBlankTitle() {
        KanbanItem item = project();
        UpdateKanbanItemDTO dto = new UpdateKanbanItemDTO();
        dto.setTitle("   ");
        assertBadRequest(item, dto, "O título deve ter pelo menos 3 caracteres.");
        assertEquals("Original", item.getTitle());
    }

    @Test
    void execute_shouldRejectShortOrEmptyAudienceOfProject() {
        KanbanItem item = project();
        UpdateKanbanItemDTO empty = new UpdateKanbanItemDTO();
        empty.setTargetAudience("");
        assertBadRequest(item, empty, "Informe o público-alvo com pelo menos 3 caracteres.");

        UpdateKanbanItemDTO shortOne = new UpdateKanbanItemDTO();
        shortOne.setTargetAudience(" ab ");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> useCase.execute(1L, 1L, shortOne));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Estudantes iniciantes", item.getTargetAudience());
    }

    @Test
    void execute_shouldRejectShortOrEmptyObjectiveOfProject() {
        KanbanItem item = project();
        UpdateKanbanItemDTO dto = new UpdateKanbanItemDTO();
        dto.setPedagogicalObjective("curto");
        assertBadRequest(item, dto, "Informe o objetivo pedagógico com pelo menos 10 caracteres.");
        assertEquals("Ensinar o básico de lógica", item.getPedagogicalObjective());
    }

    @Test
    void execute_shouldLeaveOmittedFieldsOfProjectUnchanged() {
        KanbanItem item = project();
        when(kanbanItemService.findCardOfMember(1L, 1L)).thenReturn(item);
        when(kanbanItemService.save(item)).thenReturn(item);
        UpdateKanbanItemDTO dto = new UpdateKanbanItemDTO();
        dto.setDescription("Nova");

        KanbanItem result = useCase.execute(1L, 1L, dto);

        assertEquals("Estudantes iniciantes", result.getTargetAudience());
        assertEquals("Ensinar o básico de lógica", result.getPedagogicalObjective());
        assertEquals("Original", result.getTitle());
    }

    @Test
    void execute_shouldTrimValidTextOfProject() {
        KanbanItem item = project();
        when(kanbanItemService.findCardOfMember(1L, 1L)).thenReturn(item);
        when(kanbanItemService.save(item)).thenReturn(item);
        UpdateKanbanItemDTO dto = new UpdateKanbanItemDTO();
        dto.setTitle("  Novo título ");
        dto.setTargetAudience(" Professores ");
        dto.setPedagogicalObjective(" Mostrar boas práticas ");

        KanbanItem result = useCase.execute(1L, 1L, dto);

        assertEquals("Novo título", result.getTitle());
        assertEquals("Professores", result.getTargetAudience());
        assertEquals("Mostrar boas práticas", result.getPedagogicalObjective());
    }

    @Test
    void execute_shouldLetAnIdeaKeepAudienceAndObjectiveEmpty() {
        KanbanItem idea = existingItem();
        idea.setTargetAudience("Alunos");
        idea.setPedagogicalObjective("Ensinar algo");
        when(kanbanItemService.findCardOfMember(1L, 1L)).thenReturn(idea);
        when(kanbanItemService.save(idea)).thenReturn(idea);
        UpdateKanbanItemDTO dto = new UpdateKanbanItemDTO();
        dto.setTargetAudience("");
        dto.setPedagogicalObjective("  ");

        KanbanItem result = useCase.execute(1L, 1L, dto);

        assertNull(result.getTargetAudience());
        assertNull(result.getPedagogicalObjective());
    }

    @Test
    void execute_shouldRejectImpossibleDueDate() {
        KanbanItem item = project();
        UpdateKanbanItemDTO dto = new UpdateKanbanItemDTO();
        dto.setDueDate("2026-02-30");
        assertBadRequest(item, dto, "Prazo inválido. Use o formato AAAA-MM-DD.");
    }
}
