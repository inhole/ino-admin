package com.ino.admin.identity.bootstrap;

import com.ino.admin.identity.domain.User;
import com.ino.admin.identity.domain.UserStatus;
import com.ino.admin.identity.infrastructure.persistence.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AdminBootstrapServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-09T10:00:00Z");
    private static final String VALID_PASSWORD = "Admin-Password-2026!";

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    private AdminBootstrapService service;

    @BeforeEach
    void setUp() {
        service = new AdminBootstrapService(userRepository, passwordEncoder, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createsActiveAdminWithNormalizedEmailAndHashedPassword() {
        when(userRepository.existsByEmail("admin@example.com")).thenReturn(false);
        when(passwordEncoder.encode(VALID_PASSWORD)).thenReturn("encoded-password");

        var result = service.bootstrap(" Admin@Example.com ", VALID_PASSWORD, " 시스템 관리자 ");

        assertThat(result).isEqualTo(AdminBootstrapService.Result.CREATED);
        var captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        var saved = captor.getValue();
        assertThat(saved.email()).isEqualTo("admin@example.com");
        assertThat(saved.passwordHash()).isEqualTo("encoded-password");
        assertThat(saved.displayName()).isEqualTo("시스템 관리자");
        assertThat(saved.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(saved.id()).isNotNull();
    }

    @Test
    void skipsCreationWhenEmailAlreadyExists() {
        when(userRepository.existsByEmail("admin@example.com")).thenReturn(true);

        var result = service.bootstrap("admin@example.com", VALID_PASSWORD, "시스템 관리자");

        assertThat(result).isEqualTo(AdminBootstrapService.Result.ALREADY_EXISTS);
        verify(passwordEncoder, never()).encode(VALID_PASSWORD);
        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsWeakPasswordBeforeWritingUser() {
        assertThatThrownBy(() -> service.bootstrap("admin@example.com", "password", "시스템 관리자"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("비밀번호 정책 위반");
        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsInvalidEmailBeforeWritingUser() {
        assertThatThrownBy(() -> service.bootstrap("not-an-email", VALID_PASSWORD, "시스템 관리자"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이메일 설정");
        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
