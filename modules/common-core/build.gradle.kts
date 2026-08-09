plugins {
    `java-library`
}

java {
    toolchain { languageVersion = JavaLanguageVersion.of(25) }
}

tasks.withType<JavaCompile>().configureEach { options.release = 25 }

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
