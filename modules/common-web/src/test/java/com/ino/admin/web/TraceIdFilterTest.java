package com.ino.admin.web;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class TraceIdFilterTest {
    @Test
    void reusesIncomingTraceIdAndClearsMdcAfterTheRequest() throws ServletException, IOException {
        var request = new MockHttpServletRequest();
        request.addHeader(TraceIdFilter.HEADER_NAME, "consumer-trace-id");
        var response = new MockHttpServletResponse();

        new TraceIdFilter().doFilter(request, response, new MockFilterChain());

        assertThat(response.getHeader(TraceIdFilter.HEADER_NAME)).isEqualTo("consumer-trace-id");
        assertThat(MDC.get(TraceIdFilter.MDC_KEY)).isNull();
    }
}
