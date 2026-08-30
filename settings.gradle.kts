pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { mavenCentral() }
}

rootProject.name = "ino-admin"

include("apps:admin-server")
include("features:identity")
include("features:menu")
include("features:file-management")
include("modules:common-core")
include("modules:common-web")
include("modules:common-security")
include("modules:common-file")
include("modules:common-audit")
include("modules:common-excel")
include("samples:common-modules-consumer")
