package br.edu.ifsp.tcc.application.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void sendRegistrationToken(String to, String name, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Creator Workbench - Código de verificação");
        message.setText(
            "Olá " + name + ",\n\n" +
            "Seu código de verificação para concluir o cadastro é: " + token + "\n\n" +
            "Este código expira em 10 minutos.\n\n" +
            "Se você não solicitou este cadastro, ignore este e-mail."
        );
        mailSender.send(message);
    }

    @Async
    public void sendPasswordResetToken(String to, String name, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Creator Workbench - Recuperação de senha");
        message.setText(
            "Olá " + name + ",\n\n" +
            "Seu código para redefinir a senha é: " + token + "\n\n" +
            "Este código expira em 10 minutos.\n\n" +
            "Se você não pediu para trocar a senha, ignore este e-mail. Sua senha atual continua valendo."
        );
        mailSender.send(message);
    }
}
