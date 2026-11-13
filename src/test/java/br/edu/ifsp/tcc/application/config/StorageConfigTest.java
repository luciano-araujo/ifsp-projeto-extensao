package br.edu.ifsp.tcc.application.config;

import br.edu.ifsp.tcc.application.storage.FileStorage;
import br.edu.ifsp.tcc.application.storage.LocalFileStorage;
import br.edu.ifsp.tcc.application.storage.S3FileStorage;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.annotation.UserConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.*;

/** Which FileStorage the property app.storage.type selects. No AWS call is made (the client is only built). */
class StorageConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(UserConfigurations.of(StorageConfig.class));

    @Test
    void shouldUseLocalDiskByDefault() {
        runner.withPropertyValues("app.storage.local-dir=target/test-uploads").run(context -> {
            assertInstanceOf(LocalFileStorage.class, context.getBean(FileStorage.class));
        });
    }

    @Test
    void shouldUseLocalDiskWhenTypeIsLocal() {
        runner.withPropertyValues("app.storage.type=local").run(context -> {
            assertInstanceOf(LocalFileStorage.class, context.getBean(FileStorage.class));
        });
    }

    @Test
    void shouldUseS3WhenTypeIsS3() {
        runner.withPropertyValues("app.storage.type=s3", "app.storage.s3.bucket=meu-bucket",
                "app.storage.s3.region=sa-east-1").run(context -> {
            assertInstanceOf(S3FileStorage.class, context.getBean(FileStorage.class));
        });
    }

    @Test
    void shouldNotStartWithS3WithoutABucket() {
        runner.withPropertyValues("app.storage.type=s3").run(context -> {
            assertNotNull(context.getStartupFailure());
            assertTrue(context.getStartupFailure().getMessage().contains("FileStorage")
                    || rootCause(context.getStartupFailure()).getMessage().contains("app.storage.s3.bucket"));
        });
    }

    private Throwable rootCause(Throwable t) {
        while (t.getCause() != null) t = t.getCause();
        return t;
    }
}
