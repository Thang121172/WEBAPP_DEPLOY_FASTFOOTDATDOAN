pluginManagement {
    plugins {
        // Khai báo rõ version cho các plugin sử dụng trong dự án
        id("com.android.application") version "8.5.2"
        id("androidx.navigation.safeargs") version "2.7.7"
    }

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

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "APP"
include(":app")
