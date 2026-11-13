package br.edu.ifsp.tcc.application.config;

import br.edu.ifsp.tcc.application.usecase.SendDeadlineDigestUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Scheduled;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeadlineEmailJobTest {

    @Mock private SendDeadlineDigestUseCase sendDeadlineDigestUseCase;
    @InjectMocks private DeadlineEmailJob job;

    @Test
    void run_shouldTriggerTheDigestUseCase() {
        when(sendDeadlineDigestUseCase.execute()).thenReturn(2);

        job.run();

        verify(sendDeadlineDigestUseCase).execute();
    }

    @Test
    void run_shouldSwallowFailuresSoTheSchedulerKeepsRunning() {
        when(sendDeadlineDigestUseCase.execute()).thenThrow(new IllegalStateException("boom"));

        assertDoesNotThrow(job::run);
    }

    @Test
    void run_shouldBeScheduledAt8InSaoPauloByDefault() throws NoSuchMethodException {
        Scheduled scheduled = DeadlineEmailJob.class.getMethod("run").getAnnotation(Scheduled.class);

        assertTrue(scheduled.cron().contains("0 0 8 * * *"));
        assertTrue(scheduled.zone().contains("America/Sao_Paulo"));
    }
}
