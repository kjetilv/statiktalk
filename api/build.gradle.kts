import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation("com.github.navikt:rapids-and-rivers:unspecified")
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}
