package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.MessageResponse;
import br.edu.ifsp.tcc.application.dto.RequestEmailChangeDTO;
import br.edu.ifsp.tcc.application.entity.EmailChangeToken;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.service.EmailChangeTokenService;
import br.edu.ifsp.tcc.application.service.EmailService;
import br.edu.ifsp.tcc.application.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestEmailChangeUseCaseTest {

    @Mock private UserService userService;
    @Mock private EmailChangeTokenService emailChangeTokenService;
    @Mock private EmailService emailService;
    @InjectMocks private RequestEmailChangeUseCase useCase;

    private User ana;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(useCase, "tokenExpiryMinutes", 10);
        ana = new User();
        ana.setId(1L);
        ana.setName("Ana");
        ana.setEmail("ana@exemplo.test");
    }

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private RequestEmailChangeDTO dto(String email) {
        RequestEmailChangeDTO dto = new RequestEmailChangeDTO();
        dto.setNewEmail(email);
        return dto;
    }

    private ResponseStatusException thrown(Long id, Long caller, String email) {
        return assertThrows(ResponseStatusException.class, () -> useCase.execute(id, caller, dto(email)));
    }

    @Test
    void execute_shouldStoreCodeAndSendItToTheNewAddress() {
        when(userService.findById(1L)).thenReturn(Optional.of(ana));
        when(userService.findByEmail("novo@exemplo.test")).thenReturn(Optional.empty());

        MessageResponse response = useCase.execute(1L, 1L, dto(" novo@exemplo.test "));

        assertEquals("Enviamos um código de verificação para novo@exemplo.test.", response.getMessage());
        verify(emailChangeTokenService).deleteByUserId(1L);
        ArgumentCaptor<EmailChangeToken> saved = ArgumentCaptor.forClass(EmailChangeToken.class);
        verify(emailChangeTokenService).save(saved.capture());
        assertEquals("novo@exemplo.test", saved.getValue().getNewEmail());
        assertTrue(saved.getValue().getToken().matches("\\d{6}"));
        assertTrue(saved.getValue().getExpiresAt().isAfter(LocalDateTime.now().plusMinutes(9)));
        assertEquals(0, saved.getValue().getExpiresAt().getNano(), "stored without fractions of a second");
        verify(emailService).sendEmailChangeToken("novo@exemplo.test", "Ana", saved.getValue().getToken());
    }

    @Test
    void execute_shouldSendTheEmailOnlyAfterTheTransactionCommits() {
        when(userService.findById(1L)).thenReturn(Optional.of(ana));
        when(userService.findByEmail("novo@exemplo.test")).thenReturn(Optional.empty());
        TransactionSynchronizationManager.initSynchronization();

        useCase.execute(1L, 1L, dto("novo@exemplo.test"));

        verifyNoInteractions(emailService);
        List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
        assertEquals(1, synchronizations.size());
        synchronizations.get(0).afterCommit();
        verify(emailService).sendEmailChangeToken(eq("novo@exemplo.test"), eq("Ana"), anyString());
    }

    @Test
    void execute_shouldConflictWhenEmailBelongsToSomeone() {
        when(userService.findById(1L)).thenReturn(Optional.of(ana));
        when(userService.findByEmail("bia@exemplo.test")).thenReturn(Optional.of(new User()));

        assertEquals(HttpStatus.CONFLICT, thrown(1L, 1L, "bia@exemplo.test").getStatusCode());
        verifyNoInteractions(emailService, emailChangeTokenService);
    }

    @Test
    void execute_shouldConflictWhenEmailIsTheCurrentOne() {
        when(userService.findById(1L)).thenReturn(Optional.of(ana));

        assertEquals(HttpStatus.CONFLICT, thrown(1L, 1L, "ANA@exemplo.test").getStatusCode());
    }

    @Test
    void execute_shouldForbidOtherUsers() {
        assertEquals(HttpStatus.FORBIDDEN, thrown(1L, 2L, "x@exemplo.test").getStatusCode());
        verifyNoInteractions(userService);
    }

    @Test
    void execute_shouldReturn404ForUnknownUser() {
        when(userService.findById(1L)).thenReturn(Optional.empty());

        assertEquals(HttpStatus.NOT_FOUND, thrown(1L, 1L, "x@exemplo.test").getStatusCode());
    }
}
