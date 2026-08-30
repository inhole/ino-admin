package com.ino.admin.excel.io;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class XlsxTableWriterTest {
    @Test
    void writesTypedRowsAndNeutralizesFormulaLikeText() throws Exception {
        var bytes = new XlsxTableWriter().write(
                new XlsxWriteOptions("Users", List.of("Name", "Created At")),
                rows -> rows.append(List.of(
                        XlsxCell.text("=HYPERLINK(\"bad\")"),
                        XlsxCell.dateTime(LocalDateTime.of(2026, 8, 30, 12, 0), "yyyy-mm-dd hh:mm:ss"))));

        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            var sheet = workbook.getSheet("Users");
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Name");
            assertThat(sheet.getRow(1).getCell(0).getCellType()).isEqualTo(CellType.STRING);
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).startsWith("'");
            assertThat(sheet.getRow(1).getCell(1).getCellStyle().getDataFormatString())
                    .isEqualTo("yyyy-mm-dd hh:mm:ss");
            assertThat(sheet.getPaneInformation().isFreezePane()).isTrue();
        }
    }
}
