package br.edu.ifsp.tcc.application.controller;

import br.edu.ifsp.tcc.application.dto.*;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.usecase.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teams/{teamId}")
public class BoardController {

    private final GetBoardUseCase getBoardUseCase;
    private final ListIdeasUseCase listIdeasUseCase;
    private final CreateIdeaUseCase createIdeaUseCase;
    private final CreateCardUseCase createCardUseCase;
    private final ConvertIdeaUseCase convertIdeaUseCase;
    private final MoveCardUseCase moveCardUseCase;
    private final DeleteCardUseCase deleteCardUseCase;
    private final CreateColumnUseCase createColumnUseCase;
    private final UpdateColumnUseCase updateColumnUseCase;
    private final DeleteColumnUseCase deleteColumnUseCase;
    private final CreateCardTaskUseCase createCardTaskUseCase;
    private final ToggleCardTaskUseCase toggleCardTaskUseCase;
    private final DeleteCardTaskUseCase deleteCardTaskUseCase;
    private final ToggleIdeaVoteUseCase toggleIdeaVoteUseCase;
    private final UpdateCardTaskUseCase updateCardTaskUseCase;
    private final ReorderCardTasksUseCase reorderCardTasksUseCase;
    private final ReorderColumnsUseCase reorderColumnsUseCase;
    private final GetCardHistoryUseCase getCardHistoryUseCase;
    private final CreateCardReviewUseCase createCardReviewUseCase;
    private final ListCardReviewsUseCase listCardReviewsUseCase;
    private final GenerateCardSummaryUseCase generateCardSummaryUseCase;
    private final GetCardSummaryUseCase getCardSummaryUseCase;

    public BoardController(GetBoardUseCase getBoardUseCase,
                           ListIdeasUseCase listIdeasUseCase,
                           CreateIdeaUseCase createIdeaUseCase,
                           CreateCardUseCase createCardUseCase,
                           ConvertIdeaUseCase convertIdeaUseCase,
                           MoveCardUseCase moveCardUseCase,
                           DeleteCardUseCase deleteCardUseCase,
                           CreateColumnUseCase createColumnUseCase,
                           UpdateColumnUseCase updateColumnUseCase,
                           DeleteColumnUseCase deleteColumnUseCase,
                           CreateCardTaskUseCase createCardTaskUseCase,
                           ToggleCardTaskUseCase toggleCardTaskUseCase,
                           DeleteCardTaskUseCase deleteCardTaskUseCase,
                           ToggleIdeaVoteUseCase toggleIdeaVoteUseCase,
                           UpdateCardTaskUseCase updateCardTaskUseCase,
                           ReorderCardTasksUseCase reorderCardTasksUseCase,
                           ReorderColumnsUseCase reorderColumnsUseCase,
                           GetCardHistoryUseCase getCardHistoryUseCase,
                           CreateCardReviewUseCase createCardReviewUseCase,
                           ListCardReviewsUseCase listCardReviewsUseCase,
                           GenerateCardSummaryUseCase generateCardSummaryUseCase,
                           GetCardSummaryUseCase getCardSummaryUseCase) {
        this.getBoardUseCase = getBoardUseCase;
        this.listIdeasUseCase = listIdeasUseCase;
        this.createIdeaUseCase = createIdeaUseCase;
        this.createCardUseCase = createCardUseCase;
        this.convertIdeaUseCase = convertIdeaUseCase;
        this.moveCardUseCase = moveCardUseCase;
        this.deleteCardUseCase = deleteCardUseCase;
        this.createColumnUseCase = createColumnUseCase;
        this.updateColumnUseCase = updateColumnUseCase;
        this.deleteColumnUseCase = deleteColumnUseCase;
        this.createCardTaskUseCase = createCardTaskUseCase;
        this.toggleCardTaskUseCase = toggleCardTaskUseCase;
        this.deleteCardTaskUseCase = deleteCardTaskUseCase;
        this.toggleIdeaVoteUseCase = toggleIdeaVoteUseCase;
        this.updateCardTaskUseCase = updateCardTaskUseCase;
        this.reorderCardTasksUseCase = reorderCardTasksUseCase;
        this.reorderColumnsUseCase = reorderColumnsUseCase;
        this.getCardHistoryUseCase = getCardHistoryUseCase;
        this.createCardReviewUseCase = createCardReviewUseCase;
        this.listCardReviewsUseCase = listCardReviewsUseCase;
        this.generateCardSummaryUseCase = generateCardSummaryUseCase;
        this.getCardSummaryUseCase = getCardSummaryUseCase;
    }

