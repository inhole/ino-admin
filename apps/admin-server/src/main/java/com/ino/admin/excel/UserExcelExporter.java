package com.ino.admin.excel;

import com.ino.admin.core.BusinessException;
import com.ino.admin.core.ErrorCode;
import com.ino.admin.identity.api.UserDirectoryUseCase;
import com.ino.admin.identity.api.UserDirectoryUseCase.SortDirection;
import com.ino.admin.identity.api.UserDirectoryUseCase.UserQuery;
import com.ino.admin.identity.api.UserDirectoryUseCase.UserSort;
import com.ino.admin.excel.io.XlsxCell;
import com.ino.admin.excel.io.XlsxTableWriter;
import com.ino.admin.excel.io.XlsxWriteException;
import com.ino.admin.excel.io.XlsxWriteOptions;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
class UserExcelExporter {
    private static final int PAGE_SIZE = 500;
    private static final int MAX_ROWS = 10_000;
    private final UserDirectoryUseCase users;

    UserExcelExporter(UserDirectoryUseCase users) { this.users = users; }

    byte[] export() {
        try {
            return new XlsxTableWriter().write(new XlsxWriteOptions("Users",
                    List.of("ID", "Email", "Display Name", "Status", "Role", "Created At (UTC)")), rows -> {
                var page = 0;
                int totalPages;
                do {
                    var result = users.findUsers(new UserQuery("", null, null, page, PAGE_SIZE,
                            UserSort.CREATED_AT, SortDirection.ASC));
                    if (result.totalElements() > MAX_ROWS) throw new BusinessException(ErrorCode.EXCEL_ROW_LIMIT);
                    for (var user : result.content()) {
                        rows.append(List.of(
                                XlsxCell.text(user.id().toString()),
                                XlsxCell.text(user.email()),
                                XlsxCell.text(user.displayName()),
                                XlsxCell.text(user.status()),
                                XlsxCell.text(user.role()),
                                XlsxCell.dateTime(LocalDateTime.ofInstant(user.createdAt(), ZoneOffset.UTC),
                                        "yyyy-mm-dd hh:mm:ss")));
                    }
                    totalPages = result.totalPages();
                    page++;
                } while (page < totalPages);
            });
        } catch (XlsxWriteException exception) {
            throw new BusinessException(ErrorCode.EXCEL_EXPORT_FAILED);
        }
    }

}
