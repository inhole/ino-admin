package com.ino.admin.error;

import com.ino.spring.modules.core.ErrorDescriptor;

public enum ErrorCode implements ErrorDescriptor {
    VALIDATION_ERROR("요청 값이 올바르지 않습니다."),
    FORBIDDEN("요청을 수행할 권한이 없습니다."),
    INTERNAL_ERROR("서버 오류가 발생했습니다."),
    FILE_TOO_LARGE("파일 크기 제한을 초과했습니다."),
    FILE_NOT_FOUND("파일을 찾을 수 없습니다."),
    INVALID_CREDENTIALS("이메일 또는 비밀번호가 올바르지 않습니다."),
    INVALID_REFRESH_TOKEN("유효하지 않은 새로 고침 토큰입니다."),
    INVALID_USER_SORT("허용되지 않은 사용자 정렬입니다."),
    INVALID_SORT_DIRECTION("허용되지 않은 정렬 방향입니다."),
    INVALID_CURRENT_PASSWORD("현재 비밀번호가 올바르지 않습니다."),
    PASSWORD_REUSE_NOT_ALLOWED("현재 비밀번호와 다른 비밀번호를 사용해야 합니다."),
    PASSWORD_POLICY_VIOLATION("비밀번호 정책을 만족하지 않습니다."),
    SYSTEM_ROLE_PROTECTED("시스템 역할은 변경할 수 없습니다."),
    ROLE_NOT_FOUND("역할을 찾을 수 없습니다."),
    INVALID_PERMISSION("등록되지 않은 권한이 포함되어 있습니다."),
    INVALID_ROLE_KEY("역할 키 형식이 올바르지 않습니다."),
    ROLE_ALREADY_EXISTS("이미 존재하는 역할입니다."),
    SELF_DISABLE_NOT_ALLOWED("자기 계정은 비활성화할 수 없습니다."),
    SELF_ROLE_CHANGE_NOT_ALLOWED("자기 계정의 역할은 변경할 수 없습니다."),
    USER_NOT_FOUND("사용자를 찾을 수 없습니다."),
    INVALID_USER_STATUS("사용자 상태가 올바르지 않습니다."),
    EMAIL_ALREADY_EXISTS("이미 사용 중인 이메일입니다."),
    INVALID_USER_ROLE("할당 가능한 역할을 선택해야 합니다."),
    LAST_SUPER_ADMIN_PROTECTED("마지막 활성 최고 관리자는 변경할 수 없습니다."),
    MENU_ID_ALREADY_EXISTS("이미 존재하는 메뉴 ID입니다."),
    MENU_NOT_FOUND("메뉴를 찾을 수 없습니다."),
    INVALID_MENU_PARENT("부모 메뉴가 존재하지 않습니다."),
    MENU_CYCLE("메뉴 트리에 순환을 만들 수 없습니다."),
    MENU_ORDER_DUPLICATED("같은 부모 아래 정렬 순서가 중복됩니다."),
    MENU_DEPTH_EXCEEDED("메뉴는 최대 3뎁스까지 구성할 수 있습니다."),
    INVALID_MENU_REORDER("전체 메뉴의 올바른 재정렬 정보가 필요합니다."),
    EXCEL_TEMPLATE_FAILED("Excel 양식을 생성하지 못했습니다."),
    EXCEL_IMPORT_INVALID("Excel 가져오기 내용이 올바르지 않습니다."),
    EXCEL_ROW_LIMIT("Excel 내보내기 최대 행 수를 초과했습니다."),
    EXCEL_EXPORT_FAILED("Excel 파일을 생성하지 못했습니다.");

    private final String message;

    ErrorCode(String message) { this.message = message; }

    @Override public String code() { return name(); }
    @Override public String message() { return message; }
}
