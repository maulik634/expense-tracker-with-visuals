pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        // These two lines are essential for finding the chart library
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
rootProject.name = "det"
include(":app")
