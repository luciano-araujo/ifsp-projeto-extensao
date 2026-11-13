package br.edu.ifsp.tcc.application.service;

import br.edu.ifsp.tcc.application.entity.PasswordResetToken;
import br.edu.ifsp.tcc.application.repository.PasswordResetTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetTokenServiceTest {

    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @InjectMocks private PasswordResetTokenService service;

    @Test
    void save_shouldDelegateToRepository() {
        PasswordResetToken token = new PasswordResetToken();
        when(passwordResetTokenRepository.save(token)).thenReturn(token);

        assertSame(token, service.save(token));
    }

    @Test
    void findLatestByEmail_shouldReturnLatestToken() {
        PasswordResetToken token = new PasswordResetToken();
        when(passwordResetTokenRepository.findFirstByEmailOrderByCreatedAtDesc("ana@teste.com"))
                .thenReturn(Optional.of(token));

        assertEquals(Optional.of(token), service.findLatestByEmail("ana@teste.com"));
    }

    @Test
    void deleteByEmail_shouldDelegateToRepository() {
        service.deleteByEmail("ana@teste.com");

        verify(passwordResetTokenRepository).deleteByEmail("ana@teste.com");
    }

    @Test
    void deleteExpired_shouldReturnRemovedCount() {
        LocalDateTime now = LocalDateTime.now();
        when(passwordResetTokenRepository.deleteExpired(now)).thenReturn(2);

        assertEquals(2, service.deleteExpired(now));
    }

    @Test
    void registerWrongAttempt_shouldBeTrueWhileTheCounterIsBelowTheLimitAndFalseAfterwards() {
        when(passwordResetTokenRepository.incrementAttempts(7L, 4)).thenReturn(1, 0);

        assertTrue(service.registerWrongAttempt(7L, 4));
        assertFalse(service.registerWrongAttempt(7L, 4));
    }
}
