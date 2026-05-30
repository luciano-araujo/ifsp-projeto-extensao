package br.edu.ifsp.tcc.application.service;

import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

@Service
public class AuthService {

    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User authenticate(String email, String password) {
        // to implement
    }

    public void generateRegistrationToken(String name, String email) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new RuntimeException("Este e-mail já está registrado no sistema.");
        }

        String token = String.format("%06d", new Random().nextInt(999999));
//        tokenStore.put(email, token);
    }

    public User confirmRegistration(String email, String token, String password) {
//        String storedToken = tokenStore.get(email);
//
//        if (storedToken == null || !storedToken.equals(token)) {
//            throw new RuntimeException("Código de segurança inválido ou expirado.");
//        }

        User newUser = new User();
        newUser.setName(email.split("@")[0]);
        newUser.setEmail(email);
        newUser.setPassword(password);

        userRepository.save(newUser);
//        tokenStore.remove(email);

        return newUser;
    }
}