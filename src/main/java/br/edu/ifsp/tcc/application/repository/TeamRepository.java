package br.edu.ifsp.tcc.application.repository;

import br.edu.ifsp.tcc.application.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
}
