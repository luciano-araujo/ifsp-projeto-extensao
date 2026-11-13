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

import java.time.LocalDateTime;
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

    private final TagService tagService;
    private final MaterialService materialService;
    private final KanbanItemService kanbanItemService;
    private final PlanService planService;

    public TeamService(TeamRepository teamRepository,
                       TeamMemberRepository teamMemberRepository,
                       BoardColumnRepository boardColumnRepository,
                       TagService tagService,
                       MaterialService materialService,
                       KanbanItemService kanbanItemService,
                       PlanService planService) {
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.boardColumnRepository = boardColumnRepository;
        this.tagService = tagService;
        this.materialService = materialService;
        this.kanbanItemService = kanbanItemService;
        this.planService = planService;
    }

    // --- Access verification ---

    public TeamMember requireMember(Long teamId, Long userId) {
        return teamMemberRepository.findByTeam_IdAndUser_Id(teamId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não faz parte desta equipe."));
    }

    public TeamMember requireRole(Long teamId, Long userId, TeamRole... allowed) {
        TeamMember member = requireMember(teamId, userId);
        if (!Arrays.asList(allowed).contains(member.getRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seu papel na equipe não permite esta ação.");
        }
        return member;
    }

    // --- Teams ---

    /** Creates the team, its first ADMIN and the default columns. */
    public TeamMember createTeamWithAdmin(User user, String name) {
        return createTeam(user, name, false);
    }

    /** The automatic personal team of a user (cannot be deleted, see requireDeletable). */
    public TeamMember createPersonalTeam(User user) {
        return createTeam(user, personalTeamName(user), true);
    }

    private TeamMember createTeam(User user, String name, boolean personal) {
        Team team = new Team();
        team.setName(name);
        team.setPersonal(personal);
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
        tagService.createDefaultTags(team);
        return admin;
    }


    public Team renameTeam(Team team, String name) {
        team.setName(name);
        return teamRepository.save(team);
    }

    /**
     * Product rule confirmed by Luciano: the personal team cannot be deleted. To change the rule,
     * remove or edit this single check.
     */
    public void requireDeletable(Team team) {
        if (team.isPersonal()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A equipe pessoal não pode ser excluída.");
        }
    }

    /**
     * Deletes the team; its members, columns, cards, materials and courses go with it (database cascade).
     * The stored files of the materials are removed after the commit.
     * <p>
     * The cards are deleted first, in their own statement. A single DELETE of the team reaches kanban_items by three
     * paths (team -> cards CASCADE, columns -> cards SET NULL, tags -> cards SET NULL). The local MySQL 9.x has
     * innodb_native_foreign_keys=OFF, so foreign keys are enforced in the SQL layer, and in that diamond the cascade
     * from cards to their children stopped before the end: rows were left pointing to cards that no longer existed.
     * Deleting the cards first keeps each cascade to a single path. Every team deletion must go through this method.
     */
    public void deleteTeam(Team team) {
        materialService.scheduleFileRemovalOfTeam(team.getId());
        kanbanItemService.deleteAllByTeamId(team.getId());
        teamRepository.delete(team);
    }

    public String personalTeamName(User user) {
        String firstName = user.getName() == null ? "" : user.getName().trim().split("\\s+")[0];
        return firstName.isEmpty() ? "Meu Estúdio" : "Estúdio de " + firstName;
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membro não encontrado."));
    }

    /** The member of the team, or empty when the id does not belong to it. */
    public Optional<TeamMember> findMemberOfTeam(Long teamId, Long memberId) {
        return teamMemberRepository.findByIdAndTeam_Id(memberId, teamId);
    }

    /** Members of all the given teams, users included, in one query. */
    public List<TeamMember> findMembersOfTeams(java.util.Collection<Long> teamIds) {
        return teamMemberRepository.findByTeamIdsWithUser(teamIds);
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
        TeamSubscription subscription = team.getSubscription();
        LocalDateTime now = planService.now();
        return new TeamResponse(team.getId(), team.getName(), membership.getRole(),
                countMembers(team.getId()), team.getCreatedAt(), team.isPersonal(), subscription.effectivePlan(now),
                subscription.effectiveSource(now), subscription.effectiveExpiresAt(now));
    }

    public TeamMemberResponse toMemberResponse(TeamMember member) {
        User user = member.getUser();
        return new TeamMemberResponse(member.getId(), user.getId(), user.getName(), user.getEmail(),
                member.getRole(), member.getJoinedAt());
    }
}
