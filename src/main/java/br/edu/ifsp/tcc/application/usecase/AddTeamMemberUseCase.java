package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.AddTeamMemberDTO;
import br.edu.ifsp.tcc.application.dto.CreateInviteDTO;
import br.edu.ifsp.tcc.application.dto.InviteResponse;
import br.edu.ifsp.tcc.application.service.InviteService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Decision 22: "add member" no longer inserts a membership. It creates an invite with exactly the rules of
 * POST /teams/{t}/invites; the user joins only by accepting it.
 */
@Component
public class AddTeamMemberUseCase {

    private final InviteService inviteService;

    public AddTeamMemberUseCase(InviteService inviteService) {
        this.inviteService = inviteService;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public InviteResponse execute(Long teamId, Long userId, AddTeamMemberDTO dto) {
        CreateInviteDTO invite = new CreateInviteDTO();
        invite.setEmail(dto.getEmail());
        invite.setRole(dto.getRole());
        return inviteService.createInvite(teamId, userId, invite);
    }
}
