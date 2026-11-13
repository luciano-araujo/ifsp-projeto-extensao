package br.edu.ifsp.tcc.application.service;

import br.edu.ifsp.tcc.application.entity.RegistrationToken;
import br.edu.ifsp.tcc.application.repository.RegistrationTokenRepository;
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
class RegistrationTokenServiceTest {

    @Mock private RegistrationTokenRepository registrationTokenRepository;
    @InjectMocks private RegistrationTokenService service;

    @Test
    void save_shouldDelegateToRepository() {
        RegistrationToken token = new RegistrationToken();
        when(registrationTokenRepository.save(token)).thenReturn(token);

        assertSame(token, service.save(token));
    }

    @Test
    void findLatestByEmail_shouldReturnLatestToken() {
        RegistrationToken token = new RegistrationToken();
        when(registrationTokenRepository.findFirstByEmailOrderByCreatedAtDesc("ana@teste.com"))
                .thenReturn(Optional.of(token));

        assertEquals(Optional.of(token), service.findLatestByEmail("ana@teste.com"));
    }

    @Test
    void deleteByEmail_shouldDelegateToRepository() {
        service.deleteByEmail("ana@teste.com");

        verify(registrationTokenRepository).deleteByEmail("ana@teste.com");
    }

    @Test
    void delete_shouldDelegateToRepository() {
        RegistrationToken token = new RegistrationToken();

        service.delete(token);

        verify(registrationTokenRepository).delete(token);
    }

    @Test
    void deleteExpired_shouldReturnRemovedCount() {
        LocalDateTime now = LocalDateTime.now();
        when(registrationTokenRepository.deleteExpired(now)).thenReturn(3);

        assertEquals(3, service.deleteExpired(now));
    }

    @Test
    void registerWrongAttempt_shouldBeTrueWhileTheCounterIsBelowTheLimitAndFalseAfterwards() {
        when(registrationTokenRepository.incrementAttempts(7L, 4)).thenReturn(1, 0);

        assertTrue(service.registerWrongAttempt(7L, 4));
        assertFalse(service.registerWrongAttempt(7L, 4));
    }
}
