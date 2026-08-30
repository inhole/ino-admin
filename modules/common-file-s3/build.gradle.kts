plugins {
    `java-library`
    id("io.spring.dependency-management") version "1.1.7"
}

java { toolchain { languageVersion = JavaLanguageVersion.of(25) } }
tasks.withType<JavaCompile>().configureEach { options.release = 25 }

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:4.1.0")
        mavenBom("software.amazon.awssdk:bom:2.46.11")
    }
}

dependencies {
    api(project(":modules:common-file"))
    api("software.amazon.awssdk:s3:2.46.11")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.springframework.boot:spring-boot-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test>().configureEach { useJUnitPlatform() }
