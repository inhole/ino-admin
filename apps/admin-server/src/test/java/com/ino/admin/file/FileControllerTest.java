package com.ino.admin.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ino.admin.config.ApplicationConfig;
import com.ino.admin.file.api.FileManagementUseCase;
import com.ino.admin.web.GlobalExceptionHandler;
import com.ino.admin.web.TraceIdFilter;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FileController.class)
@Import({ApplicationConfig.class, GlobalExceptionHandler.class, TraceIdFilter.class})
class FileControllerTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean FileManagementUseCase files;

    @Test
    void mapsValidatedFileFiltersToTheUseCase() throws Exception {
        var ownerId = UUID.randomUUID();
        var from = Instant.parse("2026-08-01T00:00:00Z");
        var to = Instant.parse("2026-09-01T00:00:00Z");
        when(files.list(eq(ownerId), org.mockito.ArgumentMatchers.any(), eq(0), eq(20)))
                .thenReturn(new FileManagementUseCase.FilePage(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/v1/files")
                        .with(jwt().jwt(token -> token.subject(ownerId.toString()))
                                .authorities(new SimpleGrantedAuthority("file:read")))
                        .queryParam("name", "report")
                        .queryParam("contentType", "application/pdf")
                        .queryParam("createdFrom", from.toString())
                        .queryParam("createdTo", to.toString())
                        .queryParam("sort", "originalName")
                        .queryParam("direction", "asc"))
                .andExpect(status().isOk());

        var query = ArgumentCaptor.forClass(FileManagementUseCase.FileListQuery.class);
        verify(files).list(eq(ownerId), query.capture(), eq(0), eq(20));
        assertThat(query.getValue()).isEqualTo(new FileManagementUseCase.FileListQuery(
                "report", "application/pdf", from, to,
                FileManagementUseCase.FileSort.ORIGINAL_NAME, FileManagementUseCase.SortDirection.ASC));
    }

    @Test
    void rejectsUnsupportedFileSort() throws Exception {
        var ownerId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/files")
                        .with(jwt().jwt(token -> token.subject(ownerId.toString()))
                                .authorities(new SimpleGrantedAuthority("file:read")))
                        .queryParam("sort", "storageKey")
                        .header("X-Trace-Id", "file-filter-trace"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.traceId").value("file-filter-trace"));
        verifyNoInteractions(files);
    }
}
