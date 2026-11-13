package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.service.MaterialService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArchiveMaterialsUseCaseTest {

    @Mock private MaterialService materialService;

    @Test
    void execute_shouldUseTheLimitOfThirtyDaysAgo() {
        when(materialService.archiveDueMaterials(any(LocalDateTime.class))).thenReturn(3);
        LocalDateTime before = LocalDateTime.now().minusDays(30);

        int archived = new ArchiveMaterialsUseCase(materialService, 30).execute();

        assertEquals(3, archived);
        ArgumentCaptor<LocalDateTime> limit = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(materialService).archiveDueMaterials(limit.capture());
        assertFalse(limit.getValue().isBefore(before));
        assertFalse(limit.getValue().isAfter(LocalDateTime.now().minusDays(30)));
    }

    @Test
    void execute_shouldUseTheConfiguredNumberOfDays() {
        new ArchiveMaterialsUseCase(materialService, 7).execute();

        ArgumentCaptor<LocalDateTime> limit = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(materialService).archiveDueMaterials(limit.capture());
        assertTrue(limit.getValue().isAfter(LocalDateTime.now().minusDays(8)));
    }
}
