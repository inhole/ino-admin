plugins {
    java
    id("io.spring.dependency-management") version "1.1.7"
}

java { toolchain { languageVersion = JavaLanguageVersion.of(25) } }
tasks.withType<JavaCompile>().configureEach { options.release = 25 }

dependencyManagement {
    imports { mavenBom("org.springframework.boot:spring-boot-dependencies:4.1.0") }
}

dependencies {
    implementation(project(":modules:common-core"))
    implementation(project(":modules:common-web"))
    implementation(project(":modules:common-security"))
    implementation(project(":modules:common-file"))
    implementation(project(":modules:common-audit"))
    implementation(project(":modules:common-excel"))
    implementation("org.springframework.boot:spring-boot-starter-web")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test>().configureEach { useJUnitPlatform() }

tasks.register("verifyNoAwsSdk") {
    group = "verification"
    description = "Verifies that the local-only common-file consumer does not resolve AWS SDK."

    doLast {
        listOf("compileClasspath", "runtimeClasspath").forEach { configurationName ->
            val awsComponents = configurations.getByName(configurationName)
                .incoming.resolutionResult.allComponents
                .mapNotNull { it.moduleVersion }
                .filter { it.group == "software.amazon.awssdk" }
            check(awsComponents.isEmpty()) {
                "Local-only consumer $configurationName must not contain AWS SDK: $awsComponents"
            }
        }
    }
}
