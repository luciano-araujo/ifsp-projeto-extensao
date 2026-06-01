package br.edu.ifsp.tcc.application.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ScriptVersionTest {

    @Test
    void getScriptId_shouldReturnIdWhenSet() {
        Script script = new Script();
        script.setId(10L);
        ScriptVersion version = new ScriptVersion();
        version.setScript(script);
        assertEquals(10L, version.getScriptId());
    }

    @Test
    void getScriptId_shouldReturnNullWhenNotSet() {
        ScriptVersion version = new ScriptVersion();
        assertNull(version.getScriptId());
    }

    @Test
    void settersAndGetters_shouldWork() {
        ScriptVersion version = new ScriptVersion();
        version.setId(1L);
        version.setContent("<p>Snapshot</p>");
        assertEquals(1L, version.getId());
        assertEquals("<p>Snapshot</p>", version.getContent());
    }
}
