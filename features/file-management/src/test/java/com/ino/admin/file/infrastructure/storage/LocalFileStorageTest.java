package com.ino.admin.file.infrastructure.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.nio.file.Files;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;

class LocalFileStorageTest {
    @TempDir Path tempDir;

    @Test void savesAndLoadsInsideConfiguredRoot() {
        var storage = new LocalFileStorage(tempDir);
        storage.save("safe-key", "hello".getBytes());
        assertThat(storage.load("safe-key")).isEqualTo("hello".getBytes());
        assertThat(Files.exists(tempDir.resolve("safe-key"))).isTrue();
    }

    @Test void rejectsPathTraversal() {
        var storage = new LocalFileStorage(tempDir);
        assertThatIllegalArgumentException().isThrownBy(() -> storage.save("../escape", new byte[] { 1 }));
    }
}
