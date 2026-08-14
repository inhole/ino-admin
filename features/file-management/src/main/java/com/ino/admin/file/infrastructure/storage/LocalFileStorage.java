package com.ino.admin.file.infrastructure.storage;

import com.ino.admin.file.application.port.FileStorage;
import java.io.UncheckedIOException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class LocalFileStorage implements FileStorage {
    private final Path root;

    public LocalFileStorage(Path root) {
        this.root = root.toAbsolutePath().normalize();
        try { Files.createDirectories(this.root); }
        catch (IOException exception) { throw new UncheckedIOException("파일 저장소를 생성할 수 없습니다.", exception); }
    }

    @Override public void save(String storageKey, byte[] content) {
        try { Files.write(resolve(storageKey), content); }
        catch (IOException exception) { throw new UncheckedIOException("파일을 저장할 수 없습니다.", exception); }
    }

    @Override public byte[] load(String storageKey) {
        try { return Files.readAllBytes(resolve(storageKey)); }
        catch (IOException exception) { throw new UncheckedIOException("파일을 읽을 수 없습니다.", exception); }
    }

    @Override public void delete(String storageKey) {
        try { Files.deleteIfExists(resolve(storageKey)); }
        catch (IOException exception) { throw new UncheckedIOException("파일을 삭제할 수 없습니다.", exception); }
    }

    private Path resolve(String storageKey) {
        var resolved = root.resolve(storageKey).normalize();
        if (!resolved.startsWith(root)) throw new IllegalArgumentException("올바르지 않은 저장 경로입니다.");
        return resolved;
    }
}
