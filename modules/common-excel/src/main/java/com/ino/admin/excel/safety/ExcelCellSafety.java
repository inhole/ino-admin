package com.ino.admin.excel.safety;

import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;

public final class ExcelCellSafety {
    private ExcelCellSafety() {}

    public static String safeText(String value) {
        if (value == null || value.isEmpty()) return "";
        var inspected = value.stripLeading();
        return !inspected.isEmpty() && "=+-@".indexOf(inspected.charAt(0)) >= 0 ? "'" + value : value;
    }

    public static void rejectFormulas(Row row, int columnCount) {
        for (int column = 0; column < columnCount; column++) {
            var cell = row.getCell(column);
            if (cell != null && cell.getCellType() == CellType.FORMULA) {
                throw new IllegalArgumentException("수식 셀은 입력할 수 없습니다.");
            }
        }
    }
}
