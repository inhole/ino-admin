import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService

plugins {
    base
}

val buildVersion = providers.gradleProperty("releaseVersion").orElse("0.1.0-SNAPSHOT").get()
check(Regex("^\\d+\\.\\d+\\.\\d+(?:-SNAPSHOT)?$").matches(buildVersion)) {
    "releaseVersion must use MAJOR.MINOR.PATCH or MAJOR.MINOR.PATCH-SNAPSHOT: $buildVersion"
}

allprojects {
    group = "com.ino.admin"
    version = buildVersion
}

val verifyModuleDependencies = tasks.register("verifyModuleDependencies") {
    group = "verification"
    description = "Verifies modular-monolith project dependency direction."

    doLast {
        val legacyFeatureProjects = allprojects.filter { it.path.startsWith(":features:") }
        check(legacyFeatureProjects.isEmpty()) {
            "Business features must live in apps/admin-server packages, not separate Gradle projects: " +
                legacyFeatureProjects.joinToString { it.path }
        }
        allprojects.forEach { source ->
            source.configurations
                .flatMap { configuration -> configuration.dependencies.withType(ProjectDependency::class.java) }
                .map { dependency -> dependency.path }
                .distinct()
                .forEach { target ->
                    val commonViolation = source.path.startsWith(":modules:common-") &&
                        (target.startsWith(":apps:") || target.startsWith(":features:"))
                    val consumerFixtureViolation = source.path == ":samples:common-modules-consumer" &&
                        !target.startsWith(":modules:common-")

                    if (commonViolation || consumerFixtureViolation) {
                        throw GradleException("Forbidden module dependency: ${source.path} -> $target")
                    }
                }
        }
    }
}

val commonModuleNames = listOf(
    "common-core",
    "common-web",
    "common-security",
    "common-file",
    "common-file-s3",
    "common-audit",
    "common-excel",
)

subprojects {
    if (path.startsWith(":modules:common-") && name in commonModuleNames) {
        apply(plugin = "maven-publish")
        plugins.withId("java") {
            extensions.configure<PublishingExtension> {
                publications.create<MavenPublication>("mavenJava") {
                    from(components["java"])
                    versionMapping {
                        usage("java-api") { fromResolutionOf("runtimeClasspath") }
                        usage("java-runtime") { fromResolutionResult() }
                    }
                    pom {
                        name.set(project.name)
                        description.set("Reusable INO Admin ${project.name} module")
                    }
                }
                repositories.maven {
                    name = "staging"
                    url = rootProject.uri(rootProject.layout.buildDirectory.dir("staging-repository").get().asFile)
                }
            }
        }
    }
}

tasks.register("verifyCommonModulePublications") {
    group = "verification"
    description = "Verifies common module JARs and POMs in the repository-local staging repository."
    dependsOn(commonModuleNames.map { ":modules:$it:publishMavenJavaPublicationToStagingRepository" })
    dependsOn(":samples:common-modules-consumer:verifyNoAwsSdk")

    doLast {
        val repository = layout.buildDirectory.dir("staging-repository/com/ino/admin").get().asFile
        commonModuleNames.forEach { moduleName ->
            val versionDirectory = repository.resolve("$moduleName/${project.version}")
            val jar = versionDirectory.listFiles()
                ?.filter { it.name.startsWith("$moduleName-") && it.extension == "jar" }
                ?.maxByOrNull { it.lastModified() }
            val pom = versionDirectory.listFiles()
                ?.filter { it.name.startsWith("$moduleName-") && it.extension == "pom" }
                ?.maxByOrNull { it.lastModified() }
            check(jar?.isFile == true) { "Missing staged JAR in $versionDirectory" }
            check(pom?.isFile == true) { "Missing staged POM in $versionDirectory" }
            val pomText = requireNotNull(pom).readText()
            val forbiddenArtifacts = listOf("admin-server", "identity", "menu", "file-management")
            check(forbiddenArtifacts.none { "<artifactId>$it</artifactId>" in pomText }) {
                "Forbidden app or feature dependency in $pom"
            }
            if (moduleName == "common-file") {
                check("software.amazon.awssdk" !in pomText) {
                    "common-file must not expose AWS SDK dependencies in $pom"
                }
            }
            if (moduleName == "common-excel") {
                val poiDependency = Regex("<dependency>.*?<artifactId>poi-ooxml</artifactId>.*?</dependency>", RegexOption.DOT_MATCHES_ALL)
                    .find(pomText)?.value
                check(poiDependency != null && "<scope>runtime</scope>" in poiDependency) {
                    "common-excel must keep Apache POI as a runtime implementation detail in $pom"
                }
                val fixtureRoot = layout.buildDirectory.dir("artifact-consumer/common-excel").get().asFile
                val source = fixtureRoot.resolve("src/ArtifactConsumer.java")
                val classes = fixtureRoot.resolve("classes")
                source.parentFile.mkdirs()
                classes.mkdirs()
                source.writeText(
                    """
                    import com.ino.admin.excel.io.XlsxCell;
                    import com.ino.admin.excel.io.XlsxReadOptions;
                    import com.ino.admin.excel.io.XlsxTableReader;
                    import com.ino.admin.excel.io.XlsxTableWriter;
                    import com.ino.admin.excel.io.XlsxWriteOptions;
                    import java.util.List;

                    final class ArtifactConsumer {
                        XlsxTableReader reader = new XlsxTableReader();
                        XlsxTableWriter writer = new XlsxTableWriter();
                        XlsxReadOptions read = new XlsxReadOptions(List.of("Value"), 1);
                        XlsxWriteOptions write = new XlsxWriteOptions("Sheet", List.of("Value"));
                        XlsxCell cell = XlsxCell.text("value");
                    }
                    """.trimIndent()
                )
                val toolchains = project(":modules:common-excel").extensions.getByType(JavaToolchainService::class.java)
                val compiler = toolchains.compilerFor { languageVersion.set(JavaLanguageVersion.of(25)) }.get()
                providers.exec {
                    commandLine(compiler.executablePath.asFile.absolutePath, "-classpath", jar.absolutePath,
                        "-d", classes.absolutePath, source.absolutePath)
                }.result.get().assertNormalExitValue()
            }
        }
    }
}
