package com.ino.admin.excel;

import com.ino.admin.core.BusinessException;
import com.ino.admin.core.ErrorCode;
import com.ino.admin.identity.api.UserManagementUseCase;
import com.ino.admin.excel.safety.ExcelCellSafety;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
class UserExcelImporter {
    static final String[] HEADERS = { "Email", "Display Name", "Role", "Initial Password" };
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    private static final int MAX_ROWS = 1_000;
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private final UserManagementUseCase users;

    UserExcelImporter(UserManagementUseCase users) { this.users = users; }

    @Transactional
    ImportResult importUsers(MultipartFile file) {
        validateFile(file);
        var commands = parse(file);
        for (var command : commands) {
            try {
                users.create(command.command());
            } catch (BusinessException exception) {
                throw invalid(command.rowNumber() + "행: " + exception.getMessage());
            }
        }
        return new ImportResult(commands.size());
    }

    private List<RowCommand> parse(MultipartFile file) {
        try (var workbook = new XSSFWorkbook(file.getInputStream())) {
            if (workbook.getNumberOfSheets() == 0) throw invalid("Excel 시트가 없습니다.");
            var sheet = workbook.getSheetAt(0);
            validateHeaders(sheet.getRow(0));
            if (sheet.getLastRowNum() > MAX_ROWS) throw invalid("가져오기는 최대 1,000명까지 가능합니다.");
            var formatter = new DataFormatter(Locale.ROOT);
            var emails = new HashSet<String>();
            var commands = new ArrayList<RowCommand>();
            for (int index = 1; index <= sheet.getLastRowNum(); index++) {
                var row = sheet.getRow(index);
                if (row == null || isBlank(row, formatter)) continue;
                var rowNumber = index + 1;
                try { ExcelCellSafety.rejectFormulas(row, HEADERS.length); }
                catch (IllegalArgumentException exception) { throw invalid(rowNumber + "행: 수식은 입력할 수 없습니다."); }
                var email = value(row.getCell(0), formatter).strip().toLowerCase(Locale.ROOT);
                var displayName = value(row.getCell(1), formatter).strip();
                var role = value(row.getCell(2), formatter).strip();
                var password = value(row.getCell(3), formatter);
                if (!EMAIL.matcher(email).matches()) throw invalid(rowNumber + "행: 이메일 형식이 올바르지 않습니다.");
                if (displayName.isBlank()) throw invalid(rowNumber + "행: 이름을 입력해야 합니다.");
                if (role.isBlank()) throw invalid(rowNumber + "행: 역할을 입력해야 합니다.");
                if (password.isBlank()) throw invalid(rowNumber + "행: 초기 비밀번호를 입력해야 합니다.");
                if (!emails.add(email)) throw invalid(rowNumber + "행: 파일 안에 중복된 이메일이 있습니다.");
                commands.add(new RowCommand(rowNumber, new UserManagementUseCase.CreateUser(email, password, displayName, role)));
            }
            if (commands.isEmpty()) throw invalid("가져올 사용자가 없습니다.");
            return List.copyOf(commands);
        } catch (BusinessException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw invalid("올바른 XLSX 파일을 선택해야 합니다.");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) throw invalid("Excel 파일을 선택해야 합니다.");
        if (file.getSize() > MAX_FILE_SIZE) throw invalid("Excel 파일은 5MB 이하여야 합니다.");
        var name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase(Locale.ROOT).endsWith(".xlsx")) throw invalid("XLSX 파일만 가져올 수 있습니다.");
    }

    private void validateHeaders(org.apache.poi.ss.usermodel.Row row) {
        var formatter = new DataFormatter(Locale.ROOT);
        if (row == null) throw invalid("Excel 헤더가 올바르지 않습니다.");
        for (int index = 0; index < HEADERS.length; index++) {
            if (!HEADERS[index].equals(value(row.getCell(index), formatter).strip()))
                throw invalid("Excel 헤더가 올바르지 않습니다. 양식을 다시 내려받아 주세요.");
        }
    }

    private boolean isBlank(org.apache.poi.ss.usermodel.Row row, DataFormatter formatter) {
        for (int column = 0; column < HEADERS.length; column++) if (!value(row.getCell(column), formatter).isBlank()) return false;
        return true;
    }

    private String value(Cell cell, DataFormatter formatter) { return cell == null ? "" : formatter.formatCellValue(cell); }
    private BusinessException invalid(String message) { return new BusinessException(ErrorCode.EXCEL_IMPORT_INVALID, message); }
    private record RowCommand(int rowNumber, UserManagementUseCase.CreateUser command) {}
    record ImportResult(int createdCount) {}
}
