package br.edu.ifsp.tcc.application.storage;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
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
            ResponseInputStream<GetObjectResponse> object = s3.getObject(
                    GetObjectRequest.builder().bucket(bucket).key(key).build());
            long length = object.response().contentLength();
            return new InputStreamResource(object) {
                @Override
                public long contentLength() {
                    return length;
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
