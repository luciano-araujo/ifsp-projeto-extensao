package br.edu.ifsp.tcc.application.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LocalFileStorageTest {

    @TempDir Path temp;
    private Path root;
    private LocalFileStorage storage;

    @BeforeEach
    void setUp() {
        root = temp.resolve("uploads");
        storage = new LocalFileStorage(root);
    }

    private void store(String key, String content) {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        storage.store(key, new ByteArrayInputStream(bytes), bytes.length, "text/plain");
    }

    @Test
    void store_shouldCreateTheDirectoryAndWriteTheFile() throws IOException {
        store("0b1c2d3e-aaaa-bbbb-cccc-1234567890ab", "ola");

        assertEquals("ola", Files.readString(root.resolve("0b1c2d3e-aaaa-bbbb-cccc-1234567890ab")));
    }

    @Test
    void store_shouldReplaceAnExistingKey() throws IOException {
        store("k1", "um");
        store("k1", "dois");

        assertEquals("dois", Files.readString(root.resolve("k1")));
    }

    @Test
    void load_shouldReturnTheStoredContent() throws IOException {
        store("k1", "conteudo");

        Resource resource = storage.load("k1");

        assertEquals("conteudo", new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
        assertEquals(8, resource.contentLength());
    }

    @Test
    void load_shouldFailForAMissingKey() {
        assertThrows(StorageFileNotFoundException.class, () -> storage.load("nao-existe"));
    }

    @Test
    void load_shouldNotTreatADirectoryAsAFile() throws IOException {
        Files.createDirectories(root.resolve("pasta"));

        assertThrows(StorageFileNotFoundException.class, () -> storage.load("pasta"));
    }

    @Test
    void delete_shouldRemoveTheFileAndIgnoreAMissingOne() {
        store("k1", "x");

        storage.delete("k1");
        storage.delete("k1");

        assertFalse(Files.exists(root.resolve("k1")));
    }

    @Test
    void delete_shouldFailWhenTheDiskRefuses() throws IOException {
        Files.createDirectories(root.resolve("pasta"));
        Files.writeString(root.resolve("pasta").resolve("dentro"), "x");

        assertThrows(FileStorageException.class, () -> storage.delete("pasta"));
    }

    @Test
    void store_shouldFailWhenTheDirectoryCannotBeCreated() throws IOException {
        Path blocker = Files.writeString(temp.resolve("arquivo"), "x");
        LocalFileStorage broken = new LocalFileStorage(blocker.resolve("sub"));

        assertThrows(FileStorageException.class,
                () -> broken.store("k1", new ByteArrayInputStream(new byte[0]), 0, "text/plain"));
    }

    @Test
    void archive_shouldKeepTheFileAsItIsOnLocalDisk() {
        store("k1", "x");

        storage.archive("k1");

        assertTrue(Files.exists(root.resolve("k1")));
    }

    @Test
    void everyOperationShouldRefuseKeysThatCouldLeaveTheDirectory() throws IOException {
        Files.writeString(temp.resolve("segredo.txt"), "segredo");
        String[] bad = {"../segredo.txt", "..", ".", "a/b", "a\\b", "/etc/passwd", "C:\\Windows\\win.ini",
                "..\\segredo.txt", "", " ", "%2e%2e/segredo.txt", ".oculto", "a\u0000b"};

        for (String key : bad) {
            assertThrows(IllegalArgumentException.class, () -> storage.load(key), "load " + key);
            assertThrows(IllegalArgumentException.class, () -> storage.delete(key), "delete " + key);
            assertThrows(IllegalArgumentException.class, () -> storage.archive(key), "archive " + key);
            assertThrows(IllegalArgumentException.class,
                    () -> storage.store(key, new ByteArrayInputStream(new byte[0]), 0, "text/plain"), "store " + key);
        }
        assertThrows(IllegalArgumentException.class, () -> storage.load(null));
        assertEquals("segredo", Files.readString(temp.resolve("segredo.txt")));
    }
}
