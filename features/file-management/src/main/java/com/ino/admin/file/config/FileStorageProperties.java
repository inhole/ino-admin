package com.ino.admin.file.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties("app.file-storage")
public class FileStorageProperties {
    private String root = "./data/files";
    private DataSize maxSize = DataSize.ofMegabytes(10);
    public String getRoot() { return root; }
    public void setRoot(String root) { this.root = root; }
    public DataSize getMaxSize() { return maxSize; }
    public void setMaxSize(DataSize maxSize) { this.maxSize = maxSize; }
}
