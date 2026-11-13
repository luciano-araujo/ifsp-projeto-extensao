package br.edu.ifsp.tcc.application.service;

import br.edu.ifsp.tcc.application.dto.MaterialDownload;
import br.edu.ifsp.tcc.application.dto.MaterialResponse;
import br.edu.ifsp.tcc.application.dto.MaterialUploaderResponse;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Materials (files attached to project cards) and their links to course lessons. The only class that
 * talks to the {@link FileStorage}. Business rules (roles, projects only) live in the material use cases.
 */
@Service
public class MaterialService {

    private static final Logger log = LoggerFactory.getLogger(MaterialService.class);

    // Control characters (C0 and C1) and the bidirectional controls that can disguise an extension (U+202E ...)
    private static final String UNSAFE_CHARACTERS = "[\\p{Cc}\\u200E\\u200F\\u202A-\\u202E\\u2066-\\u2069]";

    /** 20 MB per file (contract 7.8). */
    public static final long MAX_SIZE_BYTES = 20L * 1024 * 1024;
    public static final int MAX_FILE_NAME_LENGTH = 255;
    /** Bytes read to check that the content matches the extension. */
    static final int SNIFF_BYTES = 8192;

    public static final String MSG_TOO_LARGE = "O arquivo excede o limite de 20 MB.";
    public static final String MSG_TYPE_NOT_ALLOWED = "Tipo de arquivo não permitido.";

    /** Allowed extensions, each with the content types a browser may send for it; the first one is stored. */
    private static final Map<String, List<String>> TYPES_BY_EXTENSION = Map.ofEntries(
            Map.entry("pdf", List.of("application/pdf")),
            Map.entry("doc", List.of("application/msword")),
            Map.entry("docx", List.of("application/vnd.openxmlformats-officedocument.wordprocessingml.document")),
            Map.entry("ppt", List.of("application/vnd.ms-powerpoint")),
            Map.entry("pptx", List.of("application/vnd.openxmlformats-officedocument.presentationml.presentation")),
            Map.entry("xls", List.of("application/vnd.ms-excel")),
            Map.entry("xlsx", List.of("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")),
            Map.entry("txt", List.of("text/plain")),
            Map.entry("png", List.of("image/png")),
            Map.entry("jpg", List.of("image/jpeg", "image/jpg")),
            Map.entry("jpeg", List.of("image/jpeg", "image/jpg")),
            Map.entry("webp", List.of("image/webp")),
            Map.entry("gif", List.of("image/gif")));

    private final MaterialRepository materialRepository;
    private final LessonMaterialRepository lessonMaterialRepository;
    private final FileStorage fileStorage;

    public MaterialService(MaterialRepository materialRepository,
                           LessonMaterialRepository lessonMaterialRepository,
                           FileStorage fileStorage) {
        this.materialRepository = materialRepository;
        this.lessonMaterialRepository = lessonMaterialRepository;
        this.fileStorage = fileStorage;
    }

    // --- Upload rules ---

    /**
     * File name as stored: no path parts (a browser may send "C:\\dir\\file.pdf"), no control characters,
     * at most {@value #MAX_FILE_NAME_LENGTH} characters (the extension is kept when cutting).
     */
    public static String sanitizeFileName(String original) {
        if (original == null) return "";
        String name = original.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        name = name.replaceAll(UNSAFE_CHARACTERS, "").trim();
        if (name.length() > 1 && name.startsWith(".") && name.indexOf('.', 1) < 0) name = "arquivo" + name; // ".pdf" has no base name
        if (name.length() > MAX_FILE_NAME_LENGTH) {
            String extension = extensionOf(name);
            String suffix = extension.isEmpty() || extension.length() > 10 ? "" : "." + extension;
            int cut = MAX_FILE_NAME_LENGTH - suffix.length();
            // Never cut between the two halves of a surrogate pair
            if (Character.isHighSurrogate(name.charAt(cut - 1))) cut--;
            name = name.substring(0, cut) + suffix;
        }
        return name;
    }

