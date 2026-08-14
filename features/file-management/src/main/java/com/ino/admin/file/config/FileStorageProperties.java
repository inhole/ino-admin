package com.ino.admin.file.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@ConfigurationProperties("app.file-storage")
public class FileStorageProperties {
    private String type = "local";
    private String root = "./data/files";
    private DataSize maxSize = DataSize.ofMegabytes(10);
    private final S3 s3 = new S3();
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getRoot() { return root; }
    public void setRoot(String root) { this.root = root; }
    public DataSize getMaxSize() { return maxSize; }
    public void setMaxSize(DataSize maxSize) { this.maxSize = maxSize; }
    public S3 getS3() { return s3; }

    public static class S3 {
        private String endpoint;
        private String region = "us-east-1";
        private String bucket = "ino-admin-files";
        private String accessKey;
        private String secretKey;
        private boolean pathStyleAccess = true;
        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
        public String getRegion() { return region; }
        public void setRegion(String region) { this.region = region; }
        public String getBucket() { return bucket; }
        public void setBucket(String bucket) { this.bucket = bucket; }
        public String getAccessKey() { return accessKey; }
        public void setAccessKey(String accessKey) { this.accessKey = accessKey; }
        public String getSecretKey() { return secretKey; }
        public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
        public boolean isPathStyleAccess() { return pathStyleAccess; }
        public void setPathStyleAccess(boolean pathStyleAccess) { this.pathStyleAccess = pathStyleAccess; }
    }
}
