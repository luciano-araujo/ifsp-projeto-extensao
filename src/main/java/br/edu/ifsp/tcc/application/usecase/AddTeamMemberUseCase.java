package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.AddTeamMemberDTO;
import br.edu.ifsp.tcc.application.dto.TeamMemberResponse;
import br.edu.ifsp.tcc.application.entity.TeamMember;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.service.TeamService;
import br.edu.ifsp.tcc.application.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AddTeamMemberUseCase {

    private final TeamService teamService;
    private final UserService userService;

    public AddTeamMemberUseCase(TeamService teamService, UserService userService) {
        this.teamService = teamService;
        this.userService = userService;
    }

    @Transactional
    public TeamMemberResponse execute(Long teamId, Long userId, AddTeamMemberDTO dto) {
        TeamMember admin = teamService.requireRole(teamId, userId, TeamRole.ADMIN);

        User newUser = userService.findByEmail(dto.getEmail().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Nenhum usuario cadastrado com este e-mail."));

        if (teamService.findMemberByTeamIdAndUserId(teamId, newUser.getId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este usuario ja faz parte da equipe.");
        }

        TeamMember member = new TeamMember();
        member.setTeam(admin.getTeam());
        member.setUser(newUser);
        member.setRole(dto.getRole() != null ? dto.getRole() : TeamRole.CREATOR);
        return teamService.toMemberResponse(teamService.saveMember(member));
    }
}
