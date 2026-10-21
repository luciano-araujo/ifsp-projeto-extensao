package br.edu.ifsp.tcc.application.config;

import br.edu.ifsp.tcc.application.usecase.PurgeExpiredCodesUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Periodically triggers the removal of expired sign-up and password-reset codes. */
@Component
public class ExpiredCodesCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(ExpiredCodesCleanupJob.class);

    private final PurgeExpiredCodesUseCase purgeExpiredCodesUseCase;

    public ExpiredCodesCleanupJob(PurgeExpiredCodesUseCase purgeExpiredCodesUseCase) {
        this.purgeExpiredCodesUseCase = purgeExpiredCodesUseCase;
    }

    @Scheduled(initialDelayString = "${app.codes.cleanup-initial-delay-ms}",
               fixedDelayString = "${app.codes.cleanup-interval-ms}")
    public void run() {
        int removed = purgeExpiredCodesUseCase.execute();
        if (removed > 0) {
            log.info("Removed {} expired verification codes.", removed);
        }
    }
}
