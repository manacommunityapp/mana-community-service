package com.manacommunity.api.unit.storage;

import com.manacommunity.api.storage.FileStorageService;
import com.manacommunity.api.storage.impl.PostgresFileStorageService;
import com.manacommunity.api.storage.impl.S3FileStorageService;
import com.manacommunity.api.storage.repository.StoredFileRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DisplayName("FileStorageService - Conditional Bean Activation")
class FileStorageServiceBeanTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(StoredFileRepository.class, () -> mock(StoredFileRepository.class))
            .withUserConfiguration(PostgresFileStorageService.class, S3FileStorageService.class);

    @Test
    @DisplayName("Defaults to PostgresFileStorageService when app.storage.type property is absent")
    void defaultsToPostgresWhenPropertyMissing() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(FileStorageService.class);
            assertThat(context).getBean(FileStorageService.class).isInstanceOf(PostgresFileStorageService.class);
        });
    }

    @Test
    @DisplayName("Loads PostgresFileStorageService when app.storage.type=postgres")
    void loadsPostgresWhenExplicitlyConfigured() {
        contextRunner
                .withPropertyValues("app.storage.type=postgres")
                .run(context -> {
                    assertThat(context).hasSingleBean(FileStorageService.class);
                    assertThat(context).getBean(FileStorageService.class).isInstanceOf(PostgresFileStorageService.class);
                });
    }

    @Test
    @DisplayName("Loads S3FileStorageService when app.storage.type=s3")
    void loadsS3WhenExplicitlyConfigured() {
        contextRunner
                .withPropertyValues(
                        "app.storage.type=s3",
                        "app.storage.s3.bucket=test-bucket",
                        "app.storage.s3.region=eu-north-1",
                        "app.storage.s3.access-key=test-access",
                        "app.storage.s3.secret-key=test-secret"
                )
                .run(context -> {
                    assertThat(context).hasSingleBean(FileStorageService.class);
                    assertThat(context).getBean(FileStorageService.class).isInstanceOf(S3FileStorageService.class);
                });
    }
}
