plugins {
    kotlin("jvm") version "2.1.21"
    id("com.gradleup.shadow") version "8.3.6"
}

group = "win.templeos"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))

    implementation("com.github.javaparser:javaparser-symbol-solver-core:3.26.4")
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "main.kotlin.MainKt"
        )
    }
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}