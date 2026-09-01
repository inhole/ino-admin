package com.ino.admin.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ino.admin.config.ApplicationConfig;
import com.ino.admin.file.FileController;
import com.ino.admin.file.api.FileManagementUseCase;
import com.ino.admin.file.api.FileNotFoundException;
import com.ino.admin.web.GlobalExceptionHandler;
import com.ino.spring.modules.web.TraceIdFilter;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FileController.class)
@Import({ApplicationConfig.class, GlobalExceptionHandler.class, TraceIdFilter.class,
        SecurityConfig.class, RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class FileApiRbacTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean FileManagementUseCase files;
    @MockitoBean JwtDecoder jwtDecoder;

    @Test
    void unauthenticatedRequestReturnsStandardUnauthorizedError() throws Exception {
        mockMvc.perform(get("/api/v1/files").header("X-Trace-Id", "file-unauthorized-trace"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.traceId").value("file-unauthorized-trace"));

        verifyNoInteractions(files);
    }

    @Test
    void readPermissionCannotUploadOrDelete() throws Exception {
        var ownerId = UUID.randomUUID();
        var fileId = UUID.randomUUID();
        var upload = new MockMultipartFile("file", "report.pdf", "application/pdf", "pdf".getBytes());

        mockMvc.perform(multipart("/api/v1/files").file(upload)
                        .with(jwt().jwt(token -> token.subject(ownerId.toString()))
                                .authorities(new SimpleGrantedAuthority("file:read"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mockMvc.perform(delete("/api/v1/files/{fileId}", fileId)
                        .with(jwt().jwt(token -> token.subject(ownerId.toString()))
                                .authorities(new SimpleGrantedAuthority("file:read"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        verifyNoInteractions(files);
    }

    @Test
    void writePermissionCannotListOrDownload() throws Exception {
        var ownerId = UUID.randomUUID();
        var fileId = UUID.randomUUID();

        mockMvc.perform(get("/api/v1/files")
                        .with(jwt().jwt(token -> token.subject(ownerId.toString()))
                                .authorities(new SimpleGrantedAuthority("file:write"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        mockMvc.perform(get("/api/v1/files/{fileId}/content", fileId)
                        .with(jwt().jwt(token -> token.subject(ownerId.toString()))
                                .authorities(new SimpleGrantedAuthority("file:write"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        verifyNoInteractions(files);
    }

    @Test
    void writePermissionCanUploadAndDeleteOwnFile() throws Exception {
        var ownerId = UUID.randomUUID();
        var fileId = UUID.randomUUID();
        var upload = new MockMultipartFile("file", "report.pdf", "application/pdf", "pdf".getBytes());
        when(files.upload(eq(ownerId), any())).thenReturn(
                new FileManagementUseCase.StoredFile(fileId, "report.pdf", "application/pdf", 3));

        mockMvc.perform(multipart("/api/v1/files").file(upload)
                        .with(jwt().jwt(token -> token.subject(ownerId.toString()))
                                .authorities(new SimpleGrantedAuthority("file:write"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(fileId.toString()));
        mockMvc.perform(delete("/api/v1/files/{fileId}", fileId)
                        .with(jwt().jwt(token -> token.subject(ownerId.toString()))
                                .authorities(new SimpleGrantedAuthority("file:write"))))
                .andExpect(status().isNoContent());

        verify(files).delete(ownerId, fileId);
    }

    @Test
    void mapsHiddenFileFailuresToNotFound() throws Exception {
        var requesterId = UUID.randomUUID();
        var fileId = UUID.randomUUID();
        when(files.download(requesterId, fileId)).thenThrow(new FileNotFoundException());
        doThrow(new FileNotFoundException()).when(files).delete(requesterId, fileId);

        mockMvc.perform(get("/api/v1/files/{fileId}/content", fileId)
                        .with(jwt().jwt(token -> token.subject(requesterId.toString()))
                                .authorities(new SimpleGrantedAuthority("file:read"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FILE_NOT_FOUND"));
        mockMvc.perform(delete("/api/v1/files/{fileId}", fileId)
                        .with(jwt().jwt(token -> token.subject(requesterId.toString()))
                                .authorities(new SimpleGrantedAuthority("file:write"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("FILE_NOT_FOUND"));
    }
}
