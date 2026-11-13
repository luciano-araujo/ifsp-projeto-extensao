package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.MaterialDownload;
import br.edu.ifsp.tcc.application.service.MaterialService;
import br.edu.ifsp.tcc.application.service.TeamService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DownloadMaterialUseCase {

    private final TeamService teamService;
    private final MaterialService materialService;

    public DownloadMaterialUseCase(TeamService teamService, MaterialService materialService) {
        this.teamService = teamService;
        this.materialService = materialService;
    }

    /** The file to stream: as an attachment by default, shown in the browser when {@code inline} is true. Any member. */
    @Transactional(readOnly = true)
    public MaterialDownload execute(Long teamId, Long userId, Long materialId, boolean inline) {
        teamService.requireMember(teamId, userId);
        return materialService.open(materialService.findMaterial(teamId, materialId), inline);
    }
}
