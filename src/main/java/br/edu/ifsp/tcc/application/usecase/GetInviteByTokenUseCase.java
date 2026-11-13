package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.PublicInviteResponse;
import br.edu.ifsp.tcc.application.service.InviteService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class GetInviteByTokenUseCase {

    private final InviteService inviteService;

    public GetInviteByTokenUseCase(InviteService inviteService) {
        this.inviteService = inviteService;
    }

    /** Public lookup used by the /convite page: 404 when the token is unknown, already used or expired. */
    @Transactional(readOnly = true)
    public PublicInviteResponse execute(String token) {
        return inviteService.toPublicResponse(inviteService.findOpenByToken(token));
    }
}
