package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.service.TagService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DeleteTagUseCase {

    private final TeamService teamService;
    private final TagService tagService;

    public DeleteTagUseCase(TeamService teamService, TagService tagService) {
        this.teamService = teamService;
        this.tagService = tagService;
    }

    /** Cards and ideas that used the tag stay without a tag (database ON DELETE SET NULL). */
    @Transactional
    public void execute(Long teamId, Long userId, Long tagId) {
        teamService.requireRole(teamId, userId, TeamRole.ADMIN);
        tagService.delete(tagService.findTag(teamId, tagId));
    }
}
