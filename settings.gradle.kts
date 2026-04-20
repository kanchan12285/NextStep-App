pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS) // This line enforces the settings repositories
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "NextStep" // Ensure your root project name is correct here
include(":app") // Include your app module
