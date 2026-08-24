package com.ino.admin.audit;

import com.ino.admin.web.TraceIdFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@ConditionalOnBean(AuditWriter.class)
class AuditRequestFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(AuditRequestFilter.class);
    private static final Set<String> MUTATIONS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private final AuditWriter writer;

    AuditRequestFilter(AuditWriter writer) { this.writer = writer; }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        var auditableExport = request.getMethod().equals("GET")
                && request.getRequestURI().startsWith("/api/v1/excel/");
        return !request.getRequestURI().startsWith("/api/v1/")
                || (!MUTATIONS.contains(request.getMethod()) && !auditableExport);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            chain.doFilter(request, response);
        } finally {
            try {
                writer.write(new AuditCommand(actorId(), semanticAction(request), limit(request.getRequestURI(), 500),
                        response.getStatus() < 400 ? AuditResult.SUCCESS : AuditResult.FAILURE,
                        response.getStatus(), limit(request.getRemoteAddr(), 45),
                        limit(request.getHeader("User-Agent"), 512), limit(MDC.get(TraceIdFilter.MDC_KEY), 100)));
            } catch (RuntimeException exception) {
                log.error("Failed to persist audit log. traceId={}", MDC.get(TraceIdFilter.MDC_KEY), exception);
            }
        }
    }

    private UUID actorId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken token) {
            try { return UUID.fromString(token.getToken().getSubject()); }
            catch (IllegalArgumentException ignored) { return null; }
        }
        return null;
    }

    private String semanticAction(HttpServletRequest request) {
        var method = request.getMethod();
        var path = request.getRequestURI();
        if (method.equals("POST") && path.equals("/api/v1/auth/login")) return "AUTH_LOGIN";
        if (method.equals("POST") && path.equals("/api/v1/auth/refresh")) return "AUTH_REFRESH";
        if (method.equals("POST") && path.equals("/api/v1/auth/logout")) return "AUTH_LOGOUT";
        if (method.equals("PUT") && path.equals("/api/v1/auth/password")) return "PASSWORD_CHANGE";
        if (method.equals("POST") && path.equals("/api/v1/users")) return "USER_CREATE";
        if (method.equals("PATCH") && path.startsWith("/api/v1/users/")) return "USER_UPDATE";
        if ((method.equals("POST") || method.equals("PATCH")) && path.startsWith("/api/v1/permissions"))
            return "PERMISSION_UPDATE";
        if (method.equals("POST") && path.equals("/api/v1/menus")) return "MENU_CREATE";
        if (method.equals("PATCH") && path.startsWith("/api/v1/menus/")) return "MENU_UPDATE";
        if (method.equals("POST") && path.equals("/api/v1/files")) return "FILE_UPLOAD";
        if (method.equals("DELETE") && path.startsWith("/api/v1/files/")) return "FILE_DELETE";
        if (method.equals("GET") && path.equals("/api/v1/excel/users/export")) return "USER_EXCEL_EXPORT";
        return method + "_REQUEST";
    }

    private String limit(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }
}
