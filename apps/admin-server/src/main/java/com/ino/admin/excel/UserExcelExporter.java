package com.ino.admin.excel;

import com.ino.admin.core.BusinessException;
import com.ino.admin.core.ErrorCode;
import com.ino.admin.identity.api.UserDirectoryUseCase;
import com.ino.admin.identity.api.UserDirectoryUseCase.SortDirection;
import com.ino.admin.identity.api.UserDirectoryUseCase.UserQuery;
import com.ino.admin.identity.api.UserDirectoryUseCase.UserSort;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Service;

@Service
class UserExcelExporter {
    private static final int PAGE_SIZE = 500;
    private static final int MAX_ROWS = 10_000;
    private final UserDirectoryUseCase users;

    UserExcelExporter(UserDirectoryUseCase users) { this.users = users; }

    byte[] export() {
        try (var workbook = new SXSSFWorkbook(100); var output = new ByteArrayOutputStream()) {
            workbook.setCompressTempFiles(true);
            var sheet = workbook.createSheet("Users");
            var headerStyle = workbook.createCellStyle();
            var headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
            var dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(workbook.createDataFormat().getFormat("yyyy-mm-dd hh:mm:ss"));
            var header = sheet.createRow(0);
            var labels = new String[] { "ID", "Email", "Display Name", "Status", "Role", "Created At (UTC)" };
            for (int index = 0; index < labels.length; index++) {
                var cell = header.createCell(index); cell.setCellValue(labels[index]); cell.setCellStyle(headerStyle);
            }

            var page = 0;
            var rowIndex = 1;
            int totalPages;
            do {
                var result = users.findUsers(new UserQuery("", null, null, page, PAGE_SIZE,
                        UserSort.CREATED_AT, SortDirection.ASC));
                if (result.totalElements() > MAX_ROWS) {
                    throw new BusinessException(ErrorCode.EXCEL_ROW_LIMIT);
                }
                for (var user : result.content()) {
                    var row = sheet.createRow(rowIndex++);
                    row.createCell(0).setCellValue(user.id().toString());
                    row.createCell(1).setCellValue(safeText(user.email()));
                    row.createCell(2).setCellValue(safeText(user.displayName()));
                    row.createCell(3).setCellValue(user.status());
                    row.createCell(4).setCellValue(user.role());
                    var createdAt = row.createCell(5);
                    createdAt.setCellValue(LocalDateTime.ofInstant(user.createdAt(), ZoneOffset.UTC));
                    createdAt.setCellStyle(dateStyle);
                }
                totalPages = result.totalPages();
                page++;
            } while (page < totalPages);

            sheet.trackAllColumnsForAutoSizing();
            for (int column = 0; column < labels.length; column++) {
                sheet.autoSizeColumn(column);
                sheet.setColumnWidth(column, Math.min(sheet.getColumnWidth(column) + 512, 12_000));
            }
            sheet.createFreezePane(0, 1);
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.EXCEL_EXPORT_FAILED);
        }
    }

    private String safeText(String value) {
        if (value == null || value.isEmpty()) return "";
        var inspected = value.stripLeading();
        if (!inspected.isEmpty() && "=+-@".indexOf(inspected.charAt(0)) >= 0) return "'" + value;
        return value;
    }
}
