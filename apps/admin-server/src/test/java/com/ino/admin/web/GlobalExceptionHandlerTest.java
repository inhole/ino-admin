package com.ino.admin.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

class GlobalExceptionHandlerTest {
    @Test void mapsMultipartLimitToPayloadTooLarge() {
        var response = new GlobalExceptionHandler(Clock.systemUTC())
                .handleMaxUploadSize(new MaxUploadSizeExceededException(10));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("FILE_TOO_LARGE");
    }
}
