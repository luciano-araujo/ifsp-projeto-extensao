package br.edu.ifsp.tcc.application.config;

import br.edu.ifsp.tcc.application.usecase.ArchiveMaterialsUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArchiveMaterialsJobTest {

    @Mock private ArchiveMaterialsUseCase archiveMaterialsUseCase;
    @InjectMocks private ArchiveMaterialsJob job;

    @Test
    void run_shouldTriggerTheUseCase() {
        when(archiveMaterialsUseCase.execute()).thenReturn(2);

        job.run();

        verify(archiveMaterialsUseCase).execute();
    }

    @Test
    void run_shouldWorkWhenNothingWasArchived() {
        when(archiveMaterialsUseCase.execute()).thenReturn(0);

        job.run();

        verify(archiveMaterialsUseCase).execute();
    }
}
