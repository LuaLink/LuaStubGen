plugins {
    kotlin("jvm") version "2.1.10"
}

group = "win.templeos"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))

    implementation("com.github.javaparser:javaparser-core:3.26.4")
    implementation("com.github.javaparser:javaparser-symbol-solver-core:3.26.4")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}