package br.edu.ifsp.tcc.application.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock private JavaMailSender mailSender;
    @InjectMocks private EmailService emailService;

    @Test
    void sendRegistrationToken_shouldSendEmailWithCorrectContent() {
        emailService.sendRegistrationToken("user@example.com", "User Name", "123456");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage message = captor.getValue();
        assertEquals("user@example.com", message.getTo()[0]);
        assertTrue(message.getSubject().contains("verificacao"));
        assertTrue(message.getText().contains("User Name"));
        assertTrue(message.getText().contains("123456"));
        assertTrue(message.getText().contains("10 minutos"));
    }
}
