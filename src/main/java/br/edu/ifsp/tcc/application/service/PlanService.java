package br.edu.ifsp.tcc.application.service;

import br.edu.ifsp.tcc.application.dto.AdminTeamResponse;
import br.edu.ifsp.tcc.application.dto.PlanLimitsResponse;
import br.edu.ifsp.tcc.application.dto.PlanResponse;
import br.edu.ifsp.tcc.application.dto.PlanUsageResponse;
import br.edu.ifsp.tcc.application.dto.ReviewStatus;
import br.edu.ifsp.tcc.application.dto.TeamPlanResponse;
import br.edu.ifsp.tcc.application.entity.InviteStatus;
import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.KanbanItemState;
import br.edu.ifsp.tcc.application.entity.PlanSource;
import br.edu.ifsp.tcc.application.entity.PlanType;
import br.edu.ifsp.tcc.application.entity.Team;
import br.edu.ifsp.tcc.application.entity.TeamMember;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.entity.TeamSubscription;
import br.edu.ifsp.tcc.application.repository.CourseRepository;
import br.edu.ifsp.tcc.application.repository.KanbanItemRepository;
import br.edu.ifsp.tcc.application.repository.MaterialRepository;
import br.edu.ifsp.tcc.application.repository.TeamInviteRepository;
import br.edu.ifsp.tcc.application.repository.TeamMemberRepository;
import br.edu.ifsp.tcc.application.repository.TeamRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Plans of the teams: the plan in force (a trial expires on read, no job), the usage, the limit checks
 * (HTTP 402 naming the plan that lifts the limit) and the changes of plan. Over a limit only new creation is
 * blocked; nothing is ever deleted. Role checks live in the use cases.
 */
@Service
public class PlanService {

    public static final int TRIAL_DAYS = 14;

    /** The database column is a TIMESTAMP, which ends in January 2038. */
    static final LocalDateTime MAX_EXPIRY = LocalDateTime.of(2037, 12, 31, 23, 59, 59);

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final KanbanItemRepository kanbanItemRepository;
    private final CourseRepository courseRepository;
    private final MaterialRepository materialRepository;
    private final CardReviewService cardReviewService;
    private final TeamInviteRepository teamInviteRepository;
    private final Clock clock;

    public PlanService(TeamRepository teamRepository,
                       TeamMemberRepository teamMemberRepository,
                       KanbanItemRepository kanbanItemRepository,
                       CourseRepository courseRepository,
                       MaterialRepository materialRepository,
                       CardReviewService cardReviewService,
                       TeamInviteRepository teamInviteRepository,
                       Clock clock) {
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.kanbanItemRepository = kanbanItemRepository;
        this.courseRepository = courseRepository;
        this.materialRepository = materialRepository;
        this.cardReviewService = cardReviewService;
        this.teamInviteRepository = teamInviteRepository;
        this.clock = clock;
    }

    // --- Lookups ---

