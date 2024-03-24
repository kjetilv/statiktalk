plugins {
    kotlin("jvm") version "1.9.23" apply false
}

buildscript {
    dependencies {
        classpath(kotlin("gradle-plugin", version = "1.9.23"))
    }
}
