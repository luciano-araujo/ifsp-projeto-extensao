package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.service.EmailChangeTokenService;
import br.edu.ifsp.tcc.application.service.InviteService;
import br.edu.ifsp.tcc.application.service.PasswordResetTokenService;
import br.edu.ifsp.tcc.application.service.RegistrationTokenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PurgeExpiredCodesUseCaseTest {

    @Mock private RegistrationTokenService registrationTokenService;
    @Mock private PasswordResetTokenService passwordResetTokenService;
    @Mock private EmailChangeTokenService emailChangeTokenService;
    @Mock private InviteService inviteService;
    @InjectMocks private PurgeExpiredCodesUseCase useCase;

    @Test
    void execute_shouldDeleteExpiredCodesOfBothKindsWithTheSameCutoff() {
        when(registrationTokenService.deleteExpired(any(LocalDateTime.class))).thenReturn(3);
        when(passwordResetTokenService.deleteExpired(any(LocalDateTime.class))).thenReturn(2);
        when(emailChangeTokenService.deleteExpired(any(LocalDateTime.class))).thenReturn(1);
        when(inviteService.deleteExpired(any(LocalDateTime.class))).thenReturn(4);
        LocalDateTime before = LocalDateTime.now();

        int removed = useCase.execute();

        assertEquals(10, removed);
        ArgumentCaptor<LocalDateTime> registrationCutoff = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> resetCutoff = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(registrationTokenService).deleteExpired(registrationCutoff.capture());
        verify(passwordResetTokenService).deleteExpired(resetCutoff.capture());
        assertEquals(registrationCutoff.getValue(), resetCutoff.getValue());
        assertFalse(registrationCutoff.getValue().isBefore(before));
    }

    @Test
    void execute_shouldReturnZeroWhenNothingExpired() {
        when(registrationTokenService.deleteExpired(any(LocalDateTime.class))).thenReturn(0);
        when(passwordResetTokenService.deleteExpired(any(LocalDateTime.class))).thenReturn(0);
        when(emailChangeTokenService.deleteExpired(any(LocalDateTime.class))).thenReturn(0);
        when(inviteService.deleteExpired(any(LocalDateTime.class))).thenReturn(0);

        assertEquals(0, useCase.execute());
    }
}
