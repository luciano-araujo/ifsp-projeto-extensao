package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CreateInviteDTO;
import br.edu.ifsp.tcc.application.dto.InviteResponse;
import br.edu.ifsp.tcc.application.entity.InviteStatus;
import br.edu.ifsp.tcc.application.entity.TeamInvite;
import br.edu.ifsp.tcc.application.entity.TeamMember;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.service.AfterCommit;
import br.edu.ifsp.tcc.application.service.EmailService;
import br.edu.ifsp.tcc.application.service.InviteService;
import br.edu.ifsp.tcc.application.service.PlanService;
import br.edu.ifsp.tcc.application.service.TeamService;
import br.edu.ifsp.tcc.application.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Component
public class CreateInviteUseCase {

    private final TeamService teamService;
    private final UserService userService;
    private final InviteService inviteService;
    private final EmailService emailService;
    private final PlanService planService;

    public CreateInviteUseCase(TeamService teamService,
                               UserService userService,
                               InviteService inviteService,
                               EmailService emailService,
                               PlanService planService) {
        this.teamService = teamService;
        this.userService = userService;
        this.inviteService = inviteService;
        this.emailService = emailService;
        this.planService = planService;
    }

    /**
     * Invites an e-mail to the team (ADMIN). 409 when the e-mail already belongs to a member or already has an
     * open invite; an expired pending invite for the same e-mail is replaced.
     */
    @Transactional
    public InviteResponse execute(Long teamId, Long userId, CreateInviteDTO dto) {
        TeamMember admin = teamService.requireRole(teamId, userId, TeamRole.ADMIN);
        planService.requireCanInvite(admin.getTeam());
        String email = dto.getEmail().trim();

        User existing = userService.findByEmail(email).orElse(null);
        if (existing != null && teamService.findMemberByTeamIdAndUserId(teamId, existing.getId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este usuário já faz parte da equipe.");
        }

        List<TeamInvite> pending = inviteService.findPendingByTeamAndEmail(teamId, email);
        if (pending.stream().anyMatch(TeamInvite::isOpen)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um convite pendente para este e-mail.");
        }
        inviteService.deleteAll(pending);

        TeamRole role = dto.getRole() != null ? dto.getRole() : TeamRole.CREATOR;
        TeamInvite invite = new TeamInvite();
        invite.setTeam(admin.getTeam());
        invite.setEmail(email);
        invite.setRole(role);
        invite.setStatus(InviteStatus.PENDING);
        invite.setInvitedBy(admin.getUser());
        String token = inviteService.generateToken();
        invite = inviteService.create(invite, token);

        String inviterName = admin.getUser().getName();
        String teamName = admin.getTeam().getName();
        AfterCommit.run(() -> emailService.sendTeamInvite(email, inviterName, teamName,
                roleLabel(role), token));
        return inviteService.toInviteResponse(invite);
    }

    static String roleLabel(TeamRole role) {
        return switch (role) {
            case ADMIN -> "Administrador";
            case CREATOR -> "Criador";
            case REVIEWER -> "Revisor";
        };
    }
}
