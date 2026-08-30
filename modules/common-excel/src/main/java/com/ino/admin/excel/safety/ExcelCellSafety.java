package com.ino.admin.excel.safety;

public final class ExcelCellSafety {
    private ExcelCellSafety() {}

    public static String safeText(String value) {
        if (value == null || value.isEmpty()) return "";
        var inspected = value.stripLeading();
        return !inspected.isEmpty() && "=+-@".indexOf(inspected.charAt(0)) >= 0 ? "'" + value : value;
    }
}
