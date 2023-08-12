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
        mavenCentral()
    }
//    versionCatalogs {
//        create("libs") {
//            from(files("./secure-pomerium-tunneler/gradle/libs.versions.toml"))
//        }
//    }
}
rootProject.name = "Pomerium tunneler"
include(":app")
include("tunneler")
project(":tunneler").projectDir = file("secure-pomerium-tunneler/tunneler")
