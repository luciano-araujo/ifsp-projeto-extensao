package br.edu.ifsp.tcc.application.config;

import br.edu.ifsp.tcc.application.storage.FileStorage;
import br.edu.ifsp.tcc.application.storage.LocalFileStorage;
import br.edu.ifsp.tcc.application.storage.S3FileStorage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.nio.file.Path;

/** Chooses the FileStorage by {@code app.storage.type}: local (default) or s3. */
@Configuration
public class StorageConfig {

    @Bean
    @ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
    public FileStorage localFileStorage(@Value("${app.storage.local-dir:./data/uploads}") String directory) {
        return new LocalFileStorage(Path.of(directory));
    }

    @Bean
    @ConditionalOnProperty(name = "app.storage.type", havingValue = "s3")
    public FileStorage s3FileStorage(@Value("${app.storage.s3.bucket:}") String bucket,
                                     @Value("${app.storage.s3.region:us-east-1}") String region) {
        if (bucket.isBlank()) {
            throw new IllegalStateException("app.storage.s3.bucket is required when app.storage.type=s3");
        }
        // Default credentials provider chain (environment, instance profile, ...): no key in code or config
        S3Client client = S3Client.builder().region(Region.of(region)).build();
        return new S3FileStorage(client, bucket);
    }
}
