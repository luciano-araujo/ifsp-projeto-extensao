package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.CreateInviteDTO;
import br.edu.ifsp.tcc.application.dto.InviteResponse;
import br.edu.ifsp.tcc.application.service.InviteService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CreateInviteUseCase {

    private final InviteService inviteService;

    public CreateInviteUseCase(InviteService inviteService) {
        this.inviteService = inviteService;
    }

    /**
     * Invites an e-mail to the team (ADMIN). READ_COMMITTED + the team lock (taken inside the service) so the plan
     * check sees what concurrent requests committed.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public InviteResponse execute(Long teamId, Long userId, CreateInviteDTO dto) {
        return inviteService.createInvite(teamId, userId, dto);
    }
}
