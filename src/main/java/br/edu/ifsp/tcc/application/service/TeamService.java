package br.edu.ifsp.tcc.application.service;

import br.edu.ifsp.tcc.application.dto.TeamMemberResponse;
import br.edu.ifsp.tcc.application.dto.TeamResponse;
import br.edu.ifsp.tcc.application.entity.*;
import br.edu.ifsp.tcc.application.repository.BoardColumnRepository;
import br.edu.ifsp.tcc.application.repository.TeamMemberRepository;
import br.edu.ifsp.tcc.application.repository.TeamRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
public class TeamService {

    /** Default production columns from the scope document; each team can rename, add or remove them. */
    public static final List<String> DEFAULT_COLUMNS = List.of("Pesquisa", "Roteiro", "Gravação", "Edição");

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final BoardColumnRepository boardColumnRepository;

    public TeamService(TeamRepository teamRepository,
                       TeamMemberRepository teamMemberRepository,
                       BoardColumnRepository boardColumnRepository) {
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.boardColumnRepository = boardColumnRepository;
    }

    // --- Access verification ---

    public TeamMember requireMember(Long teamId, Long userId) {
        return teamMemberRepository.findByTeam_IdAndUser_Id(teamId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Voce nao faz parte desta equipe."));
    }

    public TeamMember requireRole(Long teamId, Long userId, TeamRole... allowed) {
        TeamMember member = requireMember(teamId, userId);
        if (!Arrays.asList(allowed).contains(member.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seu papel na equipe nao permite esta acao.");
        }
        return member;
    }

    // --- Teams ---

    /** Creates the team, its first ADMIN and the default columns. */
    public TeamMember createTeamWithAdmin(User user, String name) {
        Team team = new Team();
        team.setName(name);
        team = teamRepository.save(team);

        TeamMember admin = new TeamMember();
        admin.setTeam(team);
        admin.setUser(user);
        admin.setRole(TeamRole.ADMIN);
        admin = teamMemberRepository.save(admin);

        for (int i = 0; i < DEFAULT_COLUMNS.size(); i++) {
            BoardColumn column = new BoardColumn();
            column.setTeam(team);
            column.setName(DEFAULT_COLUMNS.get(i));
            column.setSortOrder(i);
            boardColumnRepository.save(column);
        }
        return admin;
    }

    /** Deletes the team; its members, columns, cards and courses go with it (database cascade). */
    public void deleteTeam(Team team) {
        teamRepository.delete(team);
    }

    public String personalTeamName(User user) {
        String firstName = user.getName() == null ? "" : user.getName().trim().split("\\s+")[0];
        return firstName.isEmpty() ? "Meu Estudio" : "Estúdio de " + firstName;
    }

    // --- Members ---

    public List<TeamMember> findMembershipsByUserId(Long userId) {
        return teamMemberRepository.findByUser_IdOrderByJoinedAtAsc(userId);
    }

    public List<TeamMember> findMembersByTeamId(Long teamId) {
        return teamMemberRepository.findByTeam_IdOrderByJoinedAtAsc(teamId);
    }

    public Optional<TeamMember> findMemberByTeamIdAndUserId(Long teamId, Long userId) {
        return teamMemberRepository.findByTeam_IdAndUser_Id(teamId, userId);
    }

    public TeamMember findMember(Long teamId, Long memberId) {
        return teamMemberRepository.findByIdAndTeam_Id(memberId, teamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membro nao encontrado."));
    }

    public int countMembers(Long teamId) {
        return teamMemberRepository.countByTeam_Id(teamId);
    }

    public int countAdmins(Long teamId) {
        return teamMemberRepository.countByTeam_IdAndRole(teamId, TeamRole.ADMIN);
    }

    public TeamMember saveMember(TeamMember member) {
        return teamMemberRepository.save(member);
    }

    public void deleteMember(TeamMember member) {
        teamMemberRepository.delete(member);
    }

    // --- Response mapping ---

    public TeamResponse toTeamResponse(TeamMember membership) {
        Team team = membership.getTeam();
        return new TeamResponse(team.getId(), team.getName(), membership.getRole(),
                countMembers(team.getId()), team.getCreatedAt());
    }

    public TeamMemberResponse toMemberResponse(TeamMember member) {
        User user = member.getUser();
        return new TeamMemberResponse(member.getId(), user.getId(), user.getName(), user.getEmail(),
                member.getRole(), member.getJoinedAt());
    }
}
