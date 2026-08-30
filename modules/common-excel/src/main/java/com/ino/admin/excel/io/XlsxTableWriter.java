package com.ino.admin.excel.io;

import com.ino.admin.excel.safety.ExcelCellSafety;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

public final class XlsxTableWriter {
    public byte[] write(XlsxWriteOptions options, Consumer<XlsxRowSink> rowWriter) {
        try (var workbook = new SXSSFWorkbook(100); var output = new ByteArrayOutputStream()) {
            workbook.setCompressTempFiles(true);
            var sheet = workbook.createSheet(options.sheetName());
            var headerStyle = workbook.createCellStyle();
            var headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);

            var header = sheet.createRow(0);
            for (int index = 0; index < options.headers().size(); index++) {
                var cell = header.createCell(index);
                cell.setCellValue(options.headers().get(index));
                cell.setCellStyle(headerStyle);
            }

            var nextRow = new int[] { 1 };
            var dateStyles = new HashMap<String, org.apache.poi.ss.usermodel.CellStyle>();
            rowWriter.accept(cells -> {
                if (cells.size() != options.headers().size()) {
                    throw new IllegalArgumentException("row cell count must match headers");
                }
                var row = sheet.createRow(nextRow[0]++);
                for (int index = 0; index < cells.size(); index++) {
                    var target = row.createCell(index);
                    switch (cells.get(index)) {
                        case XlsxCell.Text text -> target.setCellValue(ExcelCellSafety.safeText(text.value()));
                        case XlsxCell.DateTime dateTime -> {
                            target.setCellValue(dateTime.value());
                            var style = dateStyles.computeIfAbsent(dateTime.format(), format -> {
                                var created = workbook.createCellStyle();
                                created.setDataFormat(workbook.createDataFormat().getFormat(format));
                                return created;
                            });
                            target.setCellStyle(style);
                        }
                    }
                }
            });

            sheet.trackAllColumnsForAutoSizing();
            for (int column = 0; column < options.headers().size(); column++) {
                sheet.autoSizeColumn(column);
                sheet.setColumnWidth(column, Math.min(sheet.getColumnWidth(column) + 512, 12_000));
            }
            sheet.createFreezePane(0, 1);
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new XlsxWriteException(exception);
        }
    }
}
