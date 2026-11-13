package br.edu.ifsp.tcc.application.storage;

import org.springframework.core.io.Resource;

import java.io.InputStream;

/**
 * Where the bytes of the materials live (RF06): local disk in development, S3 in production,
 * selected by {@code app.storage.type}. Only MaterialService uses it.
 */
public interface FileStorage {

    /** Saves the content under the key (a random UUID chosen by the caller). */
    void store(String key, InputStream content, long size, String contentType);

    /** The stored file; throws {@link StorageFileNotFoundException} when the key does not exist. */
    Resource load(String key);

    /** Removes the file; a key that does not exist is not an error. */
    void delete(String key);

    /** Moves the file to cheaper storage (RN04). The file stays downloadable. A no-op on local disk. */
    void archive(String key);
}
