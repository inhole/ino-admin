package com.ino.admin.excel;

import com.ino.admin.core.BusinessException;
import com.ino.admin.core.ErrorCode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

@Service
class UserExcelTemplate {
    byte[] create() {
        try (var workbook = new XSSFWorkbook(); var output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("Users Import");
            var style = workbook.createCellStyle();
            var font = workbook.createFont();
            font.setBold(true); font.setColor(IndexedColors.WHITE.getIndex());
            style.setFont(font); style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            style.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
            var header = sheet.createRow(0);
            for (int index = 0; index < UserExcelImporter.HEADERS.length; index++) {
                var cell = header.createCell(index); cell.setCellValue(UserExcelImporter.HEADERS[index]); cell.setCellStyle(style);
                sheet.setColumnWidth(index, index == 3 ? 7_000 : 5_000);
            }
            sheet.createFreezePane(0, 1); workbook.write(output); return output.toByteArray();
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.EXCEL_TEMPLATE_FAILED);
        }
    }
}
