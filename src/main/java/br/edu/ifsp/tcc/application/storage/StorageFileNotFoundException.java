package br.edu.ifsp.tcc.application.storage;

/** The key has no file in the storage. */
public class StorageFileNotFoundException extends FileStorageException {

    public StorageFileNotFoundException(String key) {
        super("File not found in storage: " + key);
    }
}
