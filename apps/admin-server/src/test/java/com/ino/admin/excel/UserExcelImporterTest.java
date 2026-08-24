package com.ino.admin.excel;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.ino.admin.core.BusinessException;
import com.ino.admin.identity.api.UserManagementUseCase;
import java.io.ByteArrayOutputStream;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class UserExcelImporterTest {
    @Mock UserManagementUseCase users;

    @Test
    void importsValidRows() throws Exception {
        var importer = new UserExcelImporter(users);
        var result = importer.importUsers(file(row("viewer@example.com", "조회자", "VIEWER", "Valid-Password-2026!")));

        assertThat(result.createdCount()).isEqualTo(1);
        verify(users).create(new UserManagementUseCase.CreateUser(
                "viewer@example.com", "Valid-Password-2026!", "조회자", "VIEWER"));
    }

    @Test
    void rejectsFormulaAndDoesNotCreateUsers() throws Exception {
        try (var workbook = workbook()) {
            var row = workbook.getSheetAt(0).createRow(1);
            row.createCell(0).setCellFormula("HYPERLINK(\"https://example.com\")");
            row.createCell(1).setCellValue("조회자");
            row.createCell(2).setCellValue("VIEWER");
            row.createCell(3).setCellValue("Valid-Password-2026!");

            assertThatThrownBy(() -> new UserExcelImporter(users).importUsers(file(workbook)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("2행");
            verifyNoInteractions(users);
        }
    }

    @Test
    void rejectsDuplicateEmailsBeforeCreatingUsers() throws Exception {
        var upload = file(
                row("viewer@example.com", "조회자", "VIEWER", "Valid-Password-2026!"),
                row("VIEWER@example.com", "다른 조회자", "VIEWER", "Valid-Password-2026!"));

        assertThatThrownBy(() -> new UserExcelImporter(users).importUsers(upload))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("3행");
        verifyNoInteractions(users);
    }

    private static String[] row(String... values) { return values; }

    private static MockMultipartFile file(String[]... rows) throws Exception {
        try (var workbook = workbook()) {
            for (int index = 0; index < rows.length; index++) {
                var row = workbook.getSheetAt(0).createRow(index + 1);
                for (int column = 0; column < rows[index].length; column++) row.createCell(column).setCellValue(rows[index][column]);
            }
            return file(workbook);
        }
    }

    private static XSSFWorkbook workbook() {
        var workbook = new XSSFWorkbook();
        var header = workbook.createSheet("Users Import").createRow(0);
        var labels = new String[] { "Email", "Display Name", "Role", "Initial Password" };
        for (int index = 0; index < labels.length; index++) header.createCell(index).setCellValue(labels[index]);
        return workbook;
    }

    private static MockMultipartFile file(XSSFWorkbook workbook) throws Exception {
        var output = new ByteArrayOutputStream();
        workbook.write(output);
        return new MockMultipartFile("file", "users-import.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", output.toByteArray());
    }
}
