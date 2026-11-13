package br.edu.ifsp.tcc.application.service;

import br.edu.ifsp.tcc.application.dto.AdminTeamResponse;
import br.edu.ifsp.tcc.application.dto.PlanResponse;
import br.edu.ifsp.tcc.application.dto.PlanUsageResponse;
import br.edu.ifsp.tcc.application.dto.ReviewStatus;
import br.edu.ifsp.tcc.application.dto.TeamPlanResponse;
import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.KanbanItemState;
import br.edu.ifsp.tcc.application.entity.PlanSource;
import br.edu.ifsp.tcc.application.entity.PlanType;
import br.edu.ifsp.tcc.application.entity.Team;
import br.edu.ifsp.tcc.application.entity.TeamMember;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.repository.CourseRepository;
import br.edu.ifsp.tcc.application.repository.KanbanItemRepository;
import br.edu.ifsp.tcc.application.repository.MaterialRepository;
import br.edu.ifsp.tcc.application.repository.TeamMemberRepository;
import br.edu.ifsp.tcc.application.repository.TeamRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlanServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 13, 12, 0);
    private static final long MB = 1024L * 1024;

    @Mock private TeamRepository teamRepository;
    @Mock private TeamMemberRepository teamMemberRepository;
    @Mock private KanbanItemRepository kanbanItemRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private MaterialRepository materialRepository;
    @Mock private CardReviewService cardReviewService;

    private PlanService service;
    private final br.edu.ifsp.tcc.application.entity.User owner = new br.edu.ifsp.tcc.application.entity.User();

    @BeforeEach
    void setUp() {
        owner.setName("Ana Clara");
        Clock clock = Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
        service = new PlanService(teamRepository, teamMemberRepository, kanbanItemRepository, courseRepository,
                materialRepository, cardReviewService, clock);
    }

    private Team team(PlanType plan) {
        Team team = new Team();
        team.setId(10L);
        team.setName("Equipe");
        team.getSubscription().setPlan(plan);
        if (plan != PlanType.GRATUITO) team.getSubscription().setSource(PlanSource.MANUAL);
        return team;
    }

    private void assert402(Runnable action, String... messageParts) {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, action::run);
        assertEquals(HttpStatus.PAYMENT_REQUIRED, ex.getStatusCode());
        for (String part : messageParts) assertTrue(ex.getReason().contains(part), ex.getReason());
    }

    private KanbanItem project(long id) {
        KanbanItem item = new KanbanItem();
        item.setId(id);
        item.setState(KanbanItemState.IN_PRODUCTION);
        return item;
    }

    private void activeProjects(int total, int approved) {
        List<KanbanItem> items = new java.util.ArrayList<>();
        Map<Long, ReviewStatus> statuses = new java.util.HashMap<>();
        for (int i = 1; i <= total; i++) {
            items.add(project(i));
            if (i <= approved) statuses.put((long) i, ReviewStatus.APPROVED);
            else if (i % 2 == 0) statuses.put((long) i, ReviewStatus.CHANGES_REQUESTED);
        }
        when(kanbanItemRepository.findByTeam_IdAndState(10L, KanbanItemState.IN_PRODUCTION)).thenReturn(items);
        when(cardReviewService.findStatuses(items)).thenReturn(statuses);
    }

    // --- Plan in force / trial computed on read ---

    @Test
    void effectivePlan_shouldBeGratuitoByDefaultAndAfterTheTrialEnds() {
        Team team = team(PlanType.GRATUITO);
        assertEquals(PlanType.GRATUITO, service.effectivePlan(team));

        team.getSubscription().setPlan(PlanType.PRO);
        team.getSubscription().setSource(PlanSource.TRIAL);
        team.getSubscription().setExpiresAt(NOW.plusNanos(1));
        assertEquals(PlanType.PRO, service.effectivePlan(team));

        team.getSubscription().setExpiresAt(NOW);
        assertEquals(PlanType.GRATUITO, service.effectivePlan(team));
    }

    @Test
    void startTrial_shouldGiveFourteenDaysOnceAndRecordTheUse() {
        Team team = team(PlanType.GRATUITO);
        when(teamRepository.save(team)).thenReturn(team);

        service.startTrial(team, PlanType.AGENCIA);

        assertEquals(PlanType.AGENCIA, team.getSubscription().getPlan());
        assertEquals(PlanSource.TRIAL, team.getSubscription().getSource());
        assertEquals(NOW.plusDays(14), team.getSubscription().getExpiresAt());
        assertEquals(NOW, team.getSubscription().getTrialUsedAt());
    }

    @Test
    void startTrial_shouldNotAllowASecondTrialEvenAfterTheFirstExpired() {
        Team team = team(PlanType.GRATUITO);
        team.getSubscription().setPlan(PlanType.PRO);
        team.getSubscription().setSource(PlanSource.TRIAL);
        team.getSubscription().setExpiresAt(NOW.minusDays(1));
        team.getSubscription().setTrialUsedAt(NOW.minusDays(15));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.startTrial(team, PlanType.SOLO));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(teamRepository, never()).save(any());
    }

    @Test
    void startTrial_shouldRefuseTeamsOnAPaidPlanAndTheFreePlanAsTarget() {
        Team paid = team(PlanType.SOLO);
        ResponseStatusException conflict = assertThrows(ResponseStatusException.class, () -> service.startTrial(paid, PlanType.PRO));
        assertEquals(HttpStatus.CONFLICT, conflict.getStatusCode());

        Team free = team(PlanType.GRATUITO);
        ResponseStatusException bad = assertThrows(ResponseStatusException.class, () -> service.startTrial(free, PlanType.GRATUITO));
        assertEquals(HttpStatus.BAD_REQUEST, bad.getStatusCode());
        assertThrows(ResponseStatusException.class, () -> service.startTrial(free, null));
    }

    @Test
    void activate_shouldSetAManualPlanWithOptionalEndDate() {
        Team team = team(PlanType.GRATUITO);
        when(teamRepository.save(team)).thenReturn(team);

        service.activate(team, PlanType.PRO, NOW.plusDays(30));

        assertEquals(PlanType.PRO, team.getSubscription().getPlan());
        assertEquals(PlanSource.MANUAL, team.getSubscription().getSource());
        assertEquals(NOW.plusDays(30), team.getSubscription().getExpiresAt());

        service.activate(team, PlanType.SOLO, null);
        assertEquals(PlanType.SOLO, team.getSubscription().getPlan());
        assertNull(team.getSubscription().getExpiresAt());
    }

    @Test
    void activate_shouldResetToGratuitoAndRejectAPastEndDate() {
        Team team = team(PlanType.PRO);
        team.getSubscription().setExpiresAt(NOW.plusDays(3));
        when(teamRepository.save(team)).thenReturn(team);

        service.activate(team, PlanType.GRATUITO, NOW.plusDays(9));
        assertEquals(PlanType.GRATUITO, team.getSubscription().getPlan());
        assertEquals(PlanSource.FREE, team.getSubscription().getSource());
        assertNull(team.getSubscription().getExpiresAt());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> service.activate(team, PlanType.PRO, NOW));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        ResponseStatusException far = assertThrows(ResponseStatusException.class,
                () -> service.activate(team, PlanType.PRO, LocalDateTime.of(2038, 1, 1, 0, 0)));
        assertEquals(HttpStatus.BAD_REQUEST, far.getStatusCode());
    }

    // --- Members ---

    @Test
    void assertCanAddMember_shouldBlockGratuitoNamingTheUnlockingPlan() {
        Team team = team(PlanType.GRATUITO);
        when(teamMemberRepository.countByTeam_Id(10L)).thenReturn(1);

        assert402(() -> service.assertCanAddMember(team), "plano Gratuito", "1 usuário", "plano Agência");
    }

    @Test
    void assertCanAddMember_shouldAllowAgenciaUpToFiveAndBlockTheSixth() {
        Team team = team(PlanType.AGENCIA);
        when(teamMemberRepository.countByTeam_Id(10L)).thenReturn(4, 5);

        service.assertCanAddMember(team);
        assert402(() -> service.assertCanAddMember(team), "5 usuários");
    }

    @Test
    void assertCanAddMember_shouldBlockATeamThatIsAlreadyOverTheLimit() {
        Team team = team(PlanType.SOLO);
        when(teamMemberRepository.countByTeam_Id(10L)).thenReturn(3);

        assert402(() -> service.assertCanAddMember(team), "plano Solo", "plano Agência");
    }

    @Test
    void assertCanAddMember_shouldBlockWhenTheTrialHasExpired() {
        Team team = team(PlanType.AGENCIA);
        team.getSubscription().setSource(PlanSource.TRIAL);
        team.getSubscription().setExpiresAt(NOW.minusSeconds(1));
        when(teamMemberRepository.countByTeam_Id(10L)).thenReturn(2);

        assert402(() -> service.assertCanAddMember(team), "plano Gratuito");
    }

    // --- Active projects ---

    @Test
    void countActiveProjects_shouldIgnoreApprovedProjects() {
        activeProjects(5, 2);

        assertEquals(3, service.countActiveProjects(10L));
    }

    @Test
    void assertCanCreateActiveProject_shouldBlockTheFourthOnGratuito() {
        activeProjects(3, 0);

        assert402(() -> service.assertCanCreateActiveProject(team(PlanType.GRATUITO)),
                "até 3 projetos ativos", "plano Solo");
    }

    @Test
    void assertCanCreateActiveProject_shouldAllowWhenApprovedProjectsFreeASlot() {
        activeProjects(3, 1);

        service.assertCanCreateActiveProject(team(PlanType.GRATUITO));
    }

    @Test
    void assertCanCreateActiveProject_shouldBlockSoloAtTenAndNeverBlockUnlimitedPlans() {
        activeProjects(10, 0);
        assert402(() -> service.assertCanCreateActiveProject(team(PlanType.SOLO)), "até 10 projetos ativos", "plano Pro");

        service.assertCanCreateActiveProject(team(PlanType.PRO));
        service.assertCanCreateActiveProject(team(PlanType.AGENCIA));
    }

    // --- Courses ---

    @Test
    void assertCanCreateCourse_shouldBlockTheSecondOnGratuitoAndTheSixthOnSolo() {
        when(courseRepository.countByTeam_Id(10L)).thenReturn(1, 5);

        assert402(() -> service.assertCanCreateCourse(team(PlanType.GRATUITO)), "até 1 curso.", "plano Solo");
        assert402(() -> service.assertCanCreateCourse(team(PlanType.SOLO)), "até 5 cursos.", "plano Pro");
    }

    @Test
    void assertCanCreateCourse_shouldAllowBelowTheLimitAndUnlimitedPlans() {
        when(courseRepository.countByTeam_Id(10L)).thenReturn(0);

        service.assertCanCreateCourse(team(PlanType.GRATUITO));
        service.assertCanCreateCourse(team(PlanType.PRO));
        verify(courseRepository, times(1)).countByTeam_Id(10L);
    }

    // --- Summary ---

    @Test
    void assertCanGenerateSummary_shouldBlockGratuitoAndSoloOnly() {
        assert402(() -> service.assertCanGenerateSummary(team(PlanType.GRATUITO)), "plano Gratuito", "plano Pro");
        assert402(() -> service.assertCanGenerateSummary(team(PlanType.SOLO)), "plano Solo", "plano Pro");
        service.assertCanGenerateSummary(team(PlanType.PRO));
        service.assertCanGenerateSummary(team(PlanType.AGENCIA));
    }

    // --- Storage ---

    @Test
    void assertCanStoreMaterial_shouldAllowUpToExactly100MbOnGratuito() {
        when(materialRepository.sumSizeBytesByTeam(10L)).thenReturn(80 * MB);

        service.assertCanStoreMaterial(team(PlanType.GRATUITO), 20 * MB);
    }

    @Test
    void assertCanStoreMaterial_shouldBlockOneByteOver100MbNamingTheLimitAndPlan() {
        when(materialRepository.sumSizeBytesByTeam(10L)).thenReturn(80 * MB);

        assert402(() -> service.assertCanStoreMaterial(team(PlanType.GRATUITO), 20 * MB + 1),
                "plano Gratuito", "100 MB", "plano Solo");
    }

    @Test
    void assertCanStoreMaterial_shouldSuggestThePlanThatFitsTheTotal() {
        when(materialRepository.sumSizeBytesByTeam(10L)).thenReturn(1020L * MB);

        assert402(() -> service.assertCanStoreMaterial(team(PlanType.SOLO), 10 * MB), "1 GB", "plano Pro");
        service.assertCanStoreMaterial(team(PlanType.PRO), 10 * MB);
    }

    // --- One free team per admin ---

    private TeamMember membership(Team team, TeamRole role) {
        TeamMember member = new TeamMember();
        member.setUser(owner);
        member.setTeam(team);
        member.setRole(role);
        return member;
    }

    @Test
    void assertCanCreateFreeTeam_shouldBlockWhenTheUserAdministersAGratuitoTeam() {
        when(teamMemberRepository.findByUser_IdOrderByJoinedAtAsc(1L))
                .thenReturn(List.of(membership(team(PlanType.GRATUITO), TeamRole.ADMIN)));

        assert402(() -> service.assertCanCreateFreeTeam(1L), "apenas uma equipe compartilhada no plano Gratuito");
    }

    @Test
    void assertCanCreateFreeTeam_shouldNotCountThePersonalTeam() {
        Team personal = team(PlanType.GRATUITO);
        personal.setName("Estúdio de Ana");
        Team shared = team(PlanType.GRATUITO);
        when(teamMemberRepository.findByUser_IdOrderByJoinedAtAsc(1L))
                .thenReturn(List.of(membership(personal, TeamRole.ADMIN)));
        when(teamMemberRepository.findByUser_IdOrderByJoinedAtAsc(2L))
                .thenReturn(List.of(membership(personal, TeamRole.ADMIN), membership(shared, TeamRole.ADMIN)));

        service.assertCanCreateFreeTeam(1L);                                   // personal team + one new: allowed
        assert402(() -> service.assertCanCreateFreeTeam(2L), "equipe compartilhada"); // second shared free team
    }

    @Test
    void assertCanCreateFreeTeam_shouldIgnorePaidTeamsAndTeamsWhereTheUserIsNotAdmin() {
        Team expiredTrial = team(PlanType.PRO);
        expiredTrial.getSubscription().setSource(PlanSource.TRIAL);
        expiredTrial.getSubscription().setExpiresAt(NOW.minusDays(1));
        when(teamMemberRepository.findByUser_IdOrderByJoinedAtAsc(1L)).thenReturn(List.of(
                membership(team(PlanType.PRO), TeamRole.ADMIN),
                membership(team(PlanType.GRATUITO), TeamRole.CREATOR)));
        when(teamMemberRepository.findByUser_IdOrderByJoinedAtAsc(2L))
                .thenReturn(List.of(membership(expiredTrial, TeamRole.ADMIN)));

        service.assertCanCreateFreeTeam(1L);
        assert402(() -> service.assertCanCreateFreeTeam(2L), "equipe compartilhada"); // trial over = free again
    }

    @Test
    void assertCanCreateFreeTeam_shouldAllowUsersWithoutTeams() {
        when(teamMemberRepository.findByUser_IdOrderByJoinedAtAsc(3L)).thenReturn(List.of());

        service.assertCanCreateFreeTeam(3L);
    }

    // --- Lookups and mapping ---

    @Test
    void findTeam_shouldReturnOrFailWith404() {
        Team team = team(PlanType.GRATUITO);
        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));
        when(teamRepository.findById(99L)).thenReturn(Optional.empty());

        assertSame(team, service.findTeam(10L));
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.findTeam(99L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void listPlans_shouldListTheFourPlansWithTheirLimits() {
        List<PlanResponse> plans = service.listPlans();

        assertEquals(List.of(PlanType.GRATUITO, PlanType.SOLO, PlanType.PRO, PlanType.AGENCIA),
                plans.stream().map(PlanResponse::getPlan).toList());
        assertEquals("Agência", plans.get(3).getName());
        assertEquals(5, plans.get(3).getLimits().getMaxUsers());
        assertNull(plans.get(2).getLimits().getMaxCourses());
        assertTrue(plans.get(2).getLimits().getAutomaticSummary());
    }

    @Test
    void toTeamPlanResponse_shouldShowPlanLimitsUsageAndTrialAvailability() {
        Team team = team(PlanType.GRATUITO);
        activeProjects(2, 0);
        when(teamMemberRepository.countByTeam_Id(10L)).thenReturn(1);
        when(courseRepository.countByTeam_Id(10L)).thenReturn(1);
        when(materialRepository.sumSizeBytesByTeam(10L)).thenReturn(5 * MB);

        TeamPlanResponse response = service.toTeamPlanResponse(team);

        assertEquals(PlanType.GRATUITO, response.getPlan());
        assertEquals("Gratuito", response.getPlanName());
        assertEquals(PlanSource.FREE, response.getSource());
        assertTrue(response.getTrialAvailable());
        assertNull(response.getTrialUsedAt());
        assertEquals(3, response.getLimits().getMaxActiveProjects());
        PlanUsageResponse usage = response.getUsage();
        assertEquals(1, usage.getMembers());
        assertEquals(2, usage.getActiveProjects());
        assertEquals(1, usage.getCourses());
        assertEquals(5 * MB, usage.getStorageBytes());
        assertFalse(response.getOverLimit());
    }

    @Test
    void toTeamPlanResponse_shouldFlagOverLimitAfterTheTrialEndedAndShowTrialUsed() {
        Team team = team(PlanType.PRO);
        team.getSubscription().setSource(PlanSource.TRIAL);
        team.getSubscription().setExpiresAt(NOW.minusDays(1));
        team.getSubscription().setTrialUsedAt(NOW.minusDays(15));
        activeProjects(5, 0);
        when(teamMemberRepository.countByTeam_Id(10L)).thenReturn(1);
        when(courseRepository.countByTeam_Id(10L)).thenReturn(0);
        when(materialRepository.sumSizeBytesByTeam(10L)).thenReturn(0L);

        TeamPlanResponse response = service.toTeamPlanResponse(team);

        assertEquals(PlanType.GRATUITO, response.getPlan());
        assertNull(response.getExpiresAt());
        assertFalse(response.getTrialAvailable());
        assertEquals(NOW.minusDays(15), response.getTrialUsedAt());
        assertTrue(response.getOverLimit());
    }

    @Test
    void toTeamPlanResponse_shouldDetectEachKindOfOverLimit() {
        Team team = team(PlanType.GRATUITO);
        activeProjects(0, 0);
        when(teamMemberRepository.countByTeam_Id(10L)).thenReturn(2, 1, 1);
        when(courseRepository.countByTeam_Id(10L)).thenReturn(0, 2, 0);
        when(materialRepository.sumSizeBytesByTeam(10L)).thenReturn(0L, 0L, 101 * MB);

        assertTrue(service.toTeamPlanResponse(team).getOverLimit());   // members
        assertTrue(service.toTeamPlanResponse(team).getOverLimit());   // courses
        assertTrue(service.toTeamPlanResponse(team).getOverLimit());   // storage
    }

    @Test
    void toAdminTeamResponse_shouldShowThePlanInForce() {
        Team team = team(PlanType.PRO);
        team.getSubscription().setExpiresAt(NOW.plusDays(2));
        when(teamMemberRepository.countByTeam_Id(10L)).thenReturn(1);

        AdminTeamResponse response = service.toAdminTeamResponse(team);

        assertEquals(10L, response.getId());
        assertEquals(PlanType.PRO, response.getPlan());
        assertEquals(PlanSource.MANUAL, response.getPlanSource());
        assertEquals(NOW.plusDays(2), response.getPlanExpiresAt());
        assertEquals(1, response.getMembersCount());
    }

    @Test
    void findTeamsAdministeredBy_shouldReturnOnlyAdminTeams() {
        Team mine = team(PlanType.GRATUITO);
        Team other = team(PlanType.PRO);
        when(teamMemberRepository.findByUser_IdOrderByJoinedAtAsc(1L))
                .thenReturn(List.of(membership(mine, TeamRole.ADMIN), membership(other, TeamRole.REVIEWER)));

        assertEquals(List.of(mine), service.findTeamsAdministeredBy(1L));
    }

    @Test
    void now_shouldFollowTheClockAndFormatBytesShouldUseMbAndGb() {
        assertEquals(NOW, service.now());
        assertEquals("100 MB", PlanService.formatBytes(100 * MB));
        assertEquals("5 GB", PlanService.formatBytes(5L * 1024 * MB));
        assertNotNull(Instant.now());
    }
}
