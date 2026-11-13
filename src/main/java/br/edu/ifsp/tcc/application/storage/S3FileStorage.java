package br.edu.ifsp.tcc.application.storage;

import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.Resource;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.StorageClass;

import java.io.InputStream;

/** Files in an S3 bucket (production). Credentials come from the default AWS provider chain. */
public class S3FileStorage implements FileStorage {

    private final S3Client s3;
    private final String bucket;

    public S3FileStorage(S3Client s3, String bucket) {
        this.s3 = s3;
        this.bucket = bucket;
    }

    @Override
    public void store(String key, InputStream content, long size, String contentType) {
        try {
            s3.putObject(PutObjectRequest.builder().bucket(bucket).key(key).contentType(contentType)
                    .contentLength(size).build(), RequestBody.fromInputStream(content, size));
        } catch (SdkException e) {
            throw new FileStorageException("Could not store the file on S3.", e);
        }
    }

    @Override
    public Resource load(String key) {
        try {
            // Only a HEAD here: the object is opened when (and if) the body is read, so a HEAD request to the
            // download route never leaves an S3 stream open
            long length = s3.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build()).contentLength();
            return new AbstractResource() {
                @Override
                public String getDescription() {
                    return "S3 object " + key;
                }

                @Override
                public long contentLength() {
                    return length;
                }

                @Override
                public InputStream getInputStream() {
                    try {
                        return s3.getObject(GetObjectRequest.builder().bucket(bucket).key(key).build());
                    } catch (SdkException e) {
                        throw new FileStorageException("Could not read the file from S3.", e);
                    }
                }
            };
        } catch (NoSuchKeyException e) {
            throw new StorageFileNotFoundException(key);
        } catch (SdkException e) {
            throw new FileStorageException("Could not read the file from S3.", e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            s3.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (SdkException e) {
            throw new FileStorageException("Could not delete the file from S3.", e);
        }
    }

    /** Copies the object onto itself with the Glacier Instant Retrieval class: still downloadable at once, cheaper. */
    @Override
    public void archive(String key) {
        try {
            // Already archived (a previous run copied it but could not mark the row): nothing to do
            String current = s3.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build()).storageClassAsString();
            if (StorageClass.GLACIER_IR.toString().equals(current)) return;
            s3.copyObject(CopyObjectRequest.builder()
                    .sourceBucket(bucket).sourceKey(key)
                    .destinationBucket(bucket).destinationKey(key)
                    .storageClass(StorageClass.GLACIER_IR)
                    .build());
        } catch (SdkException e) {
            throw new FileStorageException("Could not archive the file on S3.", e);
        }
    }
}