    @GetMapping("/board")
    public ResponseEntity<BoardResponse> board(@AuthenticationPrincipal User user, @PathVariable Long teamId) {
        return ResponseEntity.ok(getBoardUseCase.execute(teamId, user.getId()));
    }

    // --- Ideas inbox ---

    @GetMapping("/ideas")
    public ResponseEntity<List<CardResponse>> ideas(@AuthenticationPrincipal User user,
                                                    @PathVariable Long teamId,
                                                    @RequestParam(required = false) String q,
                                                    @RequestParam(required = false) Long tagId,
                                                    @RequestParam(required = false) String sort) {
        return ResponseEntity.ok(listIdeasUseCase.execute(teamId, user.getId(), q, tagId, sort));
    }

    @PostMapping("/ideas")
    public ResponseEntity<CardResponse> createIdea(@AuthenticationPrincipal User user,
                                                   @PathVariable Long teamId,
                                                   @Valid @RequestBody CreateCardDTO dto) {
        return ResponseEntity.ok(createIdeaUseCase.execute(teamId, user, dto));
    }

    @PostMapping("/cards/{cardId}/vote")
    public ResponseEntity<IdeaVoteResponse> vote(@AuthenticationPrincipal User user,
                                                 @PathVariable Long teamId,
                                                 @PathVariable Long cardId) {
        return ResponseEntity.ok(toggleIdeaVoteUseCase.execute(teamId, user, cardId));
    }

    // --- Cards ---

    @PostMapping("/cards")
    public ResponseEntity<CardResponse> createCard(@AuthenticationPrincipal User user,
                                                   @PathVariable Long teamId,
                                                   @Valid @RequestBody CreateCardDTO dto) {
        return ResponseEntity.ok(createCardUseCase.execute(teamId, user, dto));
    }

    @PostMapping("/cards/{cardId}/convert")
    public ResponseEntity<CardResponse> convert(@AuthenticationPrincipal User user,
                                                @PathVariable Long teamId,
                                                @PathVariable Long cardId,
                                                @RequestParam(required = false) Long targetColumnId,
                                                @Valid @RequestBody(required = false) ConvertCardDTO dto) {
        return ResponseEntity.ok(convertIdeaUseCase.execute(teamId, user.getId(), cardId, targetColumnId, dto));
    }

    @PatchMapping("/cards/{cardId}/move")
    public ResponseEntity<CardResponse> move(@AuthenticationPrincipal User user,
                                             @PathVariable Long teamId,
                                             @PathVariable Long cardId,
                                             @Valid @RequestBody MoveCardDTO dto) {
        return ResponseEntity.ok(moveCardUseCase.execute(teamId, user.getId(), cardId, dto));
    }

