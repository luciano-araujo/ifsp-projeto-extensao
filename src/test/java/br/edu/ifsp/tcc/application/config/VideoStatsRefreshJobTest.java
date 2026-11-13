package br.edu.ifsp.tcc.application.config;

import br.edu.ifsp.tcc.application.usecase.RefreshAllVideoStatsUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VideoStatsRefreshJobTest {

    @Mock private RefreshAllVideoStatsUseCase refreshAllVideoStatsUseCase;
    @InjectMocks private VideoStatsRefreshJob job;

    @Test
    void run_shouldTriggerTheUseCase() {
        when(refreshAllVideoStatsUseCase.execute()).thenReturn(3);

        job.run();

        verify(refreshAllVideoStatsUseCase).execute();
    }

    @Test
    void run_shouldWorkWhenNothingWasSaved() {
        when(refreshAllVideoStatsUseCase.execute()).thenReturn(0);

        job.run();

        verify(refreshAllVideoStatsUseCase).execute();
    }

    @Test
    void run_shouldNeverLetAnExceptionEscapeToTheScheduler() {
        when(refreshAllVideoStatsUseCase.execute()).thenThrow(new IllegalStateException("boom"));

        assertDoesNotThrow(job::run);
    }
}
