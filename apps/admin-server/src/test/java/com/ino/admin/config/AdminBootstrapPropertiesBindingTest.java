package com.ino.admin.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.ino.admin.identity.bootstrap.AdminBootstrapProperties;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class AdminBootstrapPropertiesBindingTest {
    private static final Path DOTENV_PATH = Path.of(".env").toAbsolutePath();

    @Test
    void bindsKoreanDisplayNameFromUtf8Dotenv() throws IOException {
        assertThat(Files.exists(DOTENV_PATH)).isFalse();
        Files.writeString(
                DOTENV_PATH,
                "APP_BOOTSTRAP_ADMIN_DISPLAY_NAME=시스템 관리자\n",
                StandardCharsets.UTF_8);

        try {
            contextRunner().run(context -> {
                assertThat(context.getEnvironment().getProperty("APP_BOOTSTRAP_ADMIN_DISPLAY_NAME"))
                        .isEqualTo("시스템 관리자");
                assertThat(context.getBean(AdminBootstrapProperties.class).getDisplayName())
                        .isEqualTo("시스템 관리자");
            });
        } finally {
            Files.deleteIfExists(DOTENV_PATH);
        }
    }

    @Test
    void retainsKoreanDefaultWhenDotenvIsMissing() {
        assertThat(Files.exists(DOTENV_PATH)).isFalse();

        contextRunner().run(context ->
                assertThat(context.getBean(AdminBootstrapProperties.class).getDisplayName())
                        .isEqualTo("시스템 관리자"));
    }

    private ApplicationContextRunner contextRunner() {
        return new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withUserConfiguration(ApplicationConfig.class, utf8DotenvConfigClass());
    }

    private Class<?> utf8DotenvConfigClass() {
        try {
            return Class.forName("com.ino.admin.config.Utf8DotenvConfig");
        } catch (ClassNotFoundException exception) {
            throw new AssertionError("UTF-8 .env configuration must be registered", exception);
        }
    }
}
