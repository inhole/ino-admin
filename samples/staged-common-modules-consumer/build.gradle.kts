import org.gradle.api.attributes.Usage

plugins {
    java
    id("io.spring.dependency-management") version "1.1.7"
}

val stagingRepository = providers.gradleProperty("stagingRepository").get()
val commonModulesVersion = providers.gradleProperty("commonModulesVersion").get()
val commonModuleNames = listOf(
    "common-core", "common-web", "common-security", "common-file",
    "common-file-s3", "common-audit", "common-excel",
)
val minimalConsumerConfigurations = commonModuleNames.associateWith { moduleName ->
    configurations.create("${moduleName.replace("-", "_")}MinimalConsumer") {
        isCanBeConsumed = false
        isCanBeResolved = true
        attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_API))
    }
}

repositories {
    maven { url = uri(stagingRepository) }
    mavenCentral()
}

java { toolchain { languageVersion = JavaLanguageVersion.of(25) } }
tasks.withType<JavaCompile>().configureEach { options.release = 25 }

dependencyManagement {
    imports { mavenBom("org.springframework.boot:spring-boot-dependencies:4.1.0") }
}

dependencies {
    commonModuleNames.forEach { moduleName ->
        implementation("com.ino.admin:$moduleName:$commonModulesVersion")
    }
    implementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    minimalConsumerConfigurations.forEach { (moduleName, configuration) ->
        add(configuration.name, "com.ino.admin:$moduleName:$commonModulesVersion")
    }
}

configurations.configureEach {
    resolutionStrategy.dependencySubstitution.all {
        check(requested !is org.gradle.api.artifacts.component.ProjectComponentSelector) {
            "The staged consumer must not resolve project dependencies: $requested"
        }
    }
}

tasks.withType<Test>().configureEach { useJUnitPlatform() }

tasks.register("verifyMinimalCommonModuleConsumers") {
    group = "verification"
    description = "Verifies that each common module exposes only its intended transitive dependencies."

    doLast {
        val forbiddenGroups = mapOf(
            "common-core" to listOf("org.springframework", "software.amazon", "org.apache.poi"),
            "common-web" to listOf("org.springframework.security", "software.amazon", "org.apache.poi"),
            "common-security" to listOf("software.amazon", "org.apache.poi"),
            "common-file" to listOf("org.springframework.security", "software.amazon", "org.apache.poi"),
            "common-file-s3" to listOf("org.springframework.security", "org.apache.poi"),
            "common-audit" to listOf("org.springframework", "software.amazon", "org.apache.poi"),
            "common-excel" to listOf("org.springframework", "software.amazon", "org.apache.poi"),
        )

        minimalConsumerConfigurations.forEach { (moduleName, configuration) ->
            val resolvedDependencies = configuration.incoming.resolutionResult.allComponents
                .mapNotNull { it.moduleVersion }
            check(resolvedDependencies.any { it.group == "com.ino.admin" && it.name == moduleName }) {
                "$moduleName minimal consumer did not resolve its staged artifact: $resolvedDependencies"
            }
            val forbidden = resolvedDependencies.filter { dependency ->
                forbiddenGroups.getValue(moduleName).any { prefix -> dependency.group.startsWith(prefix) }
            }
            check(forbidden.isEmpty()) {
                "$moduleName minimal consumer exposes forbidden API dependencies: $forbidden"
            }
        }

        val s3Dependencies = minimalConsumerConfigurations.getValue("common-file-s3")
            .incoming.resolutionResult.allComponents.mapNotNull { it.moduleVersion }
        check(s3Dependencies.any { it.group == "software.amazon.awssdk" && it.name == "s3" }) {
            "common-file-s3 minimal consumer must expose the AWS S3 SDK contract: $s3Dependencies"
        }
    }
}
