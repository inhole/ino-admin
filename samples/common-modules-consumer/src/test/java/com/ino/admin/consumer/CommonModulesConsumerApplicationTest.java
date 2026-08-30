package com.ino.admin.consumer;

import static org.assertj.core.api.Assertions.assertThat;

import com.ino.admin.audit.AuditWriter;
import com.ino.admin.core.PageResponse;
import com.ino.admin.excel.io.XlsxCell;
import com.ino.admin.excel.io.XlsxReadOptions;
import com.ino.admin.excel.io.XlsxTableReader;
import com.ino.admin.excel.io.XlsxTableWriter;
import com.ino.admin.excel.io.XlsxWriteOptions;
import com.ino.admin.excel.safety.ExcelCellSafety;
import com.ino.admin.file.storage.FileStorage;
import com.ino.admin.security.jwt.JwtTokenService;
import com.ino.admin.web.ApiErrorFactory;
import com.ino.admin.web.TraceIdFilter;
import java.io.ByteArrayInputStream;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        classes = CommonModulesConsumerApplication.class,
        properties = {
            "app.jwt.secret=Y29tbW9uLW1vZHVsZXMtY29uc3VtZXItZml4dHVyZS1zZWNyZXQ=",
            "app.file-storage.root=build/consumer-files"
        })
class CommonModulesConsumerApplicationTest {
    @Autowired ApiErrorFactory apiErrorFactory;
    @Autowired TraceIdFilter traceIdFilter;
    @Autowired JwtTokenService jwtTokenService;
    @Autowired FileStorage fileStorage;
    @Autowired AuditWriter auditWriter;

    @Test
    void startsAsAnIndependentConsumerOfEveryPhaseEightModule() {
        assertThat(apiErrorFactory).isNotNull();
        assertThat(traceIdFilter).isNotNull();
        assertThat(jwtTokenService).isNotNull();
        assertThat(fileStorage).isNotNull();
        assertThat(auditWriter).isNotNull();
        assertThat(new PageResponse<>(List.of("ok"), 0, 1, 1, 1).content()).containsExactly("ok");
        assertThat(ExcelCellSafety.safeText("=1+1")).isEqualTo("'=1+1");
        var workbook = new XlsxTableWriter().write(new XlsxWriteOptions("Sample", List.of("Value")),
                rows -> rows.append(List.of(XlsxCell.text("ok"))));
        assertThat(new XlsxTableReader().read(new ByteArrayInputStream(workbook),
                new XlsxReadOptions(List.of("Value"), 1)).getFirst().value(0)).isEqualTo("ok");
    }
}
