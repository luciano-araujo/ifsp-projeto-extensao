package br.edu.ifsp.tcc.application.entity;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class MaterialTest {

    @Test
    void onCreate_shouldGenerateUuidAndCreatedAt() {
        Material material = new Material();

        material.onCreate();

        assertEquals(36, material.getUuid().length());
        assertNotNull(material.getCreatedAt());
    }

    @Test
    void onCreate_shouldKeepAnExistingUuid() {
        Material material = new Material();
        material.setUuid("fixo");

        material.onCreate();

        assertEquals("fixo", material.getUuid());
    }

    @Test
    void getKanbanItemId_shouldBeNullWithoutCardAndTheCardIdOtherwise() {
        Material material = new Material();
        assertNull(material.getKanbanItemId());

        KanbanItem card = new KanbanItem();
        card.setId(5L);
        material.setKanbanItem(card);

        assertEquals(5L, material.getKanbanItemId());
        assertSame(card, material.getKanbanItem());
    }

    @Test
    void setters_shouldExposeValues() {
        Material material = new Material();
        User uploader = new User();
        LocalDateTime archivedAt = LocalDateTime.of(2026, 12, 1, 10, 0);
        material.setId(1L);
        material.setUploadedBy(uploader);
        material.setFileName("roteiro.pdf");
        material.setContentType("application/pdf");
        material.setSizeBytes(123L);
        material.setStorageKey("chave");
        material.setArchivedAt(archivedAt);

        assertEquals(1L, material.getId());
        assertSame(uploader, material.getUploadedBy());
        assertEquals("roteiro.pdf", material.getFileName());
        assertEquals("application/pdf", material.getContentType());
        assertEquals(123L, material.getSizeBytes());
        assertEquals("chave", material.getStorageKey());
        assertEquals(archivedAt, material.getArchivedAt());
    }
}
