import org.gradle.api.artifacts.ProjectDependency

plugins {
    base
}

tasks.register("verifyModuleDependencies") {
    group = "verification"
    description = "Verifies that application projects do not introduce forbidden project dependencies."

    doLast {
        allprojects.forEach { source ->
            source.configurations
                .flatMap { configuration -> configuration.dependencies.withType(ProjectDependency::class.java) }
                .map { dependency -> dependency.path }
                .distinct()
                .forEach { target ->
                    check(!target.startsWith(":modules:common-")) {
                        "Common modules must be consumed from ino-spring-modules artifacts: ${source.path} -> $target"
                    }
                }
        }
    }
}
