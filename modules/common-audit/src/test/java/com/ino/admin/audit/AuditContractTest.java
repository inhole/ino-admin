package com.ino.admin.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class AuditContractTest {
    @Test
    void keepsLoginAndHttpDetailsOutOfTheCommonContract() {
        assertThat(Arrays.stream(AuditCommand.class.getRecordComponents())
                .map(component -> component.getName()))
                .doesNotContain("loginEmail", "loginDisplayName", "loginRole", "ipAddress", "userAgent");
        assertThat(Arrays.stream(AuditCommand.class.getDeclaredFields())
                .map(field -> field.getName()))
                .doesNotContain("LOGIN_ACCOUNT_ATTRIBUTE");
    }

    @Test
    void preservesTheExistingEventFieldsAndSupportsIndependentWriter() {
        var actorId = UUID.randomUUID();
        var command = new AuditCommand(new AuditActor(actorId, Map.of("tenant", "operations")),
                "USER_UPDATE", "/api/v1/users/1", AuditResult.SUCCESS, 200,
                "trace-id", Map.of("channel", "admin-api"));
        var received = new AtomicReference<AuditCommand>();
        AuditWriter writer = received::set;

        writer.write(command);

        assertThat(received.get()).isEqualTo(command);
        assertThat(command.actor().id()).isEqualTo(actorId);
        assertThat(command.actor().attributes()).containsEntry("tenant", "operations");
        assertThat(command.action()).isEqualTo("USER_UPDATE");
        assertThat(command.result()).isEqualTo(AuditResult.SUCCESS);
        assertThat(command.traceId()).isEqualTo("trace-id");
    }

    @Test
    void protectsActorAndContextSnapshotsFromCallerMutation() {
        var actorAttributes = new java.util.HashMap<String, String>();
        var contextAttributes = new java.util.HashMap<String, String>();
        var command = new AuditCommand(new AuditActor(null, actorAttributes), "AUTH_LOGIN", "/api/v1/auth/login",
                AuditResult.SUCCESS, 200, "trace-id", contextAttributes);

        actorAttributes.put("password", "secret");
        contextAttributes.put("authorization", "token");

        assertThat(command.actor().attributes()).isEmpty();
        assertThat(command.contextAttributes()).isEmpty();
    }
}
