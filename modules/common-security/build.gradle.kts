plugins {
    `java-library`
    id("io.spring.dependency-management") version "1.1.7"
}

java { toolchain { languageVersion = JavaLanguageVersion.of(25) } }
tasks.withType<JavaCompile>().configureEach { options.release = 25 }

dependencyManagement {
    imports { mavenBom("org.springframework.boot:spring-boot-dependencies:4.1.0") }
}

dependencies {
    api("org.springframework.security:spring-security-oauth2-jose")
    api("org.springframework.security:spring-security-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-autoconfigure")

    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.springframework.boot:spring-boot-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test>().configureEach { useJUnitPlatform() }
