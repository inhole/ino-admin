package com.ino.admin.file.storage;

public interface FileStorage {
    void save(String storageKey, byte[] content);
    byte[] load(String storageKey);
    void delete(String storageKey);
}
