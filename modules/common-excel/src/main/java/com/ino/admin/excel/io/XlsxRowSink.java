package com.ino.admin.excel.io;

import java.util.List;

@FunctionalInterface
public interface XlsxRowSink {
    void append(List<XlsxCell> cells);
}
