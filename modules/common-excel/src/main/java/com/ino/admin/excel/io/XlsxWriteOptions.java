package com.ino.admin.excel.io;

import java.util.List;

public record XlsxWriteOptions(String sheetName, List<String> headers) {
    public XlsxWriteOptions {
        if (sheetName == null || sheetName.isBlank()) throw new IllegalArgumentException("sheetName must not be blank");
        headers = List.copyOf(headers);
        if (headers.isEmpty()) throw new IllegalArgumentException("headers must not be empty");
    }
}
