package br.edu.ifsp.tcc.application.repository;

import br.edu.ifsp.tcc.application.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Used by login, registration and team member lookup (through UserService)
    Optional<User> findByEmail(String email);
}