import org.gradle.api.artifacts.ProjectDependency

plugins {
    base
}

allprojects {
    group = "com.ino.admin"
    version = "0.1.0-SNAPSHOT"
}

val verifyModuleDependencies = tasks.register("verifyModuleDependencies") {
    group = "verification"
    description = "Verifies modular-monolith project dependency direction."

    doLast {
        allprojects.forEach { source ->
            source.configurations
                .flatMap { configuration -> configuration.dependencies.withType(ProjectDependency::class.java) }
                .map { dependency -> dependency.path }
                .distinct()
                .forEach { target ->
                    val commonViolation = source.path.startsWith(":modules:common-") &&
                        (target.startsWith(":apps:") || target.startsWith(":features:"))
                    val featureViolation = source.path.startsWith(":features:") &&
                        (target.startsWith(":apps:") ||
                            (target.startsWith(":features:") && target != source.path))
                    val consumerFixtureViolation = source.path == ":samples:common-modules-consumer" &&
                        !target.startsWith(":modules:common-")

                    if (commonViolation || featureViolation || consumerFixtureViolation) {
                        throw GradleException("Forbidden module dependency: ${source.path} -> $target")
                    }
                }
        }
    }
}
