package com.ino.admin.file.api;

import com.ino.admin.core.BusinessException;

public class InvalidFileException extends BusinessException {
    public InvalidFileException(String message) { super("INVALID_FILE", message); }
}
