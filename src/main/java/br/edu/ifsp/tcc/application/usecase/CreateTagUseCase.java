package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CreateTagDTO;
import br.edu.ifsp.tcc.application.dto.TagResponse;
import br.edu.ifsp.tcc.application.entity.Tag;
import br.edu.ifsp.tcc.application.entity.Team;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.service.TagService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CreateTagUseCase {

    private final TeamService teamService;
    private final TagService tagService;

    public CreateTagUseCase(TeamService teamService, TagService tagService) {
        this.teamService = teamService;
        this.tagService = tagService;
    }

    /** Only administrators manage the tags; the name is unique inside the team (case-insensitive). */
    @Transactional
    public TagResponse execute(Long teamId, Long userId, CreateTagDTO dto) {
        Team team = teamService.requireRole(teamId, userId, TeamRole.ADMIN).getTeam();
        String name = dto.getName().trim();
        if (name.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O nome da etiqueta é obrigatório.");
        }
        if (tagService.nameTaken(teamId, name, null)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma etiqueta com este nome na equipe.");
        }

        Tag tag = new Tag();
        tag.setTeam(team);
        tag.setName(name);
        tag.setColor(dto.getColor());
        return tagService.toTagResponse(tagService.save(tag));
    }
}
