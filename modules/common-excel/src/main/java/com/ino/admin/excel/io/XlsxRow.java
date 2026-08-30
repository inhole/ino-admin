package com.ino.admin.excel.io;

import java.util.List;

public record XlsxRow(int rowNumber, List<String> values) {
    public XlsxRow {
        values = List.copyOf(values);
    }

    public String value(int column) {
        return values.get(column);
    }
}
