package com.ino.admin.excel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ino.admin.identity.api.UserDirectoryUseCase;
import com.ino.admin.identity.api.UserDirectoryUseCase.UserPage;
import com.ino.admin.identity.api.UserDirectoryUseCase.UserSummary;
import java.io.ByteArrayInputStream;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class UserExcelExporterTest {
    @Test
    void exportsTypedUserRowsAndNeutralizesFormulaLikeText() throws Exception {
        var users = Mockito.mock(UserDirectoryUseCase.class);
        when(users.findUsers(any())).thenReturn(new UserPage(List.of(
                new UserSummary(UUID.randomUUID(), "admin@example.com", "=HYPERLINK(\"bad\")",
                        "ACTIVE", "SUPER_ADMIN", Instant.parse("2026-08-24T00:00:00Z"))),
                0, 500, 1, 1));

        var bytes = new UserExcelExporter(users).export();

        try (var workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            var sheet = workbook.getSheet("Users");
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("ID");
            assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).isEqualTo("admin@example.com");
            assertThat(sheet.getRow(1).getCell(2).getCellType()).isEqualTo(CellType.STRING);
            assertThat(sheet.getRow(1).getCell(2).getStringCellValue()).startsWith("'");
            assertThat(sheet.getRow(1).getCell(5).getCellStyle().getDataFormatString()).isEqualTo("yyyy-mm-dd hh:mm:ss");
        }
    }

    @Test
    void readsAllPagesInStableOrder() {
        var users = Mockito.mock(UserDirectoryUseCase.class);
        when(users.findUsers(any())).thenAnswer(invocation -> {
            var query = invocation.getArgument(0, UserDirectoryUseCase.UserQuery.class);
            var summary = new UserSummary(UUID.randomUUID(), "user" + query.page() + "@example.com",
                    "User", "ACTIVE", "ADMIN", Instant.EPOCH);
            return new UserPage(List.of(summary), query.page(), query.size(), 2, 2);
        });

        var bytes = new UserExcelExporter(users).export();

        assertThat(bytes).isNotEmpty();
        Mockito.verify(users, Mockito.times(2)).findUsers(any());
    }
}
