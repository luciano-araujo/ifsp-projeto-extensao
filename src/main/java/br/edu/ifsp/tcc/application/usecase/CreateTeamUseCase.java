package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CreateTeamDTO;
import br.edu.ifsp.tcc.application.dto.TeamResponse;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CreateTeamUseCase {

    private final TeamService teamService;

    public CreateTeamUseCase(TeamService teamService) {
        this.teamService = teamService;
    }

    @Transactional
    public TeamResponse execute(User user, CreateTeamDTO dto) {
        return teamService.toTeamResponse(teamService.createTeamWithAdmin(user, dto.getName().trim()));
    }
}
