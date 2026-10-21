package br.edu.ifsp.tcc.application.service;

import br.edu.ifsp.tcc.application.dto.TeamResponses.TeamMemberResponse;
import br.edu.ifsp.tcc.application.dto.TeamResponses.TeamResponse;
import br.edu.ifsp.tcc.application.entity.*;
import br.edu.ifsp.tcc.application.repository.BoardColumnRepository;
import br.edu.ifsp.tcc.application.repository.TeamMemberRepository;
import br.edu.ifsp.tcc.application.repository.TeamRepository;
import br.edu.ifsp.tcc.application.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;

@Service
public class TeamService {

    /** Default production columns from the scope document; each team can rename, add or remove them. */
    public static final List<String> DEFAULT_COLUMNS = List.of("Pesquisa", "Roteiro", "Gravação", "Edição");

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final BoardColumnRepository boardColumnRepository;
    private final UserRepository userRepository;
    private final KanbanItemService kanbanItemService;

    public TeamService(TeamRepository teamRepository,
                       TeamMemberRepository teamMemberRepository,
                       BoardColumnRepository boardColumnRepository,
                       UserRepository userRepository,
                       KanbanItemService kanbanItemService) {
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.boardColumnRepository = boardColumnRepository;
        this.userRepository = userRepository;
        this.kanbanItemService = kanbanItemService;
    }

    // --- Access checks ---

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

    /**
     * Lists the user's teams. A user without teams gets a personal team, and any item created
     * before teams existed (or through the legacy /kanban route) is moved into the first team.
     */
    @Transactional
    public List<TeamResponse> listTeams(User user) {
        List<TeamMember> memberships = teamMemberRepository.findByUser_IdOrderByJoinedAtAsc(user.getId());
        if (memberships.isEmpty()) {
            createTeamWithAdmin(user, personalTeamName(user));
            memberships = teamMemberRepository.findByUser_IdOrderByJoinedAtAsc(user.getId());
        }
        adoptItemsWithoutTeam(user, memberships.get(0).getTeam());
        return memberships.stream().map(this::toTeamResponse).toList();
    }

    @Transactional
    public TeamResponse createTeam(User user, String name) {
        TeamMember admin = createTeamWithAdmin(user, name.trim());
        return toTeamResponse(admin);
    }

    /** First team of the user, used by the legacy /kanban routes that carry no team id. */
    @Transactional
    public Team defaultTeamFor(User user) {
        List<TeamMember> memberships = teamMemberRepository.findByUser_IdOrderByJoinedAtAsc(user.getId());
        if (memberships.isEmpty()) {
            return createTeamWithAdmin(user, personalTeamName(user)).getTeam();
        }
        return memberships.get(0).getTeam();
    }

    private TeamMember createTeamWithAdmin(User user, String name) {
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

    private void adoptItemsWithoutTeam(User user, Team team) {
        List<KanbanItem> orphans = kanbanItemService.findWithoutTeamByUserId(user.getId());
        if (orphans.isEmpty()) return;

        List<BoardColumn> columns = boardColumnRepository.findByTeam_IdOrderBySortOrderAscIdAsc(team.getId());
        for (KanbanItem item : orphans) {
            item.setTeam(team);
            if (item.getState() != KanbanItemState.IDEATION && item.getColumn() == null && !columns.isEmpty()) {
                boolean late = item.getState() == KanbanItemState.REVIEW || item.getState() == KanbanItemState.DONE;
                item.setColumn(late ? columns.get(columns.size() - 1) : columns.get(0));
            }
            kanbanItemService.save(item);
        }
    }

    private String personalTeamName(User user) {
        String firstName = user.getName() == null ? "" : user.getName().trim().split("\\s+")[0];
        return firstName.isEmpty() ? "Meu Estudio" : "Estúdio de " + firstName;
    }

    private TeamResponse toTeamResponse(TeamMember membership) {
        Team team = membership.getTeam();
        return new TeamResponse(team.getId(), team.getName(), membership.getRole(),
                teamMemberRepository.countByTeam_Id(team.getId()), team.getCreatedAt());
    }

    // --- Members ---

    public List<TeamMemberResponse> listMembers(Long teamId, Long userId) {
        requireMember(teamId, userId);
        return teamMemberRepository.findByTeam_IdOrderByJoinedAtAsc(teamId).stream()
                .map(this::toMemberResponse).toList();
    }

    @Transactional
    public TeamMemberResponse addMember(Long teamId, Long userId, String email, TeamRole role) {
        TeamMember admin = requireRole(teamId, userId, TeamRole.ADMIN);

        User newUser = userRepository.findByEmail(email.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Nenhum usuario cadastrado com este e-mail."));

        if (teamMemberRepository.findByTeam_IdAndUser_Id(teamId, newUser.getId()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este usuario ja faz parte da equipe.");
        }

        TeamMember member = new TeamMember();
        member.setTeam(admin.getTeam());
        member.setUser(newUser);
        member.setRole(role != null ? role : TeamRole.CREATOR);
        return toMemberResponse(teamMemberRepository.save(member));
    }

    @Transactional
    public TeamMemberResponse updateMemberRole(Long teamId, Long userId, Long memberId, TeamRole role) {
        requireRole(teamId, userId, TeamRole.ADMIN);
        TeamMember member = findMember(teamId, memberId);

        if (member.getRole() == TeamRole.ADMIN && role != TeamRole.ADMIN) {
            ensureAnotherAdmin(teamId);
        }
        member.setRole(role);
        return toMemberResponse(teamMemberRepository.save(member));
    }

    /** Admins remove anyone; any member can leave the team by removing themselves. */
    @Transactional
    public void removeMember(Long teamId, Long userId, Long memberId) {
        TeamMember requester = requireMember(teamId, userId);
        TeamMember member = findMember(teamId, memberId);

        boolean leaving = member.getId().equals(requester.getId());
        if (!leaving && requester.getRole() != TeamRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seu papel na equipe nao permite esta acao.");
        }
        if (member.getRole() == TeamRole.ADMIN) {
            ensureAnotherAdmin(teamId);
        }
        teamMemberRepository.delete(member);
    }

    private TeamMember findMember(Long teamId, Long memberId) {
        return teamMemberRepository.findByIdAndTeam_Id(memberId, teamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Membro nao encontrado."));
    }

    private void ensureAnotherAdmin(Long teamId) {
        if (teamMemberRepository.countByTeam_IdAndRole(teamId, TeamRole.ADMIN) <= 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A equipe precisa de pelo menos um administrador.");
        }
    }

    private TeamMemberResponse toMemberResponse(TeamMember member) {
        User user = member.getUser();
        return new TeamMemberResponse(member.getId(), user.getId(), user.getName(), user.getEmail(),
                member.getRole(), member.getJoinedAt());
    }
}
