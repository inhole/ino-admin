package com.ino.admin.file.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.file-storage")
public class FileStorageProperties {
    private String type = "local";
    private String root = "./data/files";
    private final S3 s3 = new S3();
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getRoot() { return root; }
    public void setRoot(String root) { this.root = root; }
    public S3 getS3() { return s3; }
    public static class S3 {
        private String endpoint; private String region = "us-east-1"; private String bucket = "ino-admin-files";
        private String accessKey; private String secretKey; private boolean pathStyleAccess = true;
        public String getEndpoint() { return endpoint; } public void setEndpoint(String value) { endpoint = value; }
        public String getRegion() { return region; } public void setRegion(String value) { region = value; }
        public String getBucket() { return bucket; } public void setBucket(String value) { bucket = value; }
        public String getAccessKey() { return accessKey; } public void setAccessKey(String value) { accessKey = value; }
        public String getSecretKey() { return secretKey; } public void setSecretKey(String value) { secretKey = value; }
        public boolean isPathStyleAccess() { return pathStyleAccess; } public void setPathStyleAccess(boolean value) { pathStyleAccess = value; }
    }
}
