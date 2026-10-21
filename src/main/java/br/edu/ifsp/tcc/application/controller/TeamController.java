package br.edu.ifsp.tcc.application.controller;

import br.edu.ifsp.tcc.application.dto.AddTeamMemberDTO;
import br.edu.ifsp.tcc.application.dto.CreateTeamDTO;
import br.edu.ifsp.tcc.application.dto.TeamResponses.TeamMemberResponse;
import br.edu.ifsp.tcc.application.dto.TeamResponses.TeamResponse;
import br.edu.ifsp.tcc.application.dto.UpdateTeamMemberDTO;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.service.TeamService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teams")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @GetMapping
    public ResponseEntity<List<TeamResponse>> list(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(teamService.listTeams(user));
    }

    @PostMapping
    public ResponseEntity<TeamResponse> create(@AuthenticationPrincipal User user,
                                               @Valid @RequestBody CreateTeamDTO dto) {
        return ResponseEntity.ok(teamService.createTeam(user, dto.getName()));
    }

    // --- Members ---

    @GetMapping("/{teamId}/members")
    public ResponseEntity<List<TeamMemberResponse>> listMembers(@AuthenticationPrincipal User user,
                                                                @PathVariable Long teamId) {
        return ResponseEntity.ok(teamService.listMembers(teamId, user.getId()));
    }

    @PostMapping("/{teamId}/members")
    public ResponseEntity<TeamMemberResponse> addMember(@AuthenticationPrincipal User user,
                                                        @PathVariable Long teamId,
                                                        @Valid @RequestBody AddTeamMemberDTO dto) {
        return ResponseEntity.ok(teamService.addMember(teamId, user.getId(), dto.getEmail(), dto.getRole()));
    }

    @PatchMapping("/{teamId}/members/{memberId}")
    public ResponseEntity<TeamMemberResponse> updateMember(@AuthenticationPrincipal User user,
                                                           @PathVariable Long teamId,
                                                           @PathVariable Long memberId,
                                                           @Valid @RequestBody UpdateTeamMemberDTO dto) {
        return ResponseEntity.ok(teamService.updateMemberRole(teamId, user.getId(), memberId, dto.getRole()));
    }

    @DeleteMapping("/{teamId}/members/{memberId}")
    public ResponseEntity<Void> removeMember(@AuthenticationPrincipal User user,
                                             @PathVariable Long teamId,
                                             @PathVariable Long memberId) {
        teamService.removeMember(teamId, user.getId(), memberId);
        return ResponseEntity.noContent().build();
    }
}
