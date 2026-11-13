package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.MaterialResponse;
import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.KanbanItemState;
import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.service.BoardService;
import br.edu.ifsp.tcc.application.service.MaterialService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Component
public class UploadMaterialUseCase {

    private final TeamService teamService;
    private final BoardService boardService;
    private final MaterialService materialService;

    public UploadMaterialUseCase(TeamService teamService, BoardService boardService, MaterialService materialService) {
        this.teamService = teamService;
        this.boardService = boardService;
        this.materialService = materialService;
    }

    /** Attaches a file to a project card. Editors only; ideas cannot have materials. */
    @Transactional
    public MaterialResponse execute(Long teamId, Long userId, Long cardId, MultipartFile file) {
        User user = teamService.requireRole(teamId, userId, TeamRole.ADMIN, TeamRole.CREATOR).getUser();
        KanbanItem card = boardService.findCard(teamId, cardId);
        if (card.getState() == KanbanItemState.IDEATION) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Converta a ideia em projeto antes de anexar materiais.");
        }
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Envie um arquivo não vazio no campo \"file\".");
        }
        return materialService.toResponse(materialService.attach(card, user, file), teamId);
    }
}
