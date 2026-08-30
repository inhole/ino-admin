package com.ino.admin.excel.io;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class XlsxTableReaderTest {
    private final XlsxTableReader reader = new XlsxTableReader();

    @Test
    void readsRowsWithoutExposingPoiTypes() throws Exception {
        var bytes = workbook(List.of("Email", "Role"), row -> {
            row.createCell(0).setCellValue("admin@example.com");
            row.createCell(1).setCellValue("ADMIN");
        });

        var rows = reader.read(new ByteArrayInputStream(bytes),
                new XlsxReadOptions(List.of("Email", "Role"), 10));

        assertThat(rows).containsExactly(new XlsxRow(2, List.of("admin@example.com", "ADMIN")));
    }

    @Test
    void rejectsFormulaCellsWithTheirSpreadsheetRowNumber() throws Exception {
        var bytes = workbook(List.of("Email", "Role"), row -> {
            row.createCell(0).setCellFormula("HYPERLINK(\"https://example.com\")");
            row.createCell(1).setCellValue("ADMIN");
        });

        assertThatThrownBy(() -> reader.read(new ByteArrayInputStream(bytes),
                new XlsxReadOptions(List.of("Email", "Role"), 10)))
                .isInstanceOfSatisfying(XlsxReadException.class, exception -> {
                    assertThat(exception.reason()).isEqualTo(XlsxReadException.Reason.FORMULA_CELL);
                    assertThat(exception.rowNumber()).isEqualTo(2);
                });
    }

    @Test
    void distinguishesHeaderAndRowLimitFailures() throws Exception {
        var bytes = workbook(List.of("Wrong"), row -> row.createCell(0).setCellValue("value"));

        assertThatThrownBy(() -> reader.read(new ByteArrayInputStream(bytes),
                new XlsxReadOptions(List.of("Expected"), 10)))
                .isInstanceOfSatisfying(XlsxReadException.class,
                        exception -> assertThat(exception.reason()).isEqualTo(XlsxReadException.Reason.HEADER_MISMATCH));

        var limited = workbook(List.of("Value"), row -> row.createCell(0).setCellValue("one"));
        assertThatThrownBy(() -> reader.read(new ByteArrayInputStream(limited),
                new XlsxReadOptions(List.of("Value"), 0)))
                .isInstanceOfSatisfying(XlsxReadException.class,
                        exception -> assertThat(exception.reason()).isEqualTo(XlsxReadException.Reason.ROW_LIMIT_EXCEEDED));
    }

    private static byte[] workbook(List<String> headers, java.util.function.Consumer<org.apache.poi.ss.usermodel.Row> rowWriter)
            throws Exception {
        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Sheet");
            var header = sheet.createRow(0);
            for (int index = 0; index < headers.size(); index++) header.createCell(index).setCellValue(headers.get(index));
            rowWriter.accept(sheet.createRow(1));
            workbook.write(output);
            return output.toByteArray();
        }
    }
}
