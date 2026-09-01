package com.ino.admin.identity.domain;

import java.util.ArrayList;
import java.util.List;

public final class PasswordPolicy {
    private PasswordPolicy() {}

    public static List<String> violations(String password) {
        var violations = new ArrayList<String>();
        if (password == null || password.length() < 12) violations.add("12자 이상이어야 합니다.");
        if (password != null && password.length() > 128) violations.add("128자를 초과할 수 없습니다.");
        if (password == null || password.chars().noneMatch(Character::isUpperCase)) violations.add("대문자를 포함해야 합니다.");
        if (password == null || password.chars().noneMatch(Character::isLowerCase)) violations.add("소문자를 포함해야 합니다.");
        if (password == null || password.chars().noneMatch(Character::isDigit)) violations.add("숫자를 포함해야 합니다.");
        if (password == null || password.chars().allMatch(Character::isLetterOrDigit)) violations.add("특수문자를 포함해야 합니다.");
        return List.copyOf(violations);
    }
}
