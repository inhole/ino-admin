plugins { `java-library` }

java { toolchain { languageVersion = JavaLanguageVersion.of(25) } }
tasks.withType<JavaCompile>().configureEach { options.release = 25 }

dependencies {
    api("org.apache.poi:poi-ooxml:5.4.1")
    testImplementation("org.junit.jupiter:junit-jupiter:6.0.3")
    testImplementation("org.assertj:assertj-core:3.27.7")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.0.3")
}
tasks.withType<Test>().configureEach { useJUnitPlatform() }
