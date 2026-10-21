package br.edu.ifsp.tcc.application.config;

import br.edu.ifsp.tcc.application.usecase.PurgeExpiredCodesUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExpiredCodesCleanupJobTest {

    @Mock private PurgeExpiredCodesUseCase purgeExpiredCodesUseCase;
    @InjectMocks private ExpiredCodesCleanupJob job;

    @Test
    void run_shouldTriggerThePurgeUseCase() {
        when(purgeExpiredCodesUseCase.execute()).thenReturn(4);

        job.run();

        verify(purgeExpiredCodesUseCase).execute();
    }
}
