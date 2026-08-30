import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication

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
        }
    }
}
