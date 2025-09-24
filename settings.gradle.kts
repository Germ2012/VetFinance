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
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"

}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {google()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            // Define versiones
            version("compose", "1.5.0") // Puedes nombrar la clave de la versión como quieras

            // Define librerías
            library("compose-ui", "androidx.compose.ui", "ui").versionRef("compose")
            library("compose-material", "androidx.compose.material", "material").versionRef("compose")
            // De
    }
}

rootProject.name = "VetFinance"
include(":app")
}