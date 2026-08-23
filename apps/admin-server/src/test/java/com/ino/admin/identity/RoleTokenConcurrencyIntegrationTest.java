package com.ino.admin.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ino.admin.identity.api.InvalidRefreshTokenException;
import com.ino.admin.identity.api.LoginUseCase.LoginResult;
import com.ino.admin.identity.api.RefreshTokenUseCase.RefreshResult;
import com.ino.admin.identity.application.LoginService;
import com.ino.admin.identity.application.RefreshTokenService;
import com.ino.admin.identity.application.RoleManagementService;
import com.ino.admin.identity.application.UserManagementService;
import com.ino.admin.identity.domain.Role;
import com.ino.admin.identity.domain.User;
import com.ino.admin.identity.infrastructure.persistence.RoleRepository;
import com.ino.admin.identity.infrastructure.persistence.UserRepository;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.Sql.ExecutionPhase;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Tag("integration")
@SpringBootTest
@Sql(statements = {"TRUNCATE TABLE refresh_tokens, users CASCADE", "DELETE FROM roles WHERE system_role = false"},
        executionPhase = ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(statements = {"TRUNCATE TABLE refresh_tokens, users CASCADE", "DELETE FROM roles WHERE system_role = false"},
        executionPhase = ExecutionPhase.AFTER_TEST_METHOD)
class RoleTokenConcurrencyIntegrationTest {
    private static final Instant NOW = Instant.parse("2026-08-23T00:00:00Z");
    private static final String ROLE = "CUSTOM_ADMIN";
    private static final String EMAIL = "custom-concurrency@example.com";
    private static final String PASSWORD = "Custom-Password-2026!";

    @Autowired LoginService loginService;
    @Autowired RefreshTokenService refreshTokenService;
    @Autowired RoleManagementService roleManagementService;
    @Autowired UserManagementService userManagementService;
    @Autowired RoleRepository roleRepository;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void roleDisableWaitsForLoginAndRevokesItsRefreshToken() throws Exception {
        createCustomRoleUser();
        var executor = Executors.newFixedThreadPool(3);
        var roleBlocker = holdRoleLock(executor);
        try {
            var login = submitWithBackendPid(executor, () -> loginService.login(EMAIL, PASSWORD));
            awaitBlockedBy(login.backendPid(), roleBlocker.backendPid());
            var disable = submitWithBackendPid(executor, () -> roleManagementService.changeEnabled(ROLE, false));
            awaitBlockedBy(disable.backendPid(), login.backendPid());

            roleBlocker.release();
            LoginResult issued = login.result().get(5, TimeUnit.SECONDS);
            disable.result().get(5, TimeUnit.SECONDS);
            roleManagementService.changeEnabled(ROLE, true);

            assertThatThrownBy(() -> refreshTokenService.rotate(issued.refreshToken()))
                    .isInstanceOf(InvalidRefreshTokenException.class);
        } finally {
            roleBlocker.release();
            shutdown(executor);
        }
    }

    @Test
    void roleDisableWaitsForRefreshRotationAndRevokesItsReplacement() throws Exception {
        createCustomRoleUser();
        var initial = loginService.login(EMAIL, PASSWORD);
        var executor = Executors.newFixedThreadPool(3);
        var roleBlocker = holdRoleLock(executor);
        try {
            var rotation = submitWithBackendPid(executor, () -> refreshTokenService.rotate(initial.refreshToken()));
            awaitBlockedBy(rotation.backendPid(), roleBlocker.backendPid());
            var disable = submitWithBackendPid(executor, () -> roleManagementService.changeEnabled(ROLE, false));
            awaitBlockedBy(disable.backendPid(), rotation.backendPid());

            roleBlocker.release();
            RefreshResult rotated = rotation.result().get(5, TimeUnit.SECONDS);
            disable.result().get(5, TimeUnit.SECONDS);
            roleManagementService.changeEnabled(ROLE, true);

            assertThatThrownBy(() -> refreshTokenService.rotate(rotated.refreshToken()))
                    .isInstanceOf(InvalidRefreshTokenException.class);
        } finally {
            roleBlocker.release();
            shutdown(executor);
        }
    }

    @Test
    void concurrentRotationAllowsExactlyOneUseOfRefreshToken() throws Exception {
        createCustomRoleUser();
        var initial = loginService.login(EMAIL, PASSWORD);
        var executor = Executors.newFixedThreadPool(3);
        var roleBlocker = holdRoleLock(executor);
        try {
            var first = submitWithBackendPid(executor, () -> refreshTokenService.rotate(initial.refreshToken()));
            awaitBlockedBy(first.backendPid(), roleBlocker.backendPid());
            var second = submitWithBackendPid(executor, () -> refreshTokenService.rotate(initial.refreshToken()));
            awaitBlockedBy(second.backendPid(), first.backendPid());

            roleBlocker.release();
            assertThat(first.result().get(5, TimeUnit.SECONDS).refreshToken()).isNotBlank();
            assertThatThrownBy(() -> second.result().get(5, TimeUnit.SECONDS))
                    .isInstanceOf(ExecutionException.class)
                    .cause().isInstanceOf(InvalidRefreshTokenException.class);
        } finally {
            roleBlocker.release();
            shutdown(executor);
        }
    }

    @Test
    void roleDisableRevokesTokenFromConcurrentRoleAssignment() throws Exception {
        createCustomRoleUser();
        var targetEmail = "assigned-concurrency@example.com";
        var target = userRepository.saveAndFlush(User.create(targetEmail, passwordEncoder.encode(PASSWORD),
                "역할 변경 사용자", "VIEWER", NOW));
        var assignmentReady = new CountDownLatch(1);
        var allowAssignmentCommit = new CountDownLatch(1);
        var assignmentPid = new AtomicInteger();
        var executor = Executors.newFixedThreadPool(2);
        try {
            var assignment = executor.submit(() -> new TransactionTemplate(transactionManager).execute(status -> {
                assignmentPid.set(jdbcTemplate.queryForObject("select pg_backend_pid()", Integer.class));
                userManagementService.updateProfile(java.util.UUID.randomUUID(), target.id(),
                        new com.ino.admin.identity.api.UserManagementUseCase.UpdateProfile("역할 변경 사용자", ROLE));
                var login = loginService.login(targetEmail, PASSWORD);
                assignmentReady.countDown();
                await(allowAssignmentCommit);
                return login;
            }));
            assertThat(assignmentReady.await(5, TimeUnit.SECONDS)).isTrue();

            var disable = submitWithBackendPid(executor, () -> roleManagementService.changeEnabled(ROLE, false));
            awaitBlockedBy(disable.backendPid(), assignmentPid.get());

            allowAssignmentCommit.countDown();
            LoginResult issued = assignment.get(5, TimeUnit.SECONDS);
            disable.result().get(5, TimeUnit.SECONDS);
            roleManagementService.changeEnabled(ROLE, true);

            assertThatThrownBy(() -> refreshTokenService.rotate(issued.refreshToken()))
                    .isInstanceOf(InvalidRefreshTokenException.class);
        } finally {
            allowAssignmentCommit.countDown();
            shutdown(executor);
        }
    }

    private RoleBlocker holdRoleLock(ExecutorService executor) throws Exception {
        var locked = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var backendPid = new AtomicInteger();
        var result = executor.submit(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            backendPid.set(jdbcTemplate.queryForObject("select pg_backend_pid()", Integer.class));
            roleRepository.findByIdForUpdate(ROLE).orElseThrow();
            locked.countDown();
            await(release);
        }));
        assertThat(locked.await(5, TimeUnit.SECONDS)).isTrue();
        return new RoleBlocker(backendPid.get(), release, result);
    }

    private <T> PidFuture<T> submitWithBackendPid(ExecutorService executor, java.util.concurrent.Callable<T> action)
            throws InterruptedException {
        var connected = new CountDownLatch(1);
        var backendPid = new AtomicInteger();
        var result = executor.submit(() -> new TransactionTemplate(transactionManager).execute(status -> {
            backendPid.set(jdbcTemplate.queryForObject("select pg_backend_pid()", Integer.class));
            connected.countDown();
            try {
                return action.call();
            } catch (RuntimeException exception) {
                throw exception;
            } catch (Exception exception) {
                throw new IllegalStateException(exception);
            }
        }));
        assertThat(connected.await(5, TimeUnit.SECONDS)).isTrue();
        return new PidFuture<>(backendPid.get(), result);
    }

    private void awaitBlockedBy(int blockedPid, int blockerPid) throws InterruptedException, TimeoutException {
        var deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            var blocked = jdbcTemplate.queryForObject(
                    "select ? = any(pg_blocking_pids(?))", Boolean.class, blockerPid, blockedPid);
            if (Boolean.TRUE.equals(blocked)) return;
            Thread.sleep(20);
        }
        throw new TimeoutException("예상한 PostgreSQL 트랜잭션의 row lock을 기다리지 않았습니다.");
    }

    private void createCustomRoleUser() {
        roleRepository.saveAndFlush(Role.create(ROLE, "커스텀 관리자", Set.of("user:read"), NOW));
        userRepository.saveAndFlush(User.create(EMAIL, passwordEncoder.encode(PASSWORD), "커스텀 사용자", ROLE, NOW));
    }

    private static void shutdown(ExecutorService executor) throws InterruptedException {
        executor.shutdownNow();
        assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("동시성 테스트 대기 시간이 초과되었습니다.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("동시성 테스트가 중단되었습니다.", exception);
        }
    }

    private record PidFuture<T>(int backendPid, Future<T> result) {}

    private record RoleBlocker(int backendPid, CountDownLatch releaseSignal, Future<?> result) {
        void release() {
            releaseSignal.countDown();
        }
    }
}
