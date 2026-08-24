package com.ino.admin.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.ino.admin.identity.bootstrap.AdminBootstrapProperties;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePropertySource;

class AdminBootstrapPropertiesBindingTest {
    private static final String DISPLAY_NAME_KEY = "APP_BOOTSTRAP_ADMIN_DISPLAY_NAME";

    @Test
    void bindsKoreanDisplayNameFromIsolatedUtf8Dotenv(@TempDir Path temporaryDirectory) throws IOException {
        var dotenvPath = temporaryDirectory.resolve(".env");
        Files.writeString(dotenvPath, DISPLAY_NAME_KEY + "=시스템 관리자\n", StandardCharsets.UTF_8);

        contextRunner(dotenvPath).run(context -> {
            var dotenvPropertySource = dotenvPropertySource(context.getEnvironment());

            assertThat(dotenvPropertySource.getName()).contains(dotenvPath.toString());
            assertThat(dotenvPropertySource.getProperty(DISPLAY_NAME_KEY)).isEqualTo("시스템 관리자");
            assertThat(context.getBean(AdminBootstrapProperties.class).getDisplayName())
                    .isEqualTo("시스템 관리자");
        });
    }

    @Test
    void letsJvmPropertiesOverrideIsolatedDotenv(@TempDir Path temporaryDirectory) throws IOException {
        var dotenvPath = temporaryDirectory.resolve(".env");
        Files.writeString(dotenvPath, DISPLAY_NAME_KEY + "=시스템 관리자\n", StandardCharsets.UTF_8);

        contextRunner(dotenvPath, Map.of(DISPLAY_NAME_KEY, "JVM 관리자")).run(context -> {
            var dotenvPropertySource = dotenvPropertySource(context.getEnvironment());

            assertThat(dotenvPropertySource.getName()).contains(dotenvPath.toString());
            assertThat(dotenvPropertySource.getProperty(DISPLAY_NAME_KEY)).isEqualTo("시스템 관리자");
            assertThat(context.getEnvironment().getProperty(DISPLAY_NAME_KEY))
                    .isEqualTo("JVM 관리자");
            assertThat(context.getBean(AdminBootstrapProperties.class).getDisplayName())
                    .isEqualTo("JVM 관리자");
        });
    }

    @Test
    void retainsKoreanDefaultWhenIsolatedDotenvIsMissing(@TempDir Path temporaryDirectory) {
        contextRunner(temporaryDirectory.resolve(".env")).run(context ->
                assertThat(context.getBean(AdminBootstrapProperties.class).getDisplayName())
                        .isEqualTo("시스템 관리자"));
    }

    private ApplicationContextRunner contextRunner(Path dotenvPath) {
        return contextRunner(dotenvPath, Map.of());
    }

    private ApplicationContextRunner contextRunner(Path dotenvPath, Map<String, Object> systemProperties) {
        return new ApplicationContextRunner(() -> applicationContext(dotenvPath, systemProperties))
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withUserConfiguration(ApplicationConfig.class, Utf8DotenvConfig.class);
    }

    private AnnotationConfigApplicationContext applicationContext(
            Path dotenvPath, Map<String, Object> systemProperties) {
        var context = new AnnotationConfigApplicationContext();
        var environment = new StandardEnvironment();
        var propertySources = environment.getPropertySources();
        propertySources.remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        propertySources.remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        if (!systemProperties.isEmpty()) {
            propertySources.addFirst(new MapPropertySource(
                    StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME, systemProperties));
        }
        context.setEnvironment(environment);
        context.setResourceLoader(new DotenvResourceLoader(new FileSystemResource(dotenvPath)));
        return context;
    }

    private PropertySource<?> dotenvPropertySource(ConfigurableEnvironment environment) {
        return StreamSupport.stream(environment.getPropertySources().spliterator(), false)
                .filter(ResourcePropertySource.class::isInstance)
                .filter(propertySource -> propertySource.containsProperty(DISPLAY_NAME_KEY))
                .findFirst()
                .orElseThrow();
    }

    private static final class DotenvResourceLoader extends DefaultResourceLoader {
        private final Resource dotenvResource;

        private DotenvResourceLoader(Resource dotenvResource) {
            this.dotenvResource = dotenvResource;
        }

        @Override
        public Resource getResource(String location) {
            if (location.equals("file:.env")) {
                return dotenvResource;
            }
            return super.getResource(location);
        }
    }
}
