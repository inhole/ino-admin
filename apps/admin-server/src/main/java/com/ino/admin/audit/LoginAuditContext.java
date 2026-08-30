package com.ino.admin.audit;

import jakarta.servlet.http.HttpServletRequest;

public final class LoginAuditContext {
    private static final String LOGIN_ACCOUNT_ATTRIBUTE = LoginAuditContext.class.getName() + ".loginAccount";

    private LoginAuditContext() {}

    public static void attach(HttpServletRequest request, String email, String displayName, String role) {
        request.setAttribute(LOGIN_ACCOUNT_ATTRIBUTE, new LoginAccount(email, displayName, role));
    }

    static LoginAccount read(HttpServletRequest request) {
        var value = request.getAttribute(LOGIN_ACCOUNT_ATTRIBUTE);
        return value instanceof LoginAccount account ? account : new LoginAccount(null, null, null);
    }

    record LoginAccount(String email, String displayName, String role) {}
}
