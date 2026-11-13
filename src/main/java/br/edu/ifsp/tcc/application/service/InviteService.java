package br.edu.ifsp.tcc.application.service;

import br.edu.ifsp.tcc.application.dto.InviteResponse;
import br.edu.ifsp.tcc.application.dto.InviterResponse;
import br.edu.ifsp.tcc.application.dto.PublicInviteResponse;
import br.edu.ifsp.tcc.application.entity.InviteStatus;
import br.edu.ifsp.tcc.application.entity.TeamInvite;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.repository.TeamInviteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

@Service
public class InviteService {

    /** Product decision 6: invites are valid for 7 days. */
    public static final int VALID_DAYS = 7;

    static final String NOT_FOUND_MESSAGE = "Convite não encontrado ou expirado.";

    private final TeamInviteRepository teamInviteRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public InviteService(TeamInviteRepository teamInviteRepository) {
        this.teamInviteRepository = teamInviteRepository;
    }

    /** 256 random bits, URL-safe; only its hash is stored. */
    public String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** SHA-256 of the token, in hex (64 characters). */
    public static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    /** Stores the invite with the hash of the token and the 7-day expiry; the token itself is never stored. */
    public TeamInvite create(TeamInvite invite, String token) {
        invite.setTokenHash(hash(token));
        invite.setExpiresAt(LocalDateTime.now().plusDays(VALID_DAYS));
        return teamInviteRepository.save(invite);
    }

    public List<TeamInvite> findOpenByTeamId(Long teamId) {
        return teamInviteRepository.findByTeam_IdAndStatusAndExpiresAtAfterOrderByCreatedAtDescIdDesc(
                teamId, InviteStatus.PENDING, LocalDateTime.now());
    }

    /** Pending invites of the team for the e-mail, expired ones included. */
    public List<TeamInvite> findPendingByTeamAndEmail(Long teamId, String email) {
        return teamInviteRepository.findByTeam_IdAndEmailIgnoreCaseAndStatus(teamId, email, InviteStatus.PENDING);
    }

    public TeamInvite findOfTeam(Long teamId, Long inviteId) {
        return teamInviteRepository.findByIdAndTeam_Id(inviteId, teamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Convite não encontrado."));
    }

    /** The usable invite for the token; unknown, accepted and expired invites are all 404. */
    public TeamInvite findOpenByToken(String token) {
        return teamInviteRepository.findByTokenHash(hash(token))
                .filter(TeamInvite::isOpen)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, NOT_FOUND_MESSAGE));
    }

    public void delete(TeamInvite invite) {
        teamInviteRepository.delete(invite);
    }

    public void deleteAll(List<TeamInvite> invites) {
        teamInviteRepository.deleteAll(invites);
    }

    public void markAccepted(TeamInvite invite) {
        invite.setStatus(InviteStatus.ACCEPTED);
        teamInviteRepository.save(invite);
    }

    /** Removes the pending invites that expired before the given moment; returns how many were removed. */
    public int deleteExpired(LocalDateTime now) {
        return teamInviteRepository.deleteExpired(now);
    }

    public InviteResponse toInviteResponse(TeamInvite invite) {
        User inviter = invite.getInvitedBy();
        return new InviteResponse(invite.getId(), invite.getEmail(), invite.getRole(), invite.getStatus(),
                inviter == null ? null : new InviterResponse(inviter.getId(), inviter.getName()),
                invite.getCreatedAt(), invite.getExpiresAt());
    }

    public PublicInviteResponse toPublicResponse(TeamInvite invite) {
        User inviter = invite.getInvitedBy();
        return new PublicInviteResponse(invite.getTeam().getName(), invite.getRole(), invite.getEmail(),
                inviter == null ? null : inviter.getName(), invite.getExpiresAt());
    }
}
