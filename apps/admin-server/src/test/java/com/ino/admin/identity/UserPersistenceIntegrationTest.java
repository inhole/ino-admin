package com.ino.admin.identity;

import com.ino.admin.identity.bootstrap.AdminBootstrapService;
import com.ino.admin.identity.domain.UserStatus;
import com.ino.admin.identity.infrastructure.persistence.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;

@Tag("integration")
@Transactional
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class UserPersistenceIntegrationTest {
    @Autowired UserRepository userRepository;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired AdminBootstrapService bootstrapService;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired EntityManager entityManager;

    @Test
    void appliesUserMigrationAndPersistsUser() {
        var migrationApplied = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM flyway_schema_history WHERE version = '2' AND success",
                Integer.class
        );
        assertThat(migrationApplied).isEqualTo(1);

        var password = "Integration-Password-2026!";
        var displayName = "시스템 관리자 한글 검증";
        var result = bootstrapService.bootstrap(
                "integration@example.com",
                password,
                displayName
        );

        entityManager.flush();
        entityManager.clear();
        var saved = userRepository.findByEmail("integration@example.com");
        assertThat(result).isEqualTo(AdminBootstrapService.Result.CREATED);
        assertThat(saved).isPresent();
        assertThat(saved.orElseThrow().displayName()).isEqualTo(displayName);
        assertThat(saved.orElseThrow().status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(saved.orElseThrow().role()).isEqualTo("SUPER_ADMIN");
        assertThat(saved.orElseThrow().passwordHash()).isNotEqualTo(password);
        assertThat(passwordEncoder.matches(password, saved.orElseThrow().passwordHash())).isTrue();
        assertThat(bootstrapService.bootstrap("integration@example.com", password, displayName))
                .isEqualTo(AdminBootstrapService.Result.ALREADY_EXISTS);
    }
}
