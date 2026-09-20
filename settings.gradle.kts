pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "News"

include(":app")

include(":core:domain")
include(":core:network:domain")
include(":core:network:data")
include(":core:db")
include(":core:ui:mvi:domain")
include(":core:ui:mvi:presentation")
include(":core:ui:navigation:domain")
include(":core:ui:navigation:presentation")
include(":core:ui:designsystem")

include(":features:news:domain")
include(":features:news:data")

include(":screens:home")
include(":screens:categories")
include(":core:logging")
