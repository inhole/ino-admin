package com.ino.admin.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.ino.admin.web.TraceIdFilter;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class AuditRequestFilterTest {
    @AfterEach
    void cleanContext() {
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    @Test
    void recordsAuthenticatedMutationWithoutQueryOrBody() throws Exception {
        var actorId = UUID.randomUUID();
        var jwt = Jwt.withTokenValue("token").header("alg", "none").subject(actorId.toString()).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
        MDC.put(TraceIdFilter.MDC_KEY, "audit-trace");
        var writer = new CapturingAuditWriter();
        var filter = new AuditRequestFilter(writer);
        var request = new MockHttpServletRequest("PATCH", "/api/v1/users/" + UUID.randomUUID());
        request.setQueryString("password=secret");
        request.addHeader("User-Agent", "browser");
        request.setRemoteAddr("127.0.0.1");
        var response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(writer.command.actor().id()).isEqualTo(actorId);
        assertThat(writer.command.action()).isEqualTo("USER_UPDATE");
        assertThat(writer.command.resource()).matches("/api/v1/users/[0-9a-f-]+")
                .doesNotContain("password", "secret");
        assertThat(writer.command.result()).isEqualTo(AuditResult.SUCCESS);
        assertThat(writer.command.traceId()).isEqualTo("audit-trace");
    }

    @Test
    void recordsOnlyAllowlistedLoginAndRequestSnapshotFieldsWithinColumnLimits() throws Exception {
        var writer = new CapturingAuditWriter();
        var filter = new AuditRequestFilter(writer);
        var request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        LoginAuditContext.attach(request, "e".repeat(400), "d".repeat(150), "r".repeat(150));
        request.setAttribute("password", "secret");
        request.setRemoteAddr("1".repeat(60));
        request.addHeader("User-Agent", "u".repeat(600));

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        assertThat(writer.command.actor().attributes())
                .containsOnlyKeys(AuditAttributeKeys.LOGIN_EMAIL, AuditAttributeKeys.LOGIN_DISPLAY_NAME,
                        AuditAttributeKeys.LOGIN_ROLE);
        assertThat(writer.command.actor().attributes().get(AuditAttributeKeys.LOGIN_EMAIL)).hasSize(320);
        assertThat(writer.command.actor().attributes().get(AuditAttributeKeys.LOGIN_DISPLAY_NAME)).hasSize(100);
        assertThat(writer.command.actor().attributes().get(AuditAttributeKeys.LOGIN_ROLE)).hasSize(100);
        assertThat(writer.command.contextAttributes())
                .containsOnlyKeys(AuditAttributeKeys.IP_ADDRESS, AuditAttributeKeys.USER_AGENT);
        assertThat(writer.command.contextAttributes().get(AuditAttributeKeys.IP_ADDRESS)).hasSize(45);
        assertThat(writer.command.contextAttributes().get(AuditAttributeKeys.USER_AGENT)).hasSize(512);
        assertThat(writer.command.toString()).doesNotContain("password", "secret");
    }

    @Test
    void ignoresReadRequests() throws Exception {
        var writer = new CapturingAuditWriter();
        var filter = new AuditRequestFilter(writer);

        filter.doFilter(new MockHttpServletRequest("GET", "/api/v1/users"),
                new MockHttpServletResponse(), new MockFilterChain());

        assertThat(writer.command).isNull();
    }

    @Test
    void mapsMajorAdminMutationsToSemanticActions() throws Exception {
        assertThat(actionFor("POST", "/api/v1/auth/login")).isEqualTo("AUTH_LOGIN");
        assertThat(actionFor("PATCH", "/api/v1/permissions/ADMIN")).isEqualTo("PERMISSION_UPDATE");
        assertThat(actionFor("POST", "/api/v1/users")).isEqualTo("USER_CREATE");
        assertThat(actionFor("POST", "/api/v1/files")).isEqualTo("FILE_UPLOAD");
        assertThat(actionFor("DELETE", "/api/v1/files/file-1")).isEqualTo("FILE_DELETE");
        assertThat(actionFor("GET", "/api/v1/excel/users/export")).isEqualTo("USER_EXCEL_EXPORT");
        assertThat(actionFor("POST", "/api/v1/excel/users/import")).isEqualTo("USER_EXCEL_IMPORT");
    }

    private String actionFor(String method, String path) throws Exception {
        var writer = new CapturingAuditWriter();
        new AuditRequestFilter(writer).doFilter(new MockHttpServletRequest(method, path),
                new MockHttpServletResponse(), new MockFilterChain());
        return writer.command.action();
    }

    private static final class CapturingAuditWriter implements AuditWriter {
        private AuditCommand command;
        @Override public void write(AuditCommand command) { this.command = command; }
    }
}
