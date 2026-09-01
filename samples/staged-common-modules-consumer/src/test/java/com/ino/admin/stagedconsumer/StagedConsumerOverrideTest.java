package com.ino.admin.stagedconsumer;

import static org.assertj.core.api.Assertions.assertThat;

import com.ino.admin.file.storage.FileStorage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@SpringBootTest(
        classes = {StagedConsumerApplication.class, StagedConsumerOverrideTest.OverrideConfiguration.class},
        properties = "app.jwt.secret=Y29tbW9uLW1vZHVsZXMtY29uc3VtZXItZml4dHVyZS1zZWNyZXQ=")
class StagedConsumerOverrideTest {
    @Autowired FileStorage fileStorage;

    @Test
    void backsOffWhenTheConsumerProvidesAFileStorageBean() {
        assertThat(fileStorage).isInstanceOf(InMemoryFileStorage.class);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class OverrideConfiguration {
        @Bean
        FileStorage consumerFileStorage() {
            return new InMemoryFileStorage();
        }
    }

    static final class InMemoryFileStorage implements FileStorage {
        private byte[] content = new byte[0];

        @Override public void save(String storageKey, byte[] content) { this.content = content.clone(); }
        @Override public byte[] load(String storageKey) { return content.clone(); }
        @Override public void delete(String storageKey) { content = new byte[0]; }
    }
}