    /** Lower-case extension without the dot; empty when the name has none. */
    public static String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 || dot == fileName.length() - 1 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * The content type to store for the file (always the one of the extension), or 415 when the extension is not
     * allowed or the content type sent conflicts with it. An empty or application/octet-stream type is accepted,
     * because Windows browsers often send it for docx, xlsx and pptx.
     */
    public static String requireAllowedType(String fileName, String declaredContentType) {
        List<String> accepted = TYPES_BY_EXTENSION.get(extensionOf(fileName));
        String declared = declaredContentType == null ? ""
                : declaredContentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        boolean generic = declared.isEmpty() || declared.equals("application/octet-stream");
        if (accepted == null || !(generic || accepted.contains(declared))) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, MSG_TYPE_NOT_ALLOWED);
        }
        return accepted.get(0);
    }

    /**
     * The first bytes must look like the extension says (a script renamed to .pdf is refused with 415). Reads at most
     * {@value #SNIFF_BYTES} bytes and leaves the stream at its start (mark/reset), so the file is never loaded whole.
     */
    static void requireContentMatchesExtension(String fileName, BufferedInputStream content) throws IOException {
        content.mark(SNIFF_BYTES + 1);
        byte[] head = content.readNBytes(SNIFF_BYTES);
        content.reset();
        if (!contentMatches(extensionOf(fileName), head)) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, MSG_TYPE_NOT_ALLOWED);
        }
    }

    static boolean contentMatches(String extension, byte[] head) {
        return switch (extension) {
            case "pdf" -> startsWith(head, 0, '%', 'P', 'D', 'F', '-');
            case "png" -> startsWith(head, 0, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A);
            case "jpg", "jpeg" -> startsWith(head, 0, 0xFF, 0xD8, 0xFF);
            case "gif" -> startsWith(head, 0, 'G', 'I', 'F', '8', '7', 'a') || startsWith(head, 0, 'G', 'I', 'F', '8', '9', 'a');
            case "webp" -> startsWith(head, 0, 'R', 'I', 'F', 'F') && startsWith(head, 8, 'W', 'E', 'B', 'P');
            case "docx", "xlsx", "pptx" -> startsWith(head, 0, 'P', 'K', 0x03, 0x04);
            case "doc", "xls", "ppt" -> startsWith(head, 0, 0xD0, 0xCF, 0x11, 0xE0, 0xA1, 0xB1, 0x1A, 0xE1);
            case "txt" -> noNulByte(head);
            default -> false;
        };
    }

    private static boolean startsWith(byte[] head, int offset, int... expected) {
        if (head.length < offset + expected.length) return false;
        for (int i = 0; i < expected.length; i++) {
            if ((head[offset + i] & 0xFF) != expected[i]) return false;
        }
        return true;
    }

    private static boolean noNulByte(byte[] head) {
        for (byte b : head) {
            if (b == 0) return false;
        }
        return true;
    }

    // --- Materials ---

    /** Validates the upload, saves the bytes in the storage and the row in the database. */
    public Material attach(KanbanItem card, User uploader, MultipartFile file) {
        String fileName = sanitizeFileName(file.getOriginalFilename());
        String contentType = requireAllowedType(fileName, file.getContentType());
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, MSG_TOO_LARGE);
        }

        String key = UUID.randomUUID().toString();
        try (InputStream raw = file.getInputStream(); BufferedInputStream content = new BufferedInputStream(raw)) {
            requireContentMatchesExtension(fileName, content);
            // Registered before storing: if the copy fails half way, the partial file is removed too
            removeFileOnRollback(key);
            fileStorage.store(key, content, file.getSize(), contentType);
        } catch (IOException e) {
            throw new FileStorageException("Could not read the uploaded file.", e);
        }

        Material material = new Material();
        material.setKanbanItem(card);
        material.setUploadedBy(uploader);
        material.setFileName(fileName);
        material.setContentType(contentType);
        material.setSizeBytes(file.getSize());
        material.setStorageKey(key);
        return materialRepository.save(material);
    }

    /** The material only when it belongs to a card of the team; otherwise 404 (other teams' ids are not revealed). */
    public Material findMaterial(Long teamId, Long materialId) {
        return materialRepository.findInTeam(materialId, teamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Material não encontrado."));
    }

    /** The material to link to a lesson: 400 when it does not exist in the team (unknown or another team's). */
    public Material findMaterialToLink(Long teamId, Long materialId) {
        return materialRepository.findInTeam(materialId, teamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "O material não pertence a esta equipe."));
    }

    /** Materials of the card, newest first. */
    public List<Material> findByCard(Long cardId) {
        return materialRepository.findByCardNewestFirst(cardId);
    }

    public int countByCard(Long cardId) {
        return materialRepository.countByKanbanItem_Id(cardId);
    }

    /** Number of materials of each card, in one query for all of them (cards without materials are absent). */
    public Map<Long, Integer> countByCards(List<KanbanItem> cards) {
        if (cards.isEmpty()) return Map.of();
        return materialRepository.countByCards(cards.stream().map(KanbanItem::getId).toList()).stream()
                .collect(Collectors.toMap(c -> c.getCardId(), c -> c.getTotal().intValue()));
    }

    /** Opens the stored file for download. */
    public MaterialDownload open(Material material, boolean inline) {
        Resource resource;
        try {
            resource = fileStorage.load(material.getStorageKey());
        } catch (StorageFileNotFoundException e) {
            log.error("Material {} has no file in the storage (key {})", material.getId(), material.getStorageKey());
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Arquivo não encontrado no armazenamento.");
        }
        String contentType = "text/plain".equals(material.getContentType())
                ? "text/plain; charset=UTF-8" : material.getContentType();
        return new MaterialDownload(resource, contentType, material.getSizeBytes(),
                contentDisposition(material.getFileName(), inline));
    }

    /**
     * Content-Disposition with an ASCII fallback name and the real name encoded as in RFC 5987
     * ("filename*=UTF-8''..."), attachment by default and inline when asked.
     */
    public static String contentDisposition(String fileName, boolean inline) {
        String fallback = fileName.replaceAll("[^\\x20-\\x7E]", "_").replace('"', '\'').replace('\\', '_');
        String encoded = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20").replace("*", "%2A");
        return (inline ? "inline" : "attachment") + "; filename=\"" + fallback + "\"; filename*=UTF-8''" + encoded;
    }

    /** Deletes the row (its lesson links go with it, by cascade) and, after commit, the stored file. */
    public void delete(Material material) {
        materialRepository.delete(material);
        deleteFilesAfterCommit(List.of(material.getStorageKey()));
    }

    // --- Stored files that must not leak ---

    /** Call before a card is deleted: its materials cascade away in the database, so the files go after commit. */
    public void scheduleFileRemovalOfCard(Long cardId) {
        deleteFilesAfterCommit(materialRepository.findStorageKeysByCard(cardId));
    }

    /** Call before a team is deleted: its materials cascade away with its cards, so the files go after commit. */
    public void scheduleFileRemovalOfTeam(Long teamId) {
        deleteFilesAfterCommit(materialRepository.findStorageKeysByTeam(teamId));
    }

    /**
     * Deletes the files once the current transaction commits (a rollback keeps them, because the rows stay).
     * Outside a transaction they are deleted at once. A failure is logged, never thrown: the data is already gone.
     */
    public void deleteFilesAfterCommit(Collection<String> keys) {
        if (keys.isEmpty()) return;
        List<String> copy = List.copyOf(keys);
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            copy.forEach(this::deleteQuietly);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                copy.forEach(MaterialService.this::deleteQuietly);
            }
        });
    }

    private void removeFileOnRollback(String key) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) deleteQuietly(key);
            }
        });
    }

    private void deleteQuietly(String key) {
        try {
            fileStorage.delete(key);
        } catch (RuntimeException e) {
            log.error("Could not delete the stored file {}", key, e);
        }
    }

    // --- Archive (RN04) ---

    /**
     * Archives every material of courses published on or before the limit that is not archived yet, then marks the
     * row, one material at a time. Call it outside a transaction, so none stays open across the S3 calls. A material whose archive fails is logged and left for the next run. Returns how many were archived.
     */
    public int archiveDueMaterials(LocalDateTime limit) {
        int archived = 0;
        for (Material material : materialRepository.findDueForArchive(limit)) {
            try {
                fileStorage.archive(material.getStorageKey());
            } catch (RuntimeException e) {
                log.error("Could not archive material {} (key {})", material.getId(), material.getStorageKey(), e);
                continue;
            }
            // Marked right after its own S3 copy, in its own transaction: a crash never leaves archived objects unmarked
            materialRepository.markArchived(material.getId(), LocalDateTime.now());
            archived++;
        }
        return archived;
    }

    // --- Lesson links ---

    /** Links the material to the lesson; 409 when it is already linked. */
    public LessonMaterial link(CourseLesson lesson, Material material) {
        if (lessonMaterialRepository.existsByLesson_IdAndMaterial_Id(lesson.getId(), material.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este material já está vinculado à aula.");
        }
        LessonMaterial link = new LessonMaterial();
        link.setLesson(lesson);
        link.setMaterial(material);
        return lessonMaterialRepository.save(link);
    }

    public void unlink(Long lessonId, Long materialId) {
        LessonMaterial link = lessonMaterialRepository.findByLesson_IdAndMaterial_Id(lessonId, materialId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Este material não está vinculado à aula."));
        lessonMaterialRepository.delete(link);
    }

    /** Materials of each lesson as responses (one query for all the lessons; lessons without materials are absent). */
    public Map<Long, List<MaterialResponse>> findResponsesByLesson(Long teamId, Collection<Long> lessonIds) {
        if (lessonIds.isEmpty()) return Map.of();
        Map<Long, List<MaterialResponse>> byLesson = new HashMap<>();
        for (LessonMaterial link : lessonMaterialRepository.findByLessons(lessonIds)) {
            byLesson.computeIfAbsent(link.getLessonId(), id -> new ArrayList<>())
                    .add(toResponse(link.getMaterial(), teamId));
        }
        return byLesson;
    }

    // --- Response mapping ---

    public MaterialResponse toResponse(Material material, Long teamId) {
        User uploader = material.getUploadedBy();
        MaterialUploaderResponse uploadedBy = uploader == null ? null
                : new MaterialUploaderResponse(uploader.getId(), uploader.getName());
        return new MaterialResponse(material.getId(), material.getUuid(), material.getKanbanItemId(),
                material.getFileName(), material.getContentType(), material.getSizeBytes(), uploadedBy,
                material.getCreatedAt(), downloadUrl(teamId, material.getId()));
    }

    public static String downloadUrl(Long teamId, Long materialId) {
        return "/api/v1/teams/" + teamId + "/materials/" + materialId + "/download";
    }
}
