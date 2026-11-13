package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.entity.TeamRole;
import br.edu.ifsp.tcc.application.service.MaterialService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DeleteMaterialUseCase {

    private final TeamService teamService;
    private final MaterialService materialService;

    public DeleteMaterialUseCase(TeamService teamService, MaterialService materialService) {
        this.teamService = teamService;
        this.materialService = materialService;
    }

    /** Editors only. The row goes first; the stored file is removed after the commit. */
    @Transactional
    public void execute(Long teamId, Long userId, Long materialId) {
        teamService.requireRole(teamId, userId, TeamRole.ADMIN, TeamRole.CREATOR);
        materialService.delete(materialService.findMaterial(teamId, materialId));
    }
}
