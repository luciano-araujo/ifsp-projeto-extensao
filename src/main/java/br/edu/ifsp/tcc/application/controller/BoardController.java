package br.edu.ifsp.tcc.application.controller;

import br.edu.ifsp.tcc.application.dto.*;
import br.edu.ifsp.tcc.application.dto.TeamResponses.BoardColumnResponse;
import br.edu.ifsp.tcc.application.dto.TeamResponses.BoardResponse;
import br.edu.ifsp.tcc.application.dto.TeamResponses.CardResponse;
import br.edu.ifsp.tcc.application.dto.TeamResponses.CardTaskResponse;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.service.BoardService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/teams/{teamId}")
public class BoardController {

    private final BoardService boardService;

    public BoardController(BoardService boardService) {
        this.boardService = boardService;
    }

    @GetMapping("/board")
    public ResponseEntity<BoardResponse> board(@AuthenticationPrincipal User user, @PathVariable Long teamId) {
        return ResponseEntity.ok(boardService.getBoard(teamId, user.getId()));
    }

    // --- Ideas inbox ---

    @GetMapping("/ideas")
    public ResponseEntity<List<CardResponse>> ideas(@AuthenticationPrincipal User user, @PathVariable Long teamId) {
        return ResponseEntity.ok(boardService.listIdeas(teamId, user.getId()));
    }

    @PostMapping("/ideas")
    public ResponseEntity<CardResponse> createIdea(@AuthenticationPrincipal User user,
                                                   @PathVariable Long teamId,
                                                   @Valid @RequestBody CreateCardDTO dto) {
        return ResponseEntity.ok(boardService.createIdea(teamId, user, dto));
    }

    // --- Cards ---

    @PostMapping("/cards")
    public ResponseEntity<CardResponse> createCard(@AuthenticationPrincipal User user,
                                                   @PathVariable Long teamId,
                                                   @Valid @RequestBody CreateCardDTO dto) {
        return ResponseEntity.ok(boardService.createCard(teamId, user, dto));
    }

    @PostMapping("/cards/{cardId}/convert")
    public ResponseEntity<CardResponse> convert(@AuthenticationPrincipal User user,
                                                @PathVariable Long teamId,
                                                @PathVariable Long cardId,
                                                @RequestParam(required = false) Long targetColumnId,
                                                @RequestBody(required = false) ConvertCardDTO dto) {
        return ResponseEntity.ok(boardService.convertIdea(teamId, user.getId(), cardId, targetColumnId, dto));
    }

    @PatchMapping("/cards/{cardId}/move")
    public ResponseEntity<CardResponse> move(@AuthenticationPrincipal User user,
                                             @PathVariable Long teamId,
                                             @PathVariable Long cardId,
                                             @Valid @RequestBody MoveCardDTO dto) {
        return ResponseEntity.ok(boardService.moveCard(teamId, user.getId(), cardId, dto.getColumnId(), dto.getSortOrder()));
    }

    @DeleteMapping("/cards/{cardId}")
    public ResponseEntity<Void> deleteCard(@AuthenticationPrincipal User user,
                                           @PathVariable Long teamId,
                                           @PathVariable Long cardId) {
        boardService.deleteCard(teamId, user.getId(), cardId);
        return ResponseEntity.noContent().build();
    }

    // --- Columns ---

    @PostMapping("/columns")
    public ResponseEntity<BoardColumnResponse> addColumn(@AuthenticationPrincipal User user,
                                                         @PathVariable Long teamId,
                                                         @Valid @RequestBody BoardColumnDTO dto) {
        return ResponseEntity.ok(boardService.addColumn(teamId, user.getId(), dto));
    }

    @PatchMapping("/columns/{columnId}")
    public ResponseEntity<BoardColumnResponse> updateColumn(@AuthenticationPrincipal User user,
                                                            @PathVariable Long teamId,
                                                            @PathVariable Long columnId,
                                                            @Valid @RequestBody BoardColumnDTO dto) {
        return ResponseEntity.ok(boardService.updateColumn(teamId, user.getId(), columnId, dto));
    }

    @DeleteMapping("/columns/{columnId}")
    public ResponseEntity<Void> deleteColumn(@AuthenticationPrincipal User user,
                                             @PathVariable Long teamId,
                                             @PathVariable Long columnId,
                                             @RequestParam(required = false) Long targetColumnId) {
        boardService.deleteColumn(teamId, user.getId(), columnId, targetColumnId);
        return ResponseEntity.noContent().build();
    }

    // --- Card tasks ---

    @PostMapping("/cards/{cardId}/tasks")
    public ResponseEntity<CardTaskResponse> createTask(@AuthenticationPrincipal User user,
                                                       @PathVariable Long teamId,
                                                       @PathVariable Long cardId,
                                                       @Valid @RequestBody CreateTaskDTO dto) {
        return ResponseEntity.ok(boardService.createTask(teamId, user.getId(), cardId, dto.getTitle()));
    }

    @PatchMapping("/cards/{cardId}/tasks/{taskId}/toggle")
    public ResponseEntity<CardTaskResponse> toggleTask(@AuthenticationPrincipal User user,
                                                       @PathVariable Long teamId,
                                                       @PathVariable Long cardId,
                                                       @PathVariable Long taskId) {
        return ResponseEntity.ok(boardService.toggleTask(teamId, user.getId(), cardId, taskId));
    }

    @DeleteMapping("/cards/{cardId}/tasks/{taskId}")
    public ResponseEntity<Void> deleteTask(@AuthenticationPrincipal User user,
                                           @PathVariable Long teamId,
                                           @PathVariable Long cardId,
                                           @PathVariable Long taskId) {
        boardService.deleteTask(teamId, user.getId(), cardId, taskId);
        return ResponseEntity.noContent().build();
    }
}
