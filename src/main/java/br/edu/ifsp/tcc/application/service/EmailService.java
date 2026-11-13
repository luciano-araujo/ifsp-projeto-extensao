package br.edu.ifsp.tcc.application.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend.base-url:http://localhost:3000}")
    private String frontendBaseUrl;

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

    @Async
    public void sendEmailChangeToken(String to, String name, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Creator Workbench - Confirmação de novo e-mail");
        message.setText(
            "Olá " + name + ",\n\n" +
            "Seu código para confirmar este endereço como o novo e-mail da sua conta é: " + token + "\n\n" +
            "Este código expira em 10 minutos.\n\n" +
            "Se você não pediu esta troca, ignore este e-mail. Sua conta não será alterada."
        );
        mailSender.send(message);
    }

    /** The link points to the frontend page /convite, which reads the token and calls the invite routes. */
    @Async
    public void sendTeamInvite(String to, String inviterName, String teamName, String roleLabel, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Creator Workbench - Convite para a equipe " + teamName);
        message.setText(
            (inviterName == null ? "Você" : inviterName) + " convidou você para a equipe \"" + teamName
                + "\" no Creator Workbench, com o papel de " + roleLabel + ".\n\n" +
            "Para aceitar, abra o link abaixo (entre ou crie uma conta com este e-mail):\n" +
            frontendBaseUrl.replaceAll("/+$", "") + "/convite?token=" + token + "\n\n" +
            "O convite vale por 7 dias. Se você não esperava este convite, ignore este e-mail."
        );
        mailSender.send(message);
    }

    /** Daily deadline digest (RF09): one e-mail per user with the alerts of all their teams. */
    @Async
    public void sendDeadlineDigest(String to, String name, List<String> lines) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject("Creator Workbench - Prazos de hoje");
        message.setText(
            "Olá " + name + ",\n\n" +
            "Estes são os prazos que precisam da sua atenção:\n\n" +
            String.join("\n", lines) + "\n\n" +
            "Veja os detalhes em " + frontendBaseUrl.replaceAll("/+$", "") + "."
        );
        mailSender.send(message);
    }
}
