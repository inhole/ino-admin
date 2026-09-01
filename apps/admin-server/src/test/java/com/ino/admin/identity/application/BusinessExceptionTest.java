package com.ino.admin.identity.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.ino.spring.modules.core.BusinessException;
import com.ino.admin.error.ErrorCode;
import org.junit.jupiter.api.Test;

class BusinessExceptionTest {
    @Test
    void usesCodeAndMessageFromErrorCode() {
        var exception = new BusinessException(ErrorCode.ROLE_NOT_FOUND);

        assertThat(exception.code()).isEqualTo("ROLE_NOT_FOUND");
        assertThat(exception.getMessage()).isEqualTo("역할을 찾을 수 없습니다.");
    }
}
