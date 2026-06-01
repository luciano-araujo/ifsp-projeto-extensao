package br.edu.ifsp.tcc.application.service;

import br.edu.ifsp.tcc.application.entity.Script;
import br.edu.ifsp.tcc.application.entity.ScriptVersion;
import br.edu.ifsp.tcc.application.repository.ScriptRepository;
import br.edu.ifsp.tcc.application.repository.ScriptVersionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ScriptService {

    private final ScriptRepository scriptRepository;
    private final ScriptVersionRepository scriptVersionRepository;

    public ScriptService(ScriptRepository scriptRepository, ScriptVersionRepository scriptVersionRepository) {
        this.scriptRepository = scriptRepository;
        this.scriptVersionRepository = scriptVersionRepository;
    }

    public Script save(Script script) {
        return scriptRepository.save(script);
    }

    public Optional<Script> findByKanbanItemId(Long kanbanItemId) {
        return scriptRepository.findByKanbanItem_Id(kanbanItemId);
    }

    public ScriptVersion saveVersion(ScriptVersion version) {
        return scriptVersionRepository.save(version);
    }

    public List<ScriptVersion> findVersionsByScriptId(Long scriptId) {
        return scriptVersionRepository.findByScript_IdOrderByCreatedAtDesc(scriptId);
    }

    public Optional<ScriptVersion> findVersionById(Long versionId) {
        return scriptVersionRepository.findById(versionId);
    }
}