    @DeleteMapping("/cards/{cardId}")
    public ResponseEntity<Void> deleteCard(@AuthenticationPrincipal User user,
                                           @PathVariable Long teamId,
                                           @PathVariable Long cardId) {
        deleteCardUseCase.execute(teamId, user.getId(), cardId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/cards/{cardId}/history")
    public ResponseEntity<List<CardMovementResponse>> history(@AuthenticationPrincipal User user,
                                                              @PathVariable Long teamId,
                                                              @PathVariable Long cardId) {
        return ResponseEntity.ok(getCardHistoryUseCase.execute(teamId, user.getId(), cardId));
    }

    // --- Reviews and summary ---

    @PostMapping("/cards/{cardId}/reviews")
    public ResponseEntity<CardReviewResponse> createReview(@AuthenticationPrincipal User user,
                                                           @PathVariable Long teamId,
                                                           @PathVariable Long cardId,
                                                           @Valid @RequestBody CreateReviewDTO dto) {
        return ResponseEntity.ok(createCardReviewUseCase.execute(teamId, user.getId(), cardId, dto));
    }

    @GetMapping("/cards/{cardId}/reviews")
    public ResponseEntity<List<CardReviewResponse>> reviews(@AuthenticationPrincipal User user,
                                                            @PathVariable Long teamId,
                                                            @PathVariable Long cardId) {
        return ResponseEntity.ok(listCardReviewsUseCase.execute(teamId, user.getId(), cardId));
    }

    @PostMapping("/cards/{cardId}/summary")
    public ResponseEntity<CardSummaryResponse> generateSummary(@AuthenticationPrincipal User user,
                                                               @PathVariable Long teamId,
                                                               @PathVariable Long cardId) {
        return ResponseEntity.ok(generateCardSummaryUseCase.execute(teamId, user.getId(), cardId));
    }

    @GetMapping("/cards/{cardId}/summary")
    public ResponseEntity<CardSummaryResponse> summary(@AuthenticationPrincipal User user,
                                                       @PathVariable Long teamId,
                                                       @PathVariable Long cardId) {
        return ResponseEntity.ok(getCardSummaryUseCase.execute(teamId, user.getId(), cardId));
    }

    // --- Columns ---

    @PutMapping("/columns/order")
    public ResponseEntity<List<BoardColumnResponse>> reorderColumns(@AuthenticationPrincipal User user,
                                                                    @PathVariable Long teamId,
                                                                    @Valid @RequestBody ReorderColumnsDTO dto) {
        return ResponseEntity.ok(reorderColumnsUseCase.execute(teamId, user.getId(), dto));
    }

    @PostMapping("/columns")
    public ResponseEntity<BoardColumnResponse> addColumn(@AuthenticationPrincipal User user,
                                                         @PathVariable Long teamId,
                                                         @Valid @RequestBody BoardColumnDTO dto) {
        return ResponseEntity.ok(createColumnUseCase.execute(teamId, user.getId(), dto));
    }

    @PatchMapping("/columns/{columnId}")
    public ResponseEntity<BoardColumnResponse> updateColumn(@AuthenticationPrincipal User user,
                                                            @PathVariable Long teamId,
                                                            @PathVariable Long columnId,
                                                            @Valid @RequestBody BoardColumnDTO dto) {
        return ResponseEntity.ok(updateColumnUseCase.execute(teamId, user.getId(), columnId, dto));
    }

    @DeleteMapping("/columns/{columnId}")
    public ResponseEntity<Void> deleteColumn(@AuthenticationPrincipal User user,
                                             @PathVariable Long teamId,
                                             @PathVariable Long columnId,
                                             @RequestParam(required = false) Long targetColumnId) {
        deleteColumnUseCase.execute(teamId, user.getId(), columnId, targetColumnId);
        return ResponseEntity.noContent().build();
    }

    // --- Card tasks ---

    @PostMapping("/cards/{cardId}/tasks")
    public ResponseEntity<CardTaskResponse> createTask(@AuthenticationPrincipal User user,
                                                       @PathVariable Long teamId,
                                                       @PathVariable Long cardId,
                                                       @Valid @RequestBody CreateTaskDTO dto) {
        return ResponseEntity.ok(createCardTaskUseCase.execute(teamId, user.getId(), cardId, dto));
    }

    @PatchMapping("/cards/{cardId}/tasks/{taskId}")
    public ResponseEntity<CardTaskResponse> updateTask(@AuthenticationPrincipal User user,
                                                       @PathVariable Long teamId,
                                                       @PathVariable Long cardId,
                                                       @PathVariable Long taskId,
                                                       @Valid @RequestBody UpdateTaskDTO dto) {
        return ResponseEntity.ok(updateCardTaskUseCase.execute(teamId, user.getId(), cardId, taskId, dto));
    }

    @PutMapping("/cards/{cardId}/tasks/order")
    public ResponseEntity<List<CardTaskResponse>> reorderTasks(@AuthenticationPrincipal User user,
                                                               @PathVariable Long teamId,
                                                               @PathVariable Long cardId,
                                                               @Valid @RequestBody ReorderTasksDTO dto) {
        return ResponseEntity.ok(reorderCardTasksUseCase.execute(teamId, user.getId(), cardId, dto));
    }

    @PatchMapping("/cards/{cardId}/tasks/{taskId}/toggle")
    public ResponseEntity<CardTaskResponse> toggleTask(@AuthenticationPrincipal User user,
                                                       @PathVariable Long teamId,
                                                       @PathVariable Long cardId,
                                                       @PathVariable Long taskId) {
        return ResponseEntity.ok(toggleCardTaskUseCase.execute(teamId, user.getId(), cardId, taskId));
    }

    @DeleteMapping("/cards/{cardId}/tasks/{taskId}")
    public ResponseEntity<Void> deleteTask(@AuthenticationPrincipal User user,
                                           @PathVariable Long teamId,
                                           @PathVariable Long cardId,
                                           @PathVariable Long taskId) {
        deleteCardTaskUseCase.execute(teamId, user.getId(), cardId, taskId);
        return ResponseEntity.noContent().build();
    }
}
