package br.edu.ifsp.tcc.application.service;

import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User save(User user) {
        // JPA automatically handles the INSERT command
        return userRepository.save(user);
    }

    public Optional<User> findById(Long id) {
        // JPA automatically handles the SELECT * WHERE id = ? command
        return userRepository.findById(id);
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public void deleteById(Long id) {
        // JPA automatically handles the DELETE command
        userRepository.deleteById(id);
    }
}