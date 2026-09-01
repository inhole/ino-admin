package com.ino.admin.stagedconsumer;

import static org.assertj.core.api.Assertions.assertThat;

import com.ino.admin.audit.AuditWriter;
import com.ino.admin.core.PageResponse;
import com.ino.admin.excel.io.XlsxTableReader;
import com.ino.admin.file.storage.FileStorage;
import com.ino.admin.file.storage.LocalFileStorage;
import com.ino.admin.security.jwt.JwtSecurityProperties;
import com.ino.admin.security.jwt.JwtTokenService;
import com.ino.admin.web.ApiErrorFactory;
import com.ino.admin.web.TraceIdFilter;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(
        classes = StagedConsumerApplication.class,
        properties = {
            "app.jwt.secret=Y29tbW9uLW1vZHVsZXMtY29uc3VtZXItZml4dHVyZS1zZWNyZXQ=",
            "app.jwt.issuer=staged-consumer",
            "app.file-storage.root=build/staged-consumer-files"
        })
class StagedConsumerApplicationTest {
    @Autowired ApiErrorFactory apiErrorFactory;
    @Autowired TraceIdFilter traceIdFilter;
    @Autowired JwtTokenService jwtTokenService;
    @Autowired JwtSecurityProperties jwtProperties;
    @Autowired FileStorage fileStorage;
    @Autowired AuditWriter auditWriter;

    @Test
    void consumesEveryPublishedArtifactWithoutProjectDependencies() {
        assertThat(apiErrorFactory).isNotNull();
        assertThat(traceIdFilter).isNotNull();
        assertThat(jwtTokenService).isNotNull();
        assertThat(jwtProperties.getIssuer()).isEqualTo("staged-consumer");
        assertThat(fileStorage).isInstanceOf(LocalFileStorage.class);
        assertThat(auditWriter).isNotNull();
        assertThat(new PageResponse<>(List.of("ok"), 0, 1, 1, 1).content()).containsExactly("ok");
        assertThat(new XlsxTableReader()).isNotNull();
    }
}
