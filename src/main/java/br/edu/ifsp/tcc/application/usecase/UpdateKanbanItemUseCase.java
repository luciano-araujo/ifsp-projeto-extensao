package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.UpdateKanbanItemDTO;
import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.KanbanItemState;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.service.KanbanItemService;
import br.edu.ifsp.tcc.application.service.TagService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@Component
public class UpdateKanbanItemUseCase {

    private final KanbanItemService kanbanItemService;
    private final TeamService teamService;
    private final TagService tagService;

    public UpdateKanbanItemUseCase(KanbanItemService kanbanItemService, TeamService teamService,
                                  TagService tagService) {
        this.kanbanItemService = kanbanItemService;
        this.teamService = teamService;
        this.tagService = tagService;
    }

    /** Editors (ADMIN and CREATOR of the card's team) only; position and progress come from the board. */
    @Transactional
    public KanbanItem execute(Long itemId, Long userId, UpdateKanbanItemDTO dto) {
        KanbanItem item = kanbanItemService.findCardOfMember(itemId, userId);
        teamService.requireRole(item.getTeamId(), userId, TeamRole.ADMIN, TeamRole.CREATOR);

        boolean project = item.getState() != KanbanItemState.IDEATION;

        if (dto.getTitle() != null) {
            KanbanItemService.requireTitle(dto.getTitle());
            item.setTitle(dto.getTitle().trim());
        }
        if (dto.getDescription() != null) item.setDescription(dto.getDescription());
        if (dto.getTargetAudience() != null) {
            String audience = dto.getTargetAudience().trim();
            if (project) KanbanItemService.requireAudience(audience);
            // An idea may keep it empty
            item.setTargetAudience(audience.isEmpty() ? null : audience);
        }
        if (dto.getPedagogicalObjective() != null) {
            String objective = dto.getPedagogicalObjective().trim();
            if (project) KanbanItemService.requireObjective(objective);
            item.setPedagogicalObjective(objective.isEmpty() ? null : objective);
        }
        if (dto.getDueDate() != null) {
            item.setDueDate(dto.getDueDate().isEmpty() ? null : parseDate(dto.getDueDate()));
        }

        if (dto.getTagId() != null) {
            item.setTag(dto.getTagId() == 0L ? null : tagService.resolveTag(item.getTeamId(), dto.getTagId(), null));
        }

        return kanbanItemService.save(item);
    }

    private static LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw badRequest("Prazo inválido. Use o formato AAAA-MM-DD.");
        }
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
