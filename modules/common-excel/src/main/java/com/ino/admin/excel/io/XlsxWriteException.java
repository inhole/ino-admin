package com.ino.admin.excel.io;

public final class XlsxWriteException extends RuntimeException {
    public XlsxWriteException(Throwable cause) { super("XLSX_WRITE_FAILED", cause); }
}
