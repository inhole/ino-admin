package com.ino.admin.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ino.spring.modules.core.BusinessException;
import com.ino.admin.identity.application.UserManagementService;
import com.ino.admin.identity.domain.User;
import com.ino.admin.identity.domain.UserStatus;
import com.ino.admin.identity.infrastructure.persistence.UserRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;

@Tag("integration")
@SpringBootTest
@Sql(statements = "TRUNCATE TABLE refresh_tokens, users CASCADE", executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(statements = "TRUNCATE TABLE refresh_tokens, users CASCADE", executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
class LastSuperAdminConcurrencyIntegrationTest {
    private static final Instant NOW = Instant.parse("2026-08-23T00:00:00Z");

    @Autowired UserManagementService userManagementService;
    @Autowired UserRepository userRepository;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void concurrentChangesKeepOneActiveSuperAdmin() throws Exception {
        var first = User.createInitialAdmin("first-admin@example.com", "hash", "첫 번째 최고 관리자", NOW);
        var second = User.createInitialAdmin("second-admin@example.com", "hash", "두 번째 최고 관리자", NOW);
        userRepository.saveAllAndFlush(List.of(first, second));

        var firstHasLock = new CountDownLatch(1);
        var allowFirstCommit = new CountDownLatch(1);
        var secondHasConnection = new CountDownLatch(1);
        var secondBackendPid = new AtomicInteger();
        var executor = Executors.newFixedThreadPool(2);
        try {
            var firstChange = executor.submit(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                userRepository.findAllActiveSuperAdminsForUpdate();
                firstHasLock.countDown();
                await(allowFirstCommit);
                userManagementService.changeStatus(UUID.randomUUID(), first.id(), "DISABLED");
            }));
            assertThat(firstHasLock.await(5, TimeUnit.SECONDS)).isTrue();

            var secondChange = executor.submit(() -> new TransactionTemplate(transactionManager).execute(status -> {
                secondBackendPid.set(jdbcTemplate.queryForObject("select pg_backend_pid()", Integer.class));
                secondHasConnection.countDown();
                return userManagementService.updateProfile(UUID.randomUUID(), second.id(),
                        new com.ino.admin.identity.api.UserManagementUseCase.UpdateProfile("두 번째 관리자", "ADMIN"));
            }));
            assertThat(secondHasConnection.await(5, TimeUnit.SECONDS)).isTrue();
            awaitPostgresLock(secondBackendPid.get());

            allowFirstCommit.countDown();
            firstChange.get(5, TimeUnit.SECONDS);
            assertThatThrownBy(() -> secondChange.get(5, TimeUnit.SECONDS))
                    .isInstanceOf(ExecutionException.class)
                    .cause().isInstanceOf(BusinessException.class)
                    .extracting("code").isEqualTo("LAST_SUPER_ADMIN_PROTECTED");

            assertThat(userRepository.findAllByRole("SUPER_ADMIN"))
                    .filteredOn(user -> user.status() == UserStatus.ACTIVE)
                    .hasSize(1);
        } finally {
            allowFirstCommit.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    private void awaitPostgresLock(int backendPid) throws InterruptedException, TimeoutException {
        var deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            var waitType = jdbcTemplate.queryForObject(
                    "select wait_event_type from pg_stat_activity where pid = ?", String.class, backendPid);
            if ("Lock".equals(waitType)) return;
            Thread.sleep(20);
        }
        throw new TimeoutException("두 번째 변경 요청이 PostgreSQL row lock을 기다리지 않았습니다.");
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("동시성 테스트 대기 시간이 초과되었습니다.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("동시성 테스트가 중단되었습니다.", exception);
        }
    }
}
