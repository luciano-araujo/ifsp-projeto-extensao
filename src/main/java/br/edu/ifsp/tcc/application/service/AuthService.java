package br.edu.ifsp.tcc.application.service;

import br.edu.ifsp.tcc.application.dto.CreateUserDTO;
import br.edu.ifsp.tcc.application.entity.RegistrationToken;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.repository.RegistrationTokenRepository;
import br.edu.ifsp.tcc.application.repository.UserRepository;
import br.edu.ifsp.tcc.application.usecase.CreateUserUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RegistrationTokenRepository registrationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final CreateUserUseCase createUserUseCase;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.registration.token-expiry-minutes}")
    private int tokenExpiryMinutes;

    public AuthService(UserRepository userRepository,
                       RegistrationTokenRepository registrationTokenRepository,
                       PasswordEncoder passwordEncoder,
                       EmailService emailService,
                       CreateUserUseCase createUserUseCase) {
        this.userRepository = userRepository;
        this.registrationTokenRepository = registrationTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.createUserUseCase = createUserUseCase;
    }

    public User authenticate(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Credenciais invalidas."));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Credenciais invalidas.");
        }

        return user;
    }

    @Transactional
    public void generateRegistrationToken(String name, String email) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Este e-mail ja esta registrado no sistema.");
        }

        registrationTokenRepository.deleteByEmail(email);

        String tokenCode = String.format("%06d", secureRandom.nextInt(1_000_000));

        RegistrationToken token = new RegistrationToken();
        token.setEmail(email);
        token.setName(name);
        token.setToken(tokenCode);
        token.setExpiresAt(LocalDateTime.now().plusMinutes(tokenExpiryMinutes));

        registrationTokenRepository.save(token);
        emailService.sendRegistrationToken(email, name, tokenCode);
    }

    @Transactional
    public User confirmRegistration(String email, String tokenCode, String password) {
        RegistrationToken token = registrationTokenRepository.findByEmailAndToken(email, tokenCode)
                .orElseThrow(() -> new RuntimeException("Codigo de verificacao invalido."));

        if (token.isExpired()) {
            registrationTokenRepository.delete(token);
            throw new RuntimeException("Codigo de verificacao expirado. Solicite um novo.");
        }

        CreateUserDTO dto = new CreateUserDTO();
        dto.setName(token.getName());
        dto.setEmail(email);
        dto.setPassword(password);

        User user = createUserUseCase.execute(dto);

        registrationTokenRepository.deleteByEmail(email);

        return user;
    }
}
