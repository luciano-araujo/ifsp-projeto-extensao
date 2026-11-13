package br.edu.ifsp.tcc.application.storage;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.regex.Pattern;

/** Files under a directory of the local disk (development). Keys that could leave the directory are refused. */
public class LocalFileStorage implements FileStorage {

    // Keys are UUIDs; the pattern also rules out separators, "..", drive letters and empty keys
    private static final Pattern SAFE_KEY = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,254}");

    private final Path root;

    public LocalFileStorage(Path root) {
        this.root = root.toAbsolutePath().normalize();
    }

    @Override
    public void store(String key, InputStream content, long size, String contentType) {
        Path target = resolve(key);
        try {
            Files.createDirectories(root);
            Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new FileStorageException("Could not store the file on disk.", e);
        }
    }

    @Override
    public Resource load(String key) {
        Path file = resolve(key);
        if (!Files.isRegularFile(file)) throw new StorageFileNotFoundException(key);
        return new FileSystemResource(file);
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            throw new FileStorageException("Could not delete the file from disk.", e);
        }
    }

    /** Nothing to archive on local disk (the key is still validated). */
    @Override
    public void archive(String key) {
        resolve(key);
    }

    private Path resolve(String key) {
        if (key == null || !SAFE_KEY.matcher(key).matches()) {
            throw new IllegalArgumentException("Invalid storage key.");
        }
        Path path = root.resolve(key).normalize();
        if (!path.startsWith(root) || path.equals(root)) {
            throw new IllegalArgumentException("Invalid storage key.");
        }
        return path;
    }
}
