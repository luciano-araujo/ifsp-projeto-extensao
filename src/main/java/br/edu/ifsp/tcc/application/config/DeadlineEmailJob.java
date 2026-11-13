package br.edu.ifsp.tcc.application.config;

import br.edu.ifsp.tcc.application.usecase.SendDeadlineDigestUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** RF09: triggers the daily deadline digest at 08:00 (America/Sao_Paulo by default; cron and zone are configurable). */
@Component
public class DeadlineEmailJob {

    private static final Logger log = LoggerFactory.getLogger(DeadlineEmailJob.class);

    private final SendDeadlineDigestUseCase sendDeadlineDigestUseCase;

    public DeadlineEmailJob(SendDeadlineDigestUseCase sendDeadlineDigestUseCase) {
        this.sendDeadlineDigestUseCase = sendDeadlineDigestUseCase;
    }

    @Scheduled(cron = "${app.alerts.digest-cron:0 0 8 * * *}", zone = "${app.alerts.timezone:America/Sao_Paulo}")
    public void run() {
        try {
            int sent = sendDeadlineDigestUseCase.execute();
            log.info("Deadline digest sent to {} users.", sent);
        } catch (RuntimeException e) {
            // A failure must not stop the scheduler; the next day runs again
            log.error("Deadline digest failed.", e);
        }
    }
}
