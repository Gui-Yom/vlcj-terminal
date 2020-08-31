plugins {
    kotlin("jvm")
    application
    id("com.github.johnrengelman.shadow") version "6.0.0"
    id("com.github.ben-manes.versions") version "0.29.0"
}

group = "tech.guiyom"
version = "1.4.0"

repositories {
    mavenLocal()
    mavenCentral()
    jcenter()
    // JitPack
    maven {
        setUrl("https://jitpack.io")
    }
}

dependencies {
    val kotlinVersion: String by project
    val vlcjVersion: String by project
    val jlineVersion: String by project

    implementation(platform(kotlin("bom", kotlinVersion)))
    implementation(kotlin("stdlib-jdk8"))

    implementation("org.jline:jline-terminal:$jlineVersion")
    implementation("org.jline:jline-terminal-jansi:$jlineVersion")
    implementation("com.github.Gui-Yom:anscapes:0.11.0")
    implementation("uk.co.caprica:vlcj:$vlcjVersion")
}

application {
    mainClassName = "tech.guiyom.nggyu.MainKt"
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = JavaVersion.VERSION_11.toString()
            //javaParameters = true
            //freeCompilerArgs = listOf("-Xemit-jvm-type-annotations")
        }
    }

    withType(JavaCompile::class).configureEach {
        options.encoding = "UTF-8"
    }

    shadowJar {
        mergeServiceFiles()
        //minimize()
    }

    dependencyUpdates {
        resolutionStrategy {
            componentSelection {
                all {
                    if (isNonStable(candidate.version) && !isNonStable(currentVersion)) {
                        reject("Release candidate")
                    }
                }
            }
        }
        checkConstraints = true
        gradleReleaseChannel = "current"
    }
}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.toUpperCase().contains(it) }
    val regex = "^[0-9,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}