    public Team findTeam(Long teamId) {
        return teamRepository.findById(teamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Equipe não encontrada."));
    }

    /** The plan in force now (Gratuito once a trial or an activation has expired). */
    public PlanType effectivePlan(Team team) {
        return team.getSubscription().effectivePlan(now());
    }

    // --- Usage ---

    public PlanUsageResponse usage(Team team) {
        return new PlanUsageResponse(teamMemberRepository.countByTeam_Id(team.getId()),
                countActiveProjects(team.getId()), courseRepository.countByTeam_Id(team.getId()),
                materialRepository.sumSizeBytesByTeam(team.getId()));
    }

    /** Active project = IN_PRODUCTION and not approved by its latest review. */
    public int countActiveProjects(Long teamId) {
        List<KanbanItem> projects = kanbanItemRepository.findByTeam_IdAndState(teamId, KanbanItemState.IN_PRODUCTION);
        Map<Long, ReviewStatus> statuses = cardReviewService.findStatuses(projects);
        return (int) projects.stream().filter(p -> statuses.get(p.getId()) != ReviewStatus.APPROVED).count();
    }

    // --- Limit checks (402) ---

    /** Call wherever a member is added (direct add, invite creation or acceptance). */
    public void requireCanAddMember(Team team) {
        PlanType plan = effectivePlan(team);
        int members = teamMemberRepository.countByTeam_Id(team.getId());
        if (plan.getMaxUsers() != null && members >= plan.getMaxUsers()) {
            throw limitReached("O plano " + plan.getDisplayName() + " permite " + plan.getMaxUsers()
                            + (plan.getMaxUsers() == 1 ? " usuário" : " usuários") + " por equipe.",
                    "adicionar membros", p -> p.getMaxUsers() == null || p.getMaxUsers() > members);
        }
    }

    /**
     * Creating an invite: members plus open (pending, not expired) invites must stay under the plan's user limit,
     * so a team cannot collect more invitations than it has seats. Accepting rechecks members only.
     */
    public void requireCanInvite(Team team) {
        PlanType plan = effectivePlan(team);
        if (plan.getMaxUsers() == null) return;
        int taken = teamMemberRepository.countByTeam_Id(team.getId())
                + teamInviteRepository.countByTeam_IdAndStatusAndExpiresAtAfter(team.getId(), InviteStatus.PENDING, now());
        if (taken >= plan.getMaxUsers()) {
            throw limitReached("O plano " + plan.getDisplayName() + " permite " + plan.getMaxUsers()
                            + (plan.getMaxUsers() == 1 ? " usuário" : " usuários") + " por equipe (membros e convites pendentes).",
                    "convidar mais pessoas", pl -> pl.getMaxUsers() == null || pl.getMaxUsers() > taken);
        }
    }

    public void requireCanCreateProject(Team team) {
        PlanType plan = effectivePlan(team);
        if (plan.getMaxActiveProjects() == null) return;
        int active = countActiveProjects(team.getId());
        if (active >= plan.getMaxActiveProjects()) {
            throw limitReached("O plano " + plan.getDisplayName() + " permite até " + plan.getMaxActiveProjects()
                            + " projetos ativos.", "criar mais projetos",
                    p -> p.getMaxActiveProjects() == null || p.getMaxActiveProjects() > active);
        }
    }

    public void requireCanCreateCourse(Team team) {
        PlanType plan = effectivePlan(team);
        if (plan.getMaxCourses() == null) return;
        int courses = courseRepository.countByTeam_Id(team.getId());
        if (courses >= plan.getMaxCourses()) {
            throw limitReached("O plano " + plan.getDisplayName() + " permite até " + plan.getMaxCourses()
                            + (plan.getMaxCourses() == 1 ? " curso." : " cursos."), "criar mais cursos",
                    p -> p.getMaxCourses() == null || p.getMaxCourses() > courses);
        }
    }

    public void requireCanSummarize(Team team) {
        PlanType plan = effectivePlan(team);
        if (!plan.isAutomaticSummary()) {
            throw limitReached("O resumo automático não está disponível no plano " + plan.getDisplayName() + ".",
                    "gerar resumos", PlanType::isAutomaticSummary);
        }
    }

    /** The total size of the team's materials plus the new file must fit in the plan storage. */
    public void requireStorage(Team team, long newFileBytes) {
        PlanType plan = effectivePlan(team);
        long needed = materialRepository.sumSizeBytesByTeam(team.getId()) + newFileBytes;
        if (needed > plan.getStorageBytes()) {
            throw limitReached("O armazenamento de materiais do plano " + plan.getDisplayName() + " ("
                            + formatBytes(plan.getStorageBytes()) + ") seria excedido por este arquivo.",
                    "enviar mais arquivos", p -> p.getStorageBytes() >= needed);
        }
    }

    /**
     * One shared team on Gratuito per person. The personal team (auto-created, flagged by Team.isPersonal)
     * does not count. A team counts as free unless a system administrator activated its plan (MANUAL and
     * not expired), so a trial still counts and trial-then-create cannot be chained.
     */
    public void requireCanCreateFreeTeam(Long userId) {
        boolean hasFreeTeam = teamMemberRepository.findByUser_IdOrderByJoinedAtAsc(userId).stream()
                .filter(m -> m.getRole() == TeamRole.ADMIN)
                .filter(m -> !m.getTeam().isPersonal())
                .anyMatch(m -> m.getTeam().getSubscription().effectiveSource(now()) != PlanSource.MANUAL);
        if (hasFreeTeam) {
            throw new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED,
                    "Cada pessoa pode administrar apenas uma equipe compartilhada no plano Gratuito. "
                            + "Para criar outra equipe, mude a sua equipe atual para um plano pago.");
        }
    }

    // --- Changes of plan ---

