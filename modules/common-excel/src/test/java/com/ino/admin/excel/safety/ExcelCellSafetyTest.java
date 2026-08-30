package com.ino.admin.excel.safety;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class ExcelCellSafetyTest {
    @Test
    void neutralizesFormulaLikeTextAfterLeadingWhitespace() {
        assertThat(ExcelCellSafety.safeText("=SUM(1,2)")).isEqualTo("'=SUM(1,2)");
        assertThat(ExcelCellSafety.safeText("  +cmd")).isEqualTo("'  +cmd");
        assertThat(ExcelCellSafety.safeText("normal")).isEqualTo("normal");
        assertThat(ExcelCellSafety.safeText(null)).isEmpty();
    }
}
