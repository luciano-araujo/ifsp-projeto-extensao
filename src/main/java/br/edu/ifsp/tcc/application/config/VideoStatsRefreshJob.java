package br.edu.ifsp.tcc.application.config;

import br.edu.ifsp.tcc.application.usecase.RefreshAllVideoStatsUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Daily trigger of the refresh of the YouTube statistics of every card with a video link ("-" disables it). */
@Component
public class VideoStatsRefreshJob {

    private static final Logger log = LoggerFactory.getLogger(VideoStatsRefreshJob.class);

    private final RefreshAllVideoStatsUseCase refreshAllVideoStatsUseCase;

    public VideoStatsRefreshJob(RefreshAllVideoStatsUseCase refreshAllVideoStatsUseCase) {
        this.refreshAllVideoStatsUseCase = refreshAllVideoStatsUseCase;
    }

    @Scheduled(cron = "${app.youtube.refresh-cron:0 0 6 * * *}", zone = "${app.youtube.zone:America/Sao_Paulo}")
    public void run() {
        try {
            int saved = refreshAllVideoStatsUseCase.execute();
            if (saved > 0) {
                log.info("Saved {} YouTube statistics snapshots.", saved);
            }
        } catch (RuntimeException e) {
            log.warn("YouTube statistics refresh failed: {}", e.getClass().getSimpleName());
        }
    }
}
