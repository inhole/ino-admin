package com.ino.admin.excel.safety;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class ExcelCellSafetyTest {
    @Test
    void neutralizesFormulaLikeTextAfterLeadingWhitespace() {
        assertThat(ExcelCellSafety.safeText("=SUM(1,2)")).isEqualTo("'=SUM(1,2)");
        assertThat(ExcelCellSafety.safeText("  +cmd")).isEqualTo("'  +cmd");
        assertThat(ExcelCellSafety.safeText("normal")).isEqualTo("normal");
        assertThat(ExcelCellSafety.safeText(null)).isEmpty();
    }

    @Test
    void rejectsFormulaCellsWithinTheDeclaredColumns() throws Exception {
        try (var workbook = new XSSFWorkbook()) {
            var row = workbook.createSheet().createRow(0);
            row.createCell(0).setCellValue("safe");
            row.createCell(1).setCellFormula("SUM(1,2)");
            assertThatIllegalArgumentException().isThrownBy(() -> ExcelCellSafety.rejectFormulas(row, 2));
        }
    }
}
