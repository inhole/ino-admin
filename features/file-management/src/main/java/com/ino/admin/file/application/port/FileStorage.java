package com.ino.admin.file.application.port;

public interface FileStorage {
    void save(String storageKey, byte[] content);
    byte[] load(String storageKey);
    void delete(String storageKey);
}
