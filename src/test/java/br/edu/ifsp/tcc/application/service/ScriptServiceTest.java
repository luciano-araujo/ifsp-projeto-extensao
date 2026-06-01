package br.edu.ifsp.tcc.application.service;

import br.edu.ifsp.tcc.application.entity.Script;
import br.edu.ifsp.tcc.application.entity.ScriptVersion;
import br.edu.ifsp.tcc.application.repository.ScriptRepository;
import br.edu.ifsp.tcc.application.repository.ScriptVersionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScriptServiceTest {

    @Mock private ScriptRepository scriptRepository;
    @Mock private ScriptVersionRepository scriptVersionRepository;
    @InjectMocks private ScriptService scriptService;

    @Test
    void save_shouldDelegateToRepository() {
        Script script = new Script();
        when(scriptRepository.save(script)).thenReturn(script);
        assertEquals(script, scriptService.save(script));
    }

    @Test
    void findByKanbanItemId_shouldReturnScript() {
        Script script = new Script();
        when(scriptRepository.findByKanbanItem_Id(1L)).thenReturn(Optional.of(script));
        assertTrue(scriptService.findByKanbanItemId(1L).isPresent());
    }

    @Test
    void findByKanbanItemId_shouldReturnEmptyWhenNotFound() {
        when(scriptRepository.findByKanbanItem_Id(99L)).thenReturn(Optional.empty());
        assertTrue(scriptService.findByKanbanItemId(99L).isEmpty());
    }

    @Test
    void saveVersion_shouldDelegateToRepository() {
        ScriptVersion version = new ScriptVersion();
        when(scriptVersionRepository.save(version)).thenReturn(version);
        assertEquals(version, scriptService.saveVersion(version));
    }

    @Test
    void findVersionsByScriptId_shouldReturnOrderedVersions() {
        List<ScriptVersion> versions = List.of(new ScriptVersion(), new ScriptVersion());
        when(scriptVersionRepository.findByScript_IdOrderByCreatedAtDesc(1L)).thenReturn(versions);
        assertEquals(2, scriptService.findVersionsByScriptId(1L).size());
    }

    @Test
    void findVersionById_shouldReturnVersion() {
        ScriptVersion version = new ScriptVersion();
        when(scriptVersionRepository.findById(1L)).thenReturn(Optional.of(version));
        assertTrue(scriptService.findVersionById(1L).isPresent());
    }
}
