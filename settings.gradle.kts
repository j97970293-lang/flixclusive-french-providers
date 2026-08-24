pluginManagement {
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "flx-provider") {
                useModule("com.github.flixclusiveorg:core-gradle:${requested.version}")
            }
        }
    }
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://jitpack.io")
        mavenLocal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
        mavenLocal()
    }
}

rootProject.name = "flixclusive-french-providers"

include("FrenchStream")

rootProject.children.forEach {
    it.projectDir = file("providers/${it.name}")
}
