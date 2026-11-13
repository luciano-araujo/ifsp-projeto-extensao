package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.service.MaterialService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class ArchiveMaterialsUseCase {

    private final MaterialService materialService;
    private final long afterDays;

    public ArchiveMaterialsUseCase(MaterialService materialService,
                                   @Value("${app.archive.after-days:30}") long afterDays) {
        this.materialService = materialService;
        this.afterDays = afterDays;
    }

    /**
     * RN04: archives (cheaper storage class, still downloadable) the materials linked to lessons of courses
     * published for at least the configured number of days. Returns how many were archived now.
     */
    @Transactional
    public int execute() {
        return materialService.archiveDueMaterials(LocalDateTime.now().minusDays(afterDays));
    }
}
