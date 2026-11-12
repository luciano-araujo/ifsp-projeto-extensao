package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.TagResponse;
import br.edu.ifsp.tcc.application.service.TagService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class ListTagsUseCase {

    private final TeamService teamService;
    private final TagService tagService;

    public ListTagsUseCase(TeamService teamService, TagService tagService) {
        this.teamService = teamService;
        this.tagService = tagService;
    }

    /** Any member sees the tags of the team. */
    @Transactional(readOnly = true)
    public List<TagResponse> execute(Long teamId, Long userId) {
        teamService.requireMember(teamId, userId);
        return tagService.findTags(teamId).stream().map(tagService::toTagResponse).toList();
    }
}
