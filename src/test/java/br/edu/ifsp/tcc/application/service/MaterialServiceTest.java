package br.edu.ifsp.tcc.application.service;

import br.edu.ifsp.tcc.application.dto.MaterialDownload;
import br.edu.ifsp.tcc.application.dto.MaterialResponse;
import br.edu.ifsp.tcc.application.entity.CourseLesson;
import br.edu.ifsp.tcc.application.entity.KanbanItem;
import br.edu.ifsp.tcc.application.entity.LessonMaterial;
import br.edu.ifsp.tcc.application.entity.Material;
import br.edu.ifsp.tcc.application.entity.User;
import br.edu.ifsp.tcc.application.repository.LessonMaterialRepository;
import br.edu.ifsp.tcc.application.repository.MaterialRepository;
import br.edu.ifsp.tcc.application.storage.FileStorage;
import br.edu.ifsp.tcc.application.storage.FileStorageException;
import br.edu.ifsp.tcc.application.storage.StorageFileNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MaterialServiceTest {

    @Mock private MaterialRepository materialRepository;
    @Mock private LessonMaterialRepository lessonMaterialRepository;
    @Mock private FileStorage fileStorage;
    @InjectMocks private MaterialService service;

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private KanbanItem card(Long id) {
        KanbanItem card = new KanbanItem();
        card.setId(id);
        return card;
    }

    private Material material(Long id, Long cardId, String key) {
        Material material = new Material();
        material.setId(id);
        material.setUuid("uuid-" + id);
        material.setKanbanItem(card(cardId));
        material.setFileName("roteiro.pdf");
        material.setContentType("application/pdf");
        material.setSizeBytes(10L);
        material.setStorageKey(key);
        return material;
    }

    private void assertStatus(HttpStatus status, String message, org.junit.jupiter.api.function.Executable action) {
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, action);
        assertEquals(status, ex.getStatusCode());
        assertEquals(message, ex.getReason());
    }

    // --- Rules on the file ---

    @Test
    void sanitizeFileName_shouldStripPathPartsAndControlCharacters() {
        assertEquals("aula.pdf", MaterialService.sanitizeFileName("C:\\Users\\ana\\docs\\aula.pdf"));
        assertEquals("passwd.txt", MaterialService.sanitizeFileName("../../etc/passwd.txt"));
        assertEquals("a b.png", MaterialService.sanitizeFileName("  a\r\n b\u0000.png "));
        assertEquals("", MaterialService.sanitizeFileName(null));
        assertEquals("", MaterialService.sanitizeFileName("dir/"));
    }

    @Test
    void sanitizeFileName_shouldCutLongNamesKeepingTheExtension() {
        String longName = "a".repeat(400) + ".pdf";

        String sanitized = MaterialService.sanitizeFileName(longName);

        assertEquals(MaterialService.MAX_FILE_NAME_LENGTH, sanitized.length());
        assertTrue(sanitized.endsWith(".pdf"));
    }

    @Test
    void sanitizeFileName_shouldCutLongNamesWithoutUsableExtension() {
        assertEquals(255, MaterialService.sanitizeFileName("b".repeat(300)).length());
        assertEquals(255, MaterialService.sanitizeFileName("b".repeat(300) + ".semextensaocurta").length());
    }

    @Test
    void extensionOf_shouldBeLowerCaseOrEmpty() {
        assertEquals("pdf", MaterialService.extensionOf("A.PDF"));
        assertEquals("gz", MaterialService.extensionOf("a.tar.gz"));
        assertEquals("", MaterialService.extensionOf("semextensao"));
        assertEquals("", MaterialService.extensionOf("termina."));
    }

    @Test
    void requireAllowedType_shouldAcceptEveryAllowedExtensionWithItsContentType() {
        Map<String, String> allowed = Map.ofEntries(
                Map.entry("a.pdf", "application/pdf"),
                Map.entry("a.doc", "application/msword"),
                Map.entry("a.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
                Map.entry("a.ppt", "application/vnd.ms-powerpoint"),
                Map.entry("a.pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
                Map.entry("a.xls", "application/vnd.ms-excel"),
                Map.entry("a.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
                Map.entry("a.txt", "text/plain"),
                Map.entry("a.png", "image/png"),
                Map.entry("a.jpg", "image/jpeg"),
                Map.entry("a.jpeg", "image/jpeg"),
                Map.entry("a.webp", "image/webp"),
                Map.entry("a.gif", "image/gif"));

        allowed.forEach((name, type) -> assertEquals(type, MaterialService.requireAllowedType(name, type), name));
    }

    @Test
    void requireAllowedType_shouldIgnoreCaseAndParametersAndStoreTheCanonicalType() {
        assertEquals("text/plain", MaterialService.requireAllowedType("NOTAS.TXT", "Text/Plain; charset=UTF-8"));
        assertEquals("image/jpeg", MaterialService.requireAllowedType("foto.jpg", "image/jpg"));
    }

    @Test
    void requireAllowedType_shouldRefuseUnknownExtensionOrMismatchedOrMissingType() {
        String message = "Tipo de arquivo não permitido.";
        assertStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE, message,
                () -> MaterialService.requireAllowedType("virus.exe", "application/octet-stream"));
        assertStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE, message,
                () -> MaterialService.requireAllowedType("pagina.html", "text/html"));
        assertStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE, message,
                () -> MaterialService.requireAllowedType("falso.pdf", "text/html"));
        assertStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE, message,
                () -> MaterialService.requireAllowedType("falso.png", "application/pdf"));
        assertStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE, message,
                () -> MaterialService.requireAllowedType("semtipo.pdf", null));
        assertStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE, message,
                () -> MaterialService.requireAllowedType("semextensao", "application/pdf"));
    }

    // --- attach ---

    @Test
    void attach_shouldStoreTheBytesUnderARandomKeyAndSaveTheRow() {
        User uploader = new User();
        uploader.setId(5L);
        MultipartFile file = new MockMultipartFile("file", "..\\sub\\Roteiro Final.pdf", "application/pdf; foo=bar",
                "conteudo".getBytes());
        when(materialRepository.save(any(Material.class))).thenAnswer(inv -> inv.getArgument(0));

        Material saved = service.attach(card(3L), uploader, file);

        ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
        verify(fileStorage).store(key.capture(), any(InputStream.class), eq(8L), eq("application/pdf"));
        assertEquals(key.getValue(), saved.getStorageKey());
        assertTrue(saved.getStorageKey().matches("[0-9a-f-]{36}"));
        assertEquals("Roteiro Final.pdf", saved.getFileName());
        assertEquals("application/pdf", saved.getContentType());
        assertEquals(8L, saved.getSizeBytes());
        assertEquals(3L, saved.getKanbanItemId());
        assertSame(uploader, saved.getUploadedBy());
    }

    @Test
    void attach_shouldRefuseDisallowedTypeBeforeTouchingTheStorage() {
        MultipartFile file = new MockMultipartFile("file", "script.sh", "text/x-sh", "x".getBytes());

        assertStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Tipo de arquivo não permitido.",
                () -> service.attach(card(3L), new User(), file));

        verifyNoInteractions(fileStorage, materialRepository);
    }

    @Test
    void attach_shouldRefuseFilesOver20Mb() {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("grande.pdf");
        when(file.getContentType()).thenReturn("application/pdf");
        when(file.getSize()).thenReturn(MaterialService.MAX_SIZE_BYTES + 1);

        assertStatus(HttpStatus.PAYLOAD_TOO_LARGE, "O arquivo excede o limite de 20 MB.",
                () -> service.attach(card(3L), new User(), file));

        verifyNoInteractions(fileStorage, materialRepository);
    }

    @Test
    void attach_shouldAcceptExactly20Mb() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("limite.pdf");
        when(file.getContentType()).thenReturn("application/pdf");
        when(file.getSize()).thenReturn(MaterialService.MAX_SIZE_BYTES);
        when(file.getInputStream()).thenReturn(InputStream.nullInputStream());
        when(materialRepository.save(any(Material.class))).thenAnswer(inv -> inv.getArgument(0));

        Material saved = service.attach(card(3L), new User(), file);

        assertEquals(MaterialService.MAX_SIZE_BYTES, saved.getSizeBytes());
    }

    @Test
    void attach_shouldFailWhenTheUploadCannotBeRead() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("a.pdf");
        when(file.getContentType()).thenReturn("application/pdf");
        when(file.getSize()).thenReturn(3L);
        when(file.getInputStream()).thenThrow(new IOException("disk"));

        assertThrows(FileStorageException.class, () -> service.attach(card(3L), new User(), file));

        verifyNoInteractions(materialRepository);
    }

    @Test
    void attach_shouldRemoveTheStoredFileWhenTheTransactionRollsBack() {
        TransactionSynchronizationManager.initSynchronization();
        MultipartFile file = new MockMultipartFile("file", "a.pdf", "application/pdf", "abc".getBytes());
        when(materialRepository.save(any(Material.class))).thenAnswer(inv -> inv.getArgument(0));

        Material saved = service.attach(card(3L), new User(), file);
        List<TransactionSynchronization> synchronizations = TransactionSynchronizationManager.getSynchronizations();
        assertEquals(1, synchronizations.size());

        synchronizations.get(0).afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
        verify(fileStorage, never()).delete(anyString());

        synchronizations.get(0).afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
        verify(fileStorage).delete(saved.getStorageKey());
    }

    // --- Lookups ---

    @Test
    void findMaterial_shouldReturnTheMaterialOfTheTeam() {
        Material material = material(7L, 3L, "k");
        when(materialRepository.findInTeam(7L, 10L)).thenReturn(Optional.of(material));

        assertSame(material, service.findMaterial(10L, 7L));
    }

    @Test
    void findMaterial_shouldBeNotFoundForAnotherTeam() {
        when(materialRepository.findInTeam(7L, 99L)).thenReturn(Optional.empty());

        assertStatus(HttpStatus.NOT_FOUND, "Material não encontrado.", () -> service.findMaterial(99L, 7L));
    }

    @Test
    void findByCard_shouldReturnNewestFirstFromTheRepository() {
        List<Material> materials = List.of(material(2L, 3L, "b"), material(1L, 3L, "a"));
        when(materialRepository.findByCardNewestFirst(3L)).thenReturn(materials);

        assertSame(materials, service.findByCard(3L));
    }

    @Test
    void countByCard_shouldDelegate() {
        when(materialRepository.countByKanbanItem_Id(3L)).thenReturn(4);

        assertEquals(4, service.countByCard(3L));
    }

    @Test
    void countByCards_shouldCountInOneQuery() {
        MaterialRepository.CardCount count = mock(MaterialRepository.CardCount.class);
        when(count.getCardId()).thenReturn(3L);
        when(count.getTotal()).thenReturn(2L);
        when(materialRepository.countByCards(List.of(3L, 4L))).thenReturn(List.of(count));

        Map<Long, Integer> counts = service.countByCards(List.of(card(3L), card(4L)));

        assertEquals(Map.of(3L, 2), counts);
    }

    @Test
    void countByCards_shouldSkipTheQueryWithoutCards() {
        assertTrue(service.countByCards(List.of()).isEmpty());

        verifyNoInteractions(materialRepository);
    }

    // --- Download ---

    @Test
    void open_shouldReturnTheFileWithItsHeaders() {
        Material material = material(7L, 3L, "key-7");
        Resource resource = new ByteArrayResource("x".getBytes());
        when(fileStorage.load("key-7")).thenReturn(resource);

        MaterialDownload download = service.open(material, false);

        assertSame(resource, download.getResource());
        assertEquals("application/pdf", download.getContentType());
        assertEquals(10L, download.getSizeBytes());
        assertTrue(download.getContentDisposition().startsWith("attachment; "));
    }

    @Test
    void open_shouldBeInlineWhenAsked() {
        when(fileStorage.load("key-7")).thenReturn(new ByteArrayResource(new byte[0]));

        MaterialDownload download = service.open(material(7L, 3L, "key-7"), true);

        assertTrue(download.getContentDisposition().startsWith("inline; "));
    }

    @Test
    void open_shouldBeNotFoundWhenTheStoredFileIsMissing() {
        when(fileStorage.load("key-7")).thenThrow(new StorageFileNotFoundException("key-7"));

        assertStatus(HttpStatus.NOT_FOUND, "Arquivo não encontrado no armazenamento.",
                () -> service.open(material(7L, 3L, "key-7"), false));
    }

    @Test
    void contentDisposition_shouldEncodeTheNameAsRfc5987WithAnAsciiFallback() {
        String header = MaterialService.contentDisposition("Relatório \"final\" (v2)*.pdf", false);

        assertEquals("attachment; filename=\"Relat_rio 'final' (v2)*.pdf\"; "
                + "filename*=UTF-8''Relat%C3%B3rio%20%22final%22%20%28v2%29%2A.pdf", header);
    }

    @Test
    void contentDisposition_shouldReplaceBackslashesInTheFallback() {
        assertEquals("inline; filename=\"a_b.txt\"; filename*=UTF-8''a%5Cb.txt",
                MaterialService.contentDisposition("a\\b.txt", true));
    }

    // --- Delete and stored files that must not leak ---

    @Test
    void delete_shouldDeleteTheRowAndTheFileAfterCommit() {
        TransactionSynchronizationManager.initSynchronization();
        Material material = material(7L, 3L, "key-7");

        service.delete(material);

        verify(materialRepository).delete(material);
        verify(fileStorage, never()).delete(anyString());
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(fileStorage).delete("key-7");
    }

    @Test
    void delete_shouldKeepTheFileWhenTheTransactionIsNotCommitted() {
        TransactionSynchronizationManager.initSynchronization();

        service.delete(material(7L, 3L, "key-7"));
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        verify(fileStorage, never()).delete(anyString());
    }

    @Test
    void deleteFilesAfterCommit_shouldDeleteAtOnceOutsideATransaction() {
        service.deleteFilesAfterCommit(List.of("a", "b"));

        verify(fileStorage).delete("a");
        verify(fileStorage).delete("b");
    }

    @Test
    void deleteFilesAfterCommit_shouldDoNothingWithoutKeys() {
        TransactionSynchronizationManager.initSynchronization();

        service.deleteFilesAfterCommit(List.of());

        assertTrue(TransactionSynchronizationManager.getSynchronizations().isEmpty());
        verifyNoInteractions(fileStorage);
    }

    @Test
    void deleteFilesAfterCommit_shouldLogAndGoOnWhenAFileCannotBeDeleted() {
        doThrow(new FileStorageException("falhou")).when(fileStorage).delete("a");

        assertDoesNotThrow(() -> service.deleteFilesAfterCommit(List.of("a", "b")));

        verify(fileStorage).delete("b");
    }

    @Test
    void scheduleFileRemovalOfCard_shouldCollectTheKeysThenDeleteThemAfterCommit() {
        TransactionSynchronizationManager.initSynchronization();
        when(materialRepository.findStorageKeysByCard(3L)).thenReturn(List.of("k1", "k2"));

        service.scheduleFileRemovalOfCard(3L);

        verifyNoInteractions(fileStorage);
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(fileStorage).delete("k1");
        verify(fileStorage).delete("k2");
    }

    @Test
    void deleteAllOfTeam_shouldDeleteTheRowsNowAndTheFilesAfterCommit() {
        TransactionSynchronizationManager.initSynchronization();
        when(materialRepository.findStorageKeysByTeam(10L)).thenReturn(List.of("k9"));

        service.deleteAllOfTeam(10L);

        verify(materialRepository).deleteByTeam(10L);
        verifyNoInteractions(fileStorage);
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(fileStorage).delete("k9");
    }

    // --- Lesson links ---

    private CourseLesson lesson(Long id) {
        CourseLesson lesson = new CourseLesson();
        lesson.setId(id);
        return lesson;
    }

    @Test
    void link_shouldSaveTheLink() {
        CourseLesson lesson = lesson(40L);
        Material material = material(7L, 3L, "k");
        when(lessonMaterialRepository.existsByLesson_IdAndMaterial_Id(40L, 7L)).thenReturn(false);
        when(lessonMaterialRepository.save(any(LessonMaterial.class))).thenAnswer(inv -> inv.getArgument(0));

        LessonMaterial link = service.link(lesson, material);

        assertSame(lesson, link.getLesson());
        assertSame(material, link.getMaterial());
    }

    @Test
    void link_shouldBeConflictWhenAlreadyLinked() {
        when(lessonMaterialRepository.existsByLesson_IdAndMaterial_Id(40L, 7L)).thenReturn(true);

        assertStatus(HttpStatus.CONFLICT, "Este material já está vinculado à aula.",
                () -> service.link(lesson(40L), material(7L, 3L, "k")));

        verify(lessonMaterialRepository, never()).save(any());
    }

    @Test
    void unlink_shouldDeleteTheLink() {
        LessonMaterial link = new LessonMaterial();
        when(lessonMaterialRepository.findByLesson_IdAndMaterial_Id(40L, 7L)).thenReturn(Optional.of(link));

        service.unlink(40L, 7L);

        verify(lessonMaterialRepository).delete(link);
    }

    @Test
    void unlink_shouldBeNotFoundWhenNotLinked() {
        when(lessonMaterialRepository.findByLesson_IdAndMaterial_Id(40L, 7L)).thenReturn(Optional.empty());

        assertStatus(HttpStatus.NOT_FOUND, "Este material não está vinculado à aula.", () -> service.unlink(40L, 7L));
    }

    @Test
    void findResponsesByLesson_shouldGroupByLessonInOneQuery() {
        LessonMaterial a = new LessonMaterial();
        a.setLesson(lesson(40L));
        a.setMaterial(material(7L, 3L, "k7"));
        LessonMaterial b = new LessonMaterial();
        b.setLesson(lesson(40L));
        b.setMaterial(material(8L, 3L, "k8"));
        LessonMaterial c = new LessonMaterial();
        c.setLesson(lesson(41L));
        c.setMaterial(material(7L, 3L, "k7"));
        when(lessonMaterialRepository.findByLessons(List.of(40L, 41L, 42L))).thenReturn(List.of(a, b, c));

        Map<Long, List<MaterialResponse>> byLesson = service.findResponsesByLesson(10L, List.of(40L, 41L, 42L));

        assertEquals(List.of(7L, 8L), byLesson.get(40L).stream().map(MaterialResponse::getId).toList());
        assertEquals(1, byLesson.get(41L).size());
        assertNull(byLesson.get(42L));
    }

    @Test
    void findResponsesByLesson_shouldSkipTheQueryWithoutLessons() {
        assertTrue(service.findResponsesByLesson(10L, List.of()).isEmpty());

        verifyNoInteractions(lessonMaterialRepository);
    }

    // --- Mapping ---

    @Test
    void toResponse_shouldMapTheFieldsTheUploaderAndTheDownloadUrl() {
        User uploader = new User();
        uploader.setId(5L);
        uploader.setName("Ana");
        Material material = material(7L, 3L, "k");
        material.setUploadedBy(uploader);

        MaterialResponse response = service.toResponse(material, 10L);

        assertEquals(7L, response.getId());
        assertEquals("uuid-7", response.getUuid());
        assertEquals(3L, response.getCardId());
        assertEquals("roteiro.pdf", response.getFileName());
        assertEquals("application/pdf", response.getContentType());
        assertEquals(10L, response.getSizeBytes());
        assertEquals(5L, response.getUploadedBy().getUserId());
        assertEquals("Ana", response.getUploadedBy().getName());
        assertEquals("/api/v1/teams/10/materials/7/download", response.getDownloadUrl());
    }

    @Test
    void toResponse_shouldHaveNoUploaderAfterTheAccountIsDeleted() {
        Material material = material(7L, 3L, "k");

        assertNull(service.toResponse(material, 10L).getUploadedBy());
    }
}
