package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.MaterialResponse;
import br.edu.ifsp.tcc.application.service.BoardService;
import br.edu.ifsp.tcc.application.service.MaterialService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class ListCardMaterialsUseCase {

    private final TeamService teamService;
    private final BoardService boardService;
    private final MaterialService materialService;

    public ListCardMaterialsUseCase(TeamService teamService, BoardService boardService, MaterialService materialService) {
        this.teamService = teamService;
        this.boardService = boardService;
        this.materialService = materialService;
    }

    /** Materials of the card, newest first. Any member can list them. */
    @Transactional(readOnly = true)
    public List<MaterialResponse> execute(Long teamId, Long userId, Long cardId) {
        teamService.requireMember(teamId, userId);
        boardService.findCard(teamId, cardId);
        return materialService.findByCard(cardId).stream()
                .map(material -> materialService.toResponse(material, teamId))
                .toList();
    }
}
