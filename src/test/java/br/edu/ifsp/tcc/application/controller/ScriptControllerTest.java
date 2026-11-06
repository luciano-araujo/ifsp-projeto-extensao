package br.edu.ifsp.tcc.application.controller;

import br.edu.ifsp.tcc.application.dto.SaveScriptDTO;
import br.edu.ifsp.tcc.application.entity.Script;
import br.edu.ifsp.tcc.application.entity.ScriptVersion;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.usecase.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScriptControllerTest {

    @Mock private SaveScriptUseCase saveScriptUseCase;
    @Mock private GetScriptUseCase getScriptUseCase;
    @Mock private CreateScriptVersionUseCase createScriptVersionUseCase;
    @Mock private GetScriptVersionsUseCase getScriptVersionsUseCase;
    @Mock private RestoreScriptVersionUseCase restoreScriptVersionUseCase;
    @InjectMocks private ScriptController controller;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
    }

    @Test
    void save_shouldSaveContentAndReturnScript() {
        SaveScriptDTO dto = new SaveScriptDTO();
        dto.setContent("<p>roteiro</p>");
        Script script = new Script();
        when(saveScriptUseCase.execute(5L, 1L, "<p>roteiro</p>")).thenReturn(script);

        ResponseEntity<Script> response = controller.save(user, 5L, dto);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(script, response.getBody());
    }

    @Test
    void get_shouldReturnScript() {
        Script script = new Script();
        when(getScriptUseCase.execute(5L, 1L)).thenReturn(script);

        ResponseEntity<Script> response = controller.get(user, 5L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(script, response.getBody());
    }

    @Test
    void createVersion_shouldReturnVersion() {
        ScriptVersion version = new ScriptVersion();
        when(createScriptVersionUseCase.execute(5L, 1L)).thenReturn(version);

        ResponseEntity<ScriptVersion> response = controller.createVersion(user, 5L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(version, response.getBody());
    }

    @Test
    void listVersions_shouldReturnVersions() {
        List<ScriptVersion> versions = List.of(new ScriptVersion());
        when(getScriptVersionsUseCase.execute(5L, 1L)).thenReturn(versions);

        ResponseEntity<List<ScriptVersion>> response = controller.listVersions(user, 5L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(versions, response.getBody());
    }

    @Test
    void restoreVersion_shouldReturnRestoredScript() {
        Script script = new Script();
        when(restoreScriptVersionUseCase.execute(5L, 1L, 9L)).thenReturn(script);

        ResponseEntity<Script> response = controller.restoreVersion(user, 5L, 9L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(script, response.getBody());
    }
}
