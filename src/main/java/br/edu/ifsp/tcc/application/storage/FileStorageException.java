package br.edu.ifsp.tcc.application.storage;

/** The storage failed (disk full, S3 unreachable, ...). Becomes a 500 without leaking the cause. */
public class FileStorageException extends RuntimeException {

    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }

    public FileStorageException(String message) {
        super(message);
    }
}
