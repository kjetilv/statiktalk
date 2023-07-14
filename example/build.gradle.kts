import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp") version "1.9.0-1.0.11"
}

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://packages.confluent.io/maven/")
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("skipped", "failed")
        showExceptions = true
        showStackTraces = true
        showCauses = true
        exceptionFormat = TestExceptionFormat.FULL
        showStandardStreams = true
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    implementation(project(":api"))
    implementation(project(":processor"))

    implementation("com.github.navikt:rapids-and-rivers:unspecified")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.3")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.9.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.3")

    testImplementation("org.testcontainers:kafka:1.18.3")
    testImplementation("org.awaitility:awaitility:4.2.0")

    ksp(project(":processor"))
}
