package com.ino.admin.sample;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import com.ino.admin.config.ApplicationConfig;
import com.ino.admin.web.GlobalExceptionHandler;
import com.ino.spring.modules.web.TraceIdFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SampleController.class)
@Import({ApplicationConfig.class, GlobalExceptionHandler.class, TraceIdFilter.class})
class SampleControllerTest {
    @Autowired MockMvc mockMvc;

    @Test
    void returnsSamplesWithTraceId() throws Exception {
        mockMvc.perform(get("/api/v1/samples").with(jwt()).header("X-Trace-Id", "test-trace"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Trace-Id", "test-trace"))
                .andExpect(jsonPath("$.content", hasSize(3)))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void returnsStandardErrorForInvalidPagination() throws Exception {
        mockMvc.perform(get("/api/v1/samples").with(jwt()).queryParam("size", "0").header("X-Trace-Id", "validation-trace"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.traceId").value("validation-trace"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("size"));
    }
}
