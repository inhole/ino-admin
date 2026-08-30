package com.ino.admin.file.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalFileStorageTest {
    @TempDir Path tempDir;

    @Test void savesLoadsAndDeletesInsideConfiguredRoot() {
        var storage = new LocalFileStorage(tempDir);
        storage.save("safe-key", "hello".getBytes());
        assertThat(storage.load("safe-key")).isEqualTo("hello".getBytes());
        storage.delete("safe-key");
        assertThat(Files.exists(tempDir.resolve("safe-key"))).isFalse();
    }

    @Test void rejectsPathTraversal() {
        var storage = new LocalFileStorage(tempDir);
        assertThatIllegalArgumentException().isThrownBy(() -> storage.save("../escape", new byte[] { 1 }));
    }
}
