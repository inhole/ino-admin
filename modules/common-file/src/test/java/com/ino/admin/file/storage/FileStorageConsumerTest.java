package com.ino.admin.file.storage;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class FileStorageConsumerTest {
    @Test
    void independentConsumerGetsLocalStorageByDefault() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(FileStorageAutoConfiguration.class))
                .withPropertyValues("app.file-storage.root=build/test-storage")
                .run(context -> assertThat(context).hasSingleBean(FileStorage.class)
                        .getBean(FileStorage.class).isInstanceOf(LocalFileStorage.class));
    }

    @Test
    void preservesConsumerProvidedStorage() {
        FileStorage override = new FileStorage() {
            public void save(String key, byte[] content) {}
            public byte[] load(String key) { return new byte[0]; }
            public void delete(String key) {}
        };
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(FileStorageAutoConfiguration.class))
                .withBean(FileStorage.class, () -> override)
                .run(context -> assertThat(context.getBean(FileStorage.class)).isSameAs(override));
    }
}
