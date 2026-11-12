package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.TagResponse;
import br.edu.ifsp.tcc.application.dto.UpdateTagDTO;
import br.edu.ifsp.tcc.application.entity.Tag;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.service.TagService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Component
public class UpdateTagUseCase {

    private final TeamService teamService;
    private final TagService tagService;

    public UpdateTagUseCase(TeamService teamService, TagService tagService) {
        this.teamService = teamService;
        this.tagService = tagService;
    }

    /** Partial update: only the fields that are sent change. */
    @Transactional
    public TagResponse execute(Long teamId, Long userId, Long tagId, UpdateTagDTO dto) {
        teamService.requireRole(teamId, userId, TeamRole.ADMIN);
        Tag tag = tagService.findTag(teamId, tagId);

        if (dto.getName() != null) {
            String name = dto.getName().trim();
            if (name.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O nome da etiqueta não pode ficar vazio.");
            }
            if (tagService.nameTaken(teamId, name, tagId)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma etiqueta com este nome na equipe.");
            }
            tag.setName(name);
        }
        if (dto.getColor() != null) tag.setColor(dto.getColor());
        return tagService.toTagResponse(tagService.save(tag));
    }
}
