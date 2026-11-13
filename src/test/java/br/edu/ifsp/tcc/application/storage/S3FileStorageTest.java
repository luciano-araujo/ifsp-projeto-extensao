package br.edu.ifsp.tcc.application.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.StorageClass;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** S3FileStorage against a mocked S3Client: AWS is never called. */
@ExtendWith(MockitoExtension.class)
class S3FileStorageTest {

    @Mock private S3Client s3;

    private S3FileStorage storage() {
        return new S3FileStorage(s3, "bucket-materiais");
    }

    @Test
    void store_shouldPutTheObjectWithKeyTypeAndLength() {
        byte[] bytes = "conteudo".getBytes(StandardCharsets.UTF_8);

        storage().store("chave-1", new ByteArrayInputStream(bytes), bytes.length, "application/pdf");

        ArgumentCaptor<PutObjectRequest> request = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3).putObject(request.capture(), any(RequestBody.class));
        assertEquals("bucket-materiais", request.getValue().bucket());
        assertEquals("chave-1", request.getValue().key());
        assertEquals("application/pdf", request.getValue().contentType());
        assertEquals(8L, request.getValue().contentLength());
    }

    @Test
    void store_shouldWrapSdkFailures() {
        when(s3.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(SdkClientException.create("sem rede"));

        assertThrows(FileStorageException.class,
                () -> storage().store("k", new ByteArrayInputStream(new byte[0]), 0, "text/plain"));
    }

    @Test
    void load_shouldStreamTheObjectWithItsLength() throws IOException {
        byte[] bytes = "abc".getBytes(StandardCharsets.UTF_8);
        ResponseInputStream<GetObjectResponse> object = new ResponseInputStream<>(
                GetObjectResponse.builder().contentLength(3L).build(),
                AbortableInputStream.create(new ByteArrayInputStream(bytes)));
        when(s3.headObject(any(HeadObjectRequest.class))).thenReturn(HeadObjectResponse.builder().contentLength(3L).build());
        when(s3.getObject(any(GetObjectRequest.class))).thenReturn(object);

        Resource resource = storage().load("chave-1");

        assertEquals(3L, resource.contentLength());
        assertTrue(resource.getDescription().contains("chave-1"));
        // Nothing is opened until the body is read (a HEAD request never reads it)
        verify(s3, never()).getObject(any(GetObjectRequest.class));
        assertEquals("abc", new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8));
        ArgumentCaptor<GetObjectRequest> request = ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(s3).getObject(request.capture());
        assertEquals("bucket-materiais", request.getValue().bucket());
        assertEquals("chave-1", request.getValue().key());
    }

    @Test
    void load_shouldReportAMissingKey() {
        when(s3.headObject(any(HeadObjectRequest.class))).thenThrow(NoSuchKeyException.builder().build());

        assertThrows(StorageFileNotFoundException.class, () -> storage().load("nao-existe"));
    }

    @Test
    void load_shouldWrapOtherSdkFailures() {
        when(s3.headObject(any(HeadObjectRequest.class)))
                .thenThrow(AwsServiceException.builder().message("negado").build());

        FileStorageException ex = assertThrows(FileStorageException.class, () -> storage().load("k"));
        assertFalse(ex instanceof StorageFileNotFoundException);
    }

    @Test
    void load_shouldWrapFailuresWhenTheBodyIsOpened() throws IOException {
        when(s3.headObject(any(HeadObjectRequest.class))).thenReturn(HeadObjectResponse.builder().contentLength(3L).build());
        when(s3.getObject(any(GetObjectRequest.class))).thenThrow(SdkClientException.create("sem rede"));

        Resource resource = storage().load("k");

        assertThrows(FileStorageException.class, resource::getInputStream);
    }

    @Test
    void delete_shouldDeleteTheObject() {
        storage().delete("chave-1");

        ArgumentCaptor<DeleteObjectRequest> request = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3).deleteObject(request.capture());
        assertEquals("bucket-materiais", request.getValue().bucket());
        assertEquals("chave-1", request.getValue().key());
    }

    @Test
    void delete_shouldWrapSdkFailures() {
        when(s3.deleteObject(any(DeleteObjectRequest.class))).thenThrow(SdkClientException.create("sem rede"));

        assertThrows(FileStorageException.class, () -> storage().delete("k"));
    }

    @Test
    void archive_shouldCopyTheObjectOntoItselfWithGlacierInstantRetrieval() {
        storage().archive("chave-1");

        ArgumentCaptor<CopyObjectRequest> request = ArgumentCaptor.forClass(CopyObjectRequest.class);
        verify(s3).copyObject(request.capture());
        CopyObjectRequest copy = request.getValue();
        assertEquals("bucket-materiais", copy.sourceBucket());
        assertEquals("chave-1", copy.sourceKey());
        assertEquals("bucket-materiais", copy.destinationBucket());
        assertEquals("chave-1", copy.destinationKey());
        assertEquals(StorageClass.GLACIER_IR, copy.storageClass());
    }

    @Test
    void archive_shouldWrapSdkFailures() {
        when(s3.copyObject(any(CopyObjectRequest.class))).thenThrow(SdkClientException.create("sem rede"));

        assertThrows(FileStorageException.class, () -> storage().archive("k"));
    }
}
