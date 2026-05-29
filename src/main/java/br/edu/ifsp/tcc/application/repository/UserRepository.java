package br.edu.ifsp.tcc.application.repository;

import br.edu.ifsp.tcc.application.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // This custom method is required by your AuthService
    Optional<User> findByEmail(String email);
}