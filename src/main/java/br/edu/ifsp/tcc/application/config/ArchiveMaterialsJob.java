package br.edu.ifsp.tcc.application.config;

import br.edu.ifsp.tcc.application.usecase.ArchiveMaterialsUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Daily trigger of the archive of materials of courses published for 30+ days (RN04). */
@Component
public class ArchiveMaterialsJob {

    private static final Logger log = LoggerFactory.getLogger(ArchiveMaterialsJob.class);

    private final ArchiveMaterialsUseCase archiveMaterialsUseCase;

    public ArchiveMaterialsJob(ArchiveMaterialsUseCase archiveMaterialsUseCase) {
        this.archiveMaterialsUseCase = archiveMaterialsUseCase;
    }

    @Scheduled(cron = "${app.archive.cron:0 0 3 * * *}", zone = "${app.archive.zone:America/Sao_Paulo}")
    public void run() {
        int archived = archiveMaterialsUseCase.execute();
        if (archived > 0) {
            log.info("Archived {} materials of published courses.", archived);
        }
    }
}
