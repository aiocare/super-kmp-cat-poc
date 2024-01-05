pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/aiocare/*")
            credentials {
                username = readSecrets("username")
                password = readSecrets("token")
            }
        }
    }
}

fun readSecrets(name: String): String {
    val propertiesPath = File("$rootDir", "secrets.properties")
    if (!propertiesPath.isFile) {
        error("You need to create a local 'secrets.properties' file. Check the README for more details.")
    }
    return propertiesPath.readLines().first { it.startsWith(name) }.split("=")[1]
}

rootProject.name = "super-kmp-cat-poc"
include(":super-cat")
include(":example")
include(":examination")
