plugins {
    java
    id("io.spring.dependency-management") version "1.1.7"
}

val stagingRepository = providers.gradleProperty("stagingRepository").get()
val commonModulesVersion = providers.gradleProperty("commonModulesVersion").get()

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
    implementation("com.ino.admin:common-core:$commonModulesVersion")
    implementation("com.ino.admin:common-web:$commonModulesVersion")
    implementation("com.ino.admin:common-security:$commonModulesVersion")
    implementation("com.ino.admin:common-file:$commonModulesVersion")
    implementation("com.ino.admin:common-file-s3:$commonModulesVersion")
    implementation("com.ino.admin:common-audit:$commonModulesVersion")
    implementation("com.ino.admin:common-excel:$commonModulesVersion")
    implementation("org.springframework.boot:spring-boot-starter-web")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

configurations.configureEach {
    resolutionStrategy.dependencySubstitution.all {
        check(requested !is org.gradle.api.artifacts.component.ProjectComponentSelector) {
            "The staged consumer must not resolve project dependencies: $requested"
        }
    }
}

tasks.withType<Test>().configureEach { useJUnitPlatform() }
