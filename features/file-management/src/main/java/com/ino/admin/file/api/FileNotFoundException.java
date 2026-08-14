package com.ino.admin.file.api;

public class FileNotFoundException extends RuntimeException {
    public FileNotFoundException() { super("파일을 찾을 수 없습니다."); }
}
