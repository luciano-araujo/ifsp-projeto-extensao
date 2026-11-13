package br.edu.ifsp.tcc.application.controller;

import br.edu.ifsp.tcc.application.dto.AlertResponse;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.usecase.ListAlertsUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertControllerTest {

    @Mock private ListAlertsUseCase listAlertsUseCase;
    @InjectMocks private AlertController controller;

    @Test
    void list_shouldReturnAlerts() {
        User user = new User();
        user.setId(1L);
        List<AlertResponse> alerts = List.of();
        when(listAlertsUseCase.execute(10L, 1L)).thenReturn(alerts);

        ResponseEntity<List<AlertResponse>> response = controller.list(user, 10L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(alerts, response.getBody());
    }
}
