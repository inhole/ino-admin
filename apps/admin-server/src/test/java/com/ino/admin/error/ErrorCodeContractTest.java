package com.ino.admin.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ErrorCodeContractTest {
    @Test
    void applicationOwnsTheExistingPublicErrorCatalog() {
        assertThat(ErrorCode.values()).extracting(ErrorCode::code, ErrorCode::message)
                .contains(
                        org.assertj.core.groups.Tuple.tuple("VALIDATION_ERROR", "요청 값이 올바르지 않습니다."),
                        org.assertj.core.groups.Tuple.tuple("INVALID_CREDENTIALS", "이메일 또는 비밀번호가 올바르지 않습니다."),
                        org.assertj.core.groups.Tuple.tuple("USER_NOT_FOUND", "사용자를 찾을 수 없습니다."),
                        org.assertj.core.groups.Tuple.tuple("MENU_NOT_FOUND", "메뉴를 찾을 수 없습니다."),
                        org.assertj.core.groups.Tuple.tuple("EXCEL_IMPORT_INVALID", "Excel 가져오기 내용이 올바르지 않습니다."));
    }

    @Test
    void errorCodesRemainUnique() {
        var counts = java.util.Arrays.stream(ErrorCode.values())
                .collect(java.util.stream.Collectors.groupingBy(ErrorCode::code, java.util.stream.Collectors.counting()));

        assertThat(counts).allSatisfy((code, count) -> assertThat(count).as(code).isOne());
        assertThat(counts).containsKeys("FORBIDDEN", "INTERNAL_ERROR", "FILE_TOO_LARGE", "EXCEL_EXPORT_FAILED");
    }
}
