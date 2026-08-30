package com.ino.admin.excel.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public final class XlsxTableReader {
    public List<XlsxRow> read(InputStream input, XlsxReadOptions options) {
        try (var workbook = new XSSFWorkbook(input)) {
            if (workbook.getNumberOfSheets() == 0) throw new XlsxReadException(XlsxReadException.Reason.MISSING_SHEET);
            var sheet = workbook.getSheetAt(0);
            var formatter = new DataFormatter(Locale.ROOT);
            validateHeaders(sheet.getRow(0), formatter, options.headers());

            var rows = new ArrayList<XlsxRow>();
            for (int index = 1; index <= sheet.getLastRowNum(); index++) {
                var row = sheet.getRow(index);
                if (row == null || isBlank(row, formatter, options.headers().size())) continue;
                if (rows.size() >= options.maxRows()) {
                    throw new XlsxReadException(XlsxReadException.Reason.ROW_LIMIT_EXCEEDED);
                }
                var values = new ArrayList<String>(options.headers().size());
                for (int column = 0; column < options.headers().size(); column++) {
                    var cell = row.getCell(column);
                    if (cell != null && cell.getCellType() == CellType.FORMULA) {
                        throw new XlsxReadException(XlsxReadException.Reason.FORMULA_CELL, index + 1);
                    }
                    values.add(cell == null ? "" : formatter.formatCellValue(cell));
                }
                rows.add(new XlsxRow(index + 1, values));
            }
            return List.copyOf(rows);
        } catch (XlsxReadException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw new XlsxReadException(XlsxReadException.Reason.INVALID_WORKBOOK, exception);
        }
    }

    private void validateHeaders(org.apache.poi.ss.usermodel.Row row, DataFormatter formatter, List<String> headers) {
        if (row == null) throw new XlsxReadException(XlsxReadException.Reason.HEADER_MISMATCH);
        for (int index = 0; index < headers.size(); index++) {
            var cell = row.getCell(index);
            var actual = cell == null ? "" : formatter.formatCellValue(cell).strip();
            if (!headers.get(index).equals(actual)) throw new XlsxReadException(XlsxReadException.Reason.HEADER_MISMATCH);
        }
    }

    private boolean isBlank(org.apache.poi.ss.usermodel.Row row, DataFormatter formatter, int columnCount) {
        for (int column = 0; column < columnCount; column++) {
            var cell = row.getCell(column);
            if (cell != null && !formatter.formatCellValue(cell).isBlank()) return false;
        }
        return true;
    }
}
