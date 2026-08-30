package com.ino.admin.file.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class LocalFileStorage implements FileStorage {
    private final Path root;
    public LocalFileStorage(Path root) {
        this.root = root.toAbsolutePath().normalize();
        try { Files.createDirectories(this.root); }
        catch (IOException exception) { throw new UncheckedIOException("파일 저장소를 생성할 수 없습니다.", exception); }
    }
    public void save(String key, byte[] content) {
        try { Files.write(resolve(key), content); }
        catch (IOException exception) { throw new UncheckedIOException("파일을 저장할 수 없습니다.", exception); }
    }
    public byte[] load(String key) {
        try { return Files.readAllBytes(resolve(key)); }
        catch (IOException exception) { throw new UncheckedIOException("파일을 읽을 수 없습니다.", exception); }
    }
    public void delete(String key) {
        try { Files.deleteIfExists(resolve(key)); }
        catch (IOException exception) { throw new UncheckedIOException("파일을 삭제할 수 없습니다.", exception); }
    }
    private Path resolve(String key) {
        var resolved = root.resolve(key).normalize();
        if (!resolved.startsWith(root)) throw new IllegalArgumentException("올바르지 않은 저장 경로입니다.");
        return resolved;
    }
}
