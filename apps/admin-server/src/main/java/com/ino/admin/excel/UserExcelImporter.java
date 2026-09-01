package com.ino.admin.excel;

import com.ino.spring.modules.core.BusinessException;
import com.ino.admin.error.ErrorCode;
import com.ino.admin.identity.api.UserManagementUseCase;
import com.ino.spring.modules.excel.io.XlsxReadException;
import com.ino.spring.modules.excel.io.XlsxReadOptions;
import com.ino.spring.modules.excel.io.XlsxTableReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
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
    private final XlsxTableReader reader = new XlsxTableReader();

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
        try (var input = file.getInputStream()) {
            var rows = reader.read(input, new XlsxReadOptions(List.of(HEADERS), MAX_ROWS));
            var emails = new HashSet<String>();
            var commands = new ArrayList<RowCommand>();
            for (var row : rows) {
                var email = row.value(0).strip().toLowerCase(Locale.ROOT);
                var displayName = row.value(1).strip();
                var role = row.value(2).strip();
                var password = row.value(3);
                if (!EMAIL.matcher(email).matches()) throw invalid(row.rowNumber() + "행: 이메일 형식이 올바르지 않습니다.");
                if (displayName.isBlank()) throw invalid(row.rowNumber() + "행: 이름을 입력해야 합니다.");
                if (role.isBlank()) throw invalid(row.rowNumber() + "행: 역할을 입력해야 합니다.");
                if (password.isBlank()) throw invalid(row.rowNumber() + "행: 초기 비밀번호를 입력해야 합니다.");
                if (!emails.add(email)) throw invalid(row.rowNumber() + "행: 파일 안에 중복된 이메일이 있습니다.");
                commands.add(new RowCommand(row.rowNumber(), new UserManagementUseCase.CreateUser(email, password, displayName, role)));
            }
            if (commands.isEmpty()) throw invalid("가져올 사용자가 없습니다.");
            return List.copyOf(commands);
        } catch (XlsxReadException exception) {
            throw switch (exception.reason()) {
                case MISSING_SHEET -> invalid("Excel 시트가 없습니다.");
                case HEADER_MISMATCH -> invalid("Excel 헤더가 올바르지 않습니다. 양식을 다시 내려받아 주세요.");
                case ROW_LIMIT_EXCEEDED -> invalid("가져오기는 최대 1,000명까지 가능합니다.");
                case FORMULA_CELL -> invalid(exception.rowNumber() + "행: 수식은 입력할 수 없습니다.");
                case INVALID_WORKBOOK -> invalid("올바른 XLSX 파일을 선택해야 합니다.");
            };
        } catch (BusinessException exception) {
            throw exception;
        } catch (IOException exception) {
            throw invalid("올바른 XLSX 파일을 선택해야 합니다.");
        }
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) throw invalid("Excel 파일을 선택해야 합니다.");
        if (file.getSize() > MAX_FILE_SIZE) throw invalid("Excel 파일은 5MB 이하여야 합니다.");
        var name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase(Locale.ROOT).endsWith(".xlsx")) throw invalid("XLSX 파일만 가져올 수 있습니다.");
    }

    private BusinessException invalid(String message) { return new BusinessException(ErrorCode.EXCEL_IMPORT_INVALID, message); }
    private record RowCommand(int rowNumber, UserManagementUseCase.CreateUser command) {}
    record ImportResult(int createdCount) {}
}
