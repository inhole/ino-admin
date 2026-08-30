package com.ino.admin.excel.io;

import java.util.List;

public record XlsxReadOptions(List<String> headers, int maxRows) {
    public XlsxReadOptions {
        headers = List.copyOf(headers);
        if (headers.isEmpty()) throw new IllegalArgumentException("headers must not be empty");
        if (maxRows < 0) throw new IllegalArgumentException("maxRows must not be negative");
    }
}
