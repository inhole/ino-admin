package com.ino.admin.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class AuditContractTest {
    @Test
    void preservesTheExistingEventFieldsAndSupportsIndependentWriter() {
        var actorId = UUID.randomUUID();
        var command = new AuditCommand(actorId, "admin@example.com", "관리자", "SUPER_ADMIN",
                "USER_UPDATE", "/api/v1/users/1", AuditResult.SUCCESS, 200,
                "127.0.0.1", "consumer-agent", "trace-id");
        var received = new AtomicReference<AuditCommand>();
        AuditWriter writer = received::set;

        writer.write(command);

        assertThat(received.get()).isEqualTo(command);
        assertThat(command.actorId()).isEqualTo(actorId);
        assertThat(command.loginEmail()).isEqualTo("admin@example.com");
        assertThat(command.action()).isEqualTo("USER_UPDATE");
        assertThat(command.result()).isEqualTo(AuditResult.SUCCESS);
        assertThat(command.traceId()).isEqualTo("trace-id");
    }

    @Test
    void exposesStableLoginAccountRequestAttributeContract() {
        var account = new AuditCommand.LoginAccount("admin@example.com", "관리자", "ADMIN");
        assertThat(AuditCommand.LOGIN_ACCOUNT_ATTRIBUTE).isEqualTo(AuditCommand.class.getName() + ".loginAccount");
        assertThat(account.role()).isEqualTo("ADMIN");
    }
}
