package com.ino.admin.excel.io;

import java.time.LocalDateTime;

public sealed interface XlsxCell permits XlsxCell.Text, XlsxCell.DateTime {
    static XlsxCell text(String value) { return new Text(value); }
    static XlsxCell dateTime(LocalDateTime value, String format) { return new DateTime(value, format); }

    record Text(String value) implements XlsxCell {}
    record DateTime(LocalDateTime value, String format) implements XlsxCell {}
}
