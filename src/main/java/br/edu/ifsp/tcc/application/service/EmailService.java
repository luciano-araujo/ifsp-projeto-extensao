package br.edu.ifsp.tcc.application.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendRegistrationToken(String to, String name, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Creator Workbench - Codigo de verificacao");
        message.setText(
            "Ola " + name + ",\n\n" +
            "Seu codigo de verificacao para concluir o cadastro e: " + token + "\n\n" +
            "Este codigo expira em 10 minutos.\n\n" +
            "Se voce nao solicitou este cadastro, ignore este e-mail."
        );
        mailSender.send(message);
    }
}