    /** Starts the 14-day trial; once per team, and only while the team is on Gratuito. */
    public Team startTrial(Team team, PlanType plan) {
        if (plan == null || plan == PlanType.GRATUITO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escolha Solo, Pro ou Agência para o teste grátis.");
        }
        TeamSubscription subscription = team.getSubscription();
        if (subscription.getTrialUsedAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O teste grátis já foi usado por esta equipe.");
        }
        if (effectivePlan(team) != PlanType.GRATUITO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A equipe já possui um plano pago ativo.");
        }
        LocalDateTime now = now();
        subscription.setPlan(plan);
        subscription.setSource(PlanSource.TRIAL);
        subscription.setExpiresAt(now.plusDays(TRIAL_DAYS));
        subscription.setTrialUsedAt(now);
        return teamRepository.save(team);
    }

    /** Manual activation by the system administrator. GRATUITO resets the team to the free plan. */
    public Team activate(Team team, PlanType plan, LocalDateTime expiresAt) {
        TeamSubscription subscription = team.getSubscription();
        if (plan == PlanType.GRATUITO) {
            subscription.setPlan(PlanType.GRATUITO);
            subscription.setSource(PlanSource.FREE);
            subscription.setExpiresAt(null);
        } else {
            if (expiresAt != null && !expiresAt.isAfter(now())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A data de término deve estar no futuro.");
            }
            if (expiresAt != null && expiresAt.isAfter(MAX_EXPIRY)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A data de término deve ser anterior a 2038.");
            }
            subscription.setPlan(plan);
            subscription.setSource(PlanSource.MANUAL);
            subscription.setExpiresAt(expiresAt);
        }
        return teamRepository.save(team);
    }

    // --- Response mapping ---

    public List<PlanResponse> listPlans() {
        return Arrays.stream(PlanType.values())
                .map(p -> new PlanResponse(p, p.getDisplayName(), p.getMonthlyPrice(), limitsOf(p))).toList();
    }

    public TeamPlanResponse toPlanResponse(Team team) {
        LocalDateTime now = now();
        TeamSubscription subscription = team.getSubscription();
        PlanType plan = subscription.effectivePlan(now);
        PlanUsageResponse usage = usage(team);
        boolean over = (plan.getMaxUsers() != null && usage.getMembers() > plan.getMaxUsers())
                || (plan.getMaxActiveProjects() != null && usage.getActiveProjects() > plan.getMaxActiveProjects())
                || (plan.getMaxCourses() != null && usage.getCourses() > plan.getMaxCourses())
                || usage.getStorageBytes() > plan.getStorageBytes();
        return new TeamPlanResponse(team.getId(), plan, plan.getDisplayName(), subscription.effectiveSource(now),
                subscription.effectiveExpiresAt(now), subscription.getTrialUsedAt() == null,
                subscription.getTrialUsedAt(), limitsOf(plan), usage, over);
    }

    public AdminTeamResponse toAdminTeamResponse(Team team) {
        LocalDateTime now = now();
        TeamSubscription subscription = team.getSubscription();
        return new AdminTeamResponse(team.getId(), team.getName(), subscription.effectivePlan(now),
                subscription.effectiveSource(now), subscription.effectiveExpiresAt(now),
                teamMemberRepository.countByTeam_Id(team.getId()), team.getCreatedAt());
    }

    /** Teams the user administers, for the system administrator's lookup. */
    public List<Team> findTeamsAdministeredBy(Long userId) {
        return teamMemberRepository.findByUser_IdOrderByJoinedAtAsc(userId).stream()
                .filter(m -> m.getRole() == TeamRole.ADMIN).map(TeamMember::getTeam).toList();
    }

    public static PlanLimitsResponse limitsOf(PlanType plan) {
        return new PlanLimitsResponse(plan.getMaxUsers(), plan.getMaxActiveProjects(), plan.getMaxCourses(),
                plan.isAutomaticSummary(), plan.getStorageBytes());
    }

    // --- Helpers ---

    /** The current moment of the application clock. */
    public LocalDateTime now() {
        return LocalDateTime.now(clock).truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
    }

    /** 402 whose message names the current limit and the cheapest plan that lifts it. */
    private static ResponseStatusException limitReached(String limitText, String action, Predicate<PlanType> lifts) {
        String message = limitText;
        PlanType unlocking = Arrays.stream(PlanType.values()).filter(lifts).findFirst().orElse(null);
        if (unlocking != null) message += " Para " + action + ", mude para o plano " + unlocking.getDisplayName() + ".";
        return new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, message);
    }

    static String formatBytes(long bytes) {
        long gb = 1024L * 1024 * 1024;
        return bytes >= gb ? (bytes / gb) + " GB" : (bytes / (1024L * 1024)) + " MB";
    }
}
