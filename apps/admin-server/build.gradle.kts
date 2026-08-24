import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
    java
    id("org.springframework.boot") version "4.1.0"
    id("io.spring.dependency-management") version "1.1.7"
}

tasks.register("verifyBootRunWorkingDirectory") {
    description = "Verifies that bootRun resolves file:.env from the repository root."
    group = "verification"

    doLast {
        val bootRun = tasks.named<BootRun>("bootRun").get()
        check(bootRun.workingDir.toPath().toAbsolutePath().normalize() ==
            rootProject.projectDir.toPath().toAbsolutePath().normalize()) {
            "bootRun must use the repository root as its working directory so file:.env resolves README's root .env"
        }
    }
}

java {
    toolchain { languageVersion = JavaLanguageVersion.of(25) }
}

tasks.withType<JavaCompile>().configureEach { options.release = 25 }

tasks.named<BootRun>("bootRun") {
    workingDir = rootProject.projectDir
}

dependencies {
    implementation(project(":features:identity"))
    implementation(project(":features:menu"))
    implementation(project(":features:file-management"))
    implementation(project(":modules:common-core"))
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-security-oauth2-resource-server")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("software.amazon.awssdk:s3:2.46.11")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    systemProperty("app.jwt.secret", "MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=")
}

tasks.named<Test>("test") {
    useJUnitPlatform { excludeTags("integration", "architecture") }
    dependsOn("verifyBootRunWorkingDirectory")
}

tasks.register<Test>("integrationTest") {
    description = "Runs integration tests."
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform { includeTags("integration") }
    shouldRunAfter(tasks.test)
}

tasks.register<Test>("architectureTest") {
    description = "Runs architecture tests."
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform { includeTags("architecture") }
    shouldRunAfter(tasks.test)
    dependsOn(rootProject.tasks.named("verifyModuleDependencies"))
}
