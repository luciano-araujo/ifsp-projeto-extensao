package br.edu.ifsp.tcc.application.usecase;

import br.edu.ifsp.tcc.application.dto.ChangePasswordDTO;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.dto.TokenResponse;
import br.edu.ifsp.tcc.application.service.JwtService;
import br.edu.ifsp.tcc.application.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChangePasswordUseCaseTest {

    @Mock private UserService userService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @InjectMocks private ChangePasswordUseCase useCase;

    private User ana;

    @BeforeEach
    void setUp() {
        ana = new User();
        ana.setId(1L);
        ana.setPassword("old-hash");
    }

    private ChangePasswordDTO dto(String current, String next) {
        ChangePasswordDTO dto = new ChangePasswordDTO();
        dto.setCurrentPassword(current);
        dto.setNewPassword(next);
        return dto;
    }

    private ResponseStatusException thrown(Long id, Long caller, ChangePasswordDTO dto) {
        return assertThrows(ResponseStatusException.class, () -> useCase.execute(id, caller, dto));
    }

    @Test
    void execute_shouldEncodeAndSaveTheNewPassword() {
        when(userService.findById(1L)).thenReturn(Optional.of(ana));
        when(passwordEncoder.matches("atual123", "old-hash")).thenReturn(true);
        when(passwordEncoder.encode("nova1234")).thenReturn("new-hash");

        when(jwtService.generateToken(ana)).thenReturn("fresh-jwt");

        TokenResponse response = useCase.execute(1L, 1L, dto("atual123", "nova1234"));

        assertEquals("fresh-jwt", response.getToken());
        assertEquals(1, ana.getTokenVersion());
        assertEquals("new-hash", ana.getPassword());
        verify(userService).update(ana);
    }

    @Test
    void execute_shouldForbidOtherUsers() {
        assertEquals(HttpStatus.FORBIDDEN, thrown(1L, 2L, dto("a", "bbbbbb")).getStatusCode());
        verifyNoInteractions(userService);
    }

    @Test
    void execute_shouldReturn404ForUnknownUser() {
        when(userService.findById(1L)).thenReturn(Optional.empty());

        assertEquals(HttpStatus.NOT_FOUND, thrown(1L, 1L, dto("a", "bbbbbb")).getStatusCode());
    }

    @Test
    void execute_shouldReturn400WhenCurrentPasswordIsWrong() {
        when(userService.findById(1L)).thenReturn(Optional.of(ana));
        when(passwordEncoder.matches("errada", "old-hash")).thenReturn(false);

        ResponseStatusException ex = thrown(1L, 1L, dto("errada", "nova1234"));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Senha atual incorreta.", ex.getReason());
        verify(userService, never()).update(any());
    }

    @Test
    void execute_shouldTreatACurrentPasswordOver72BytesAsWrong() {
        when(userService.findById(1L)).thenReturn(Optional.of(ana));

        assertEquals(HttpStatus.BAD_REQUEST, thrown(1L, 1L, dto("a".repeat(73), "nova1234")).getStatusCode());
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void execute_shouldRejectANewPasswordOver72Bytes() {
        when(userService.findById(1L)).thenReturn(Optional.of(ana));
        when(passwordEncoder.matches("atual123", "old-hash")).thenReturn(true);

        // 37 accented characters = 74 bytes
        assertEquals(HttpStatus.BAD_REQUEST, thrown(1L, 1L, dto("atual123", "é".repeat(37))).getStatusCode());
        verify(userService, never()).update(any());
    }
}
