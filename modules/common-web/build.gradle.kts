plugins {
    `java-library`
    id("io.spring.dependency-management") version "1.1.7"
}

java {
    toolchain { languageVersion = JavaLanguageVersion.of(25) }
}

tasks.withType<JavaCompile>().configureEach { options.release = 25 }

dependencies {
    api(project(":modules:common-core"))
    implementation("org.springframework:spring-webmvc:7.0.6")
    implementation("jakarta.servlet:jakarta.servlet-api:6.1.0")
    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("org.springframework.boot:spring-boot-autoconfigure:4.1.0")

    testImplementation("org.junit.jupiter:junit-jupiter:6.0.3")
    testImplementation("org.assertj:assertj-core:3.27.7")
    testImplementation("org.springframework:spring-test:7.0.6")
    testImplementation("org.springframework.boot:spring-boot-test:4.1.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.0.3")
    testRuntimeOnly("ch.qos.logback:logback-classic:1.5.18")
}

tasks.withType<Test>().configureEach { useJUnitPlatform() }
