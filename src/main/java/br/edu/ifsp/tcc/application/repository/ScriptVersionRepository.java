package br.edu.ifsp.tcc.application.repository;

import br.edu.ifsp.tcc.application.entity.ScriptVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScriptVersionRepository extends JpaRepository<ScriptVersion, Long> {
    List<ScriptVersion> findByScript_IdOrderByCreatedAtDesc(Long scriptId);
}
