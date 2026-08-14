import org.gradle.api.artifacts.ProjectDependency

plugins {
    base
}

allprojects {
    group = "com.ino.admin"
    version = "0.1.0-SNAPSHOT"
}

val verifyModuleDependencies by tasks.registering {
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

                    if (commonViolation || featureViolation) {
                        throw GradleException("Forbidden module dependency: ${source.path} -> $target")
                    }
                }
        }
    }
}
