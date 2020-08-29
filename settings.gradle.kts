pluginManagement {
    val kotlinVersion: String by settings
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id.contains("org.jetbrains.kotlin")) {
                useVersion(kotlinVersion)
            }
        }
    }
}
rootProject.name = "vlcj-terminal"
