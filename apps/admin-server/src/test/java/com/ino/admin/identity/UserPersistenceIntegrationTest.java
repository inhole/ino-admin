package com.ino.admin.identity;

import com.ino.admin.identity.bootstrap.AdminBootstrapService;
import com.ino.admin.identity.domain.UserStatus;
import com.ino.admin.identity.domain.UserRole;
import com.ino.admin.identity.infrastructure.persistence.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@Tag("integration")
@Transactional
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class UserPersistenceIntegrationTest {
    @Autowired UserRepository userRepository;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired AdminBootstrapService bootstrapService;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    void appliesUserMigrationAndPersistsUser() {
        var migrationApplied = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version = '2' AND success",
                Integer.class
        );
        assertThat(migrationApplied).isEqualTo(1);

        var password = "Integration-Password-2026!";
        var result = bootstrapService.bootstrap(
                "integration@example.com",
                password,
                "통합 테스트 관리자"
        );

        var saved = userRepository.findByEmail("integration@example.com");
        assertThat(result).isEqualTo(AdminBootstrapService.Result.CREATED);
        assertThat(saved).isPresent();
        assertThat(saved.orElseThrow().status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(saved.orElseThrow().role()).isEqualTo(UserRole.SUPER_ADMIN);
        assertThat(saved.orElseThrow().passwordHash()).isNotEqualTo(password);
        assertThat(passwordEncoder.matches(password, saved.orElseThrow().passwordHash())).isTrue();
        assertThat(bootstrapService.bootstrap("integration@example.com", password, "통합 테스트 관리자"))
                .isEqualTo(AdminBootstrapService.Result.ALREADY_EXISTS);
    }
}
