package com.ino.admin.excel;

import static org.assertj.core.api.Assertions.assertThat;

import com.ino.admin.excel.io.XlsxCell;
import com.ino.admin.excel.io.XlsxReadException;
import com.ino.admin.excel.io.XlsxReadOptions;
import com.ino.admin.excel.io.XlsxRow;
import com.ino.admin.excel.io.XlsxRowSink;
import com.ino.admin.excel.io.XlsxTableReader;
import com.ino.admin.excel.io.XlsxTableWriter;
import com.ino.admin.excel.io.XlsxWriteException;
import com.ino.admin.excel.io.XlsxWriteOptions;
import com.ino.admin.excel.safety.ExcelCellSafety;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class PublicApiBoundaryTest {
    @Test
    void publicApiDoesNotExposeApachePoiTypes() {
        var publicTypes = List.of(ExcelCellSafety.class, XlsxCell.class, XlsxReadException.class,
                XlsxReadOptions.class, XlsxRow.class, XlsxRowSink.class, XlsxTableReader.class,
                XlsxTableWriter.class, XlsxWriteException.class, XlsxWriteOptions.class);

        var exposedTypes = publicTypes.stream()
                .flatMap(type -> Stream.of(type.getMethods()))
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .flatMap(method -> Stream.concat(Stream.of(method.getReturnType()), Stream.of(method.getParameterTypes())))
                .map(Class::getName)
                .filter(name -> name.startsWith("org.apache.poi."))
                .toList();

        assertThat(exposedTypes).isEmpty();
    }
}
