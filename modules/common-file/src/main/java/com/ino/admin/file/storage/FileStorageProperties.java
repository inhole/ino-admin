package com.ino.admin.file.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.file-storage")
public class FileStorageProperties {
    private String type = "local";
    private String root = "./data/files";
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getRoot() { return root; }
    public void setRoot(String root) { this.root = root; }
}
