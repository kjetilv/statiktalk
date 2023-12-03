import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    kotlin("jvm")
    id("com.google.devtools.ksp") version "1.9.20-1.0.14"
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
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.20")

    implementation(project(":api"))
    implementation(project(":processor"))

    implementation("com.github.navikt:rapids-and-rivers:unspecified")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.1")

    testImplementation("org.testcontainers:kafka:1.19.1")
    testImplementation("org.awaitility:awaitility:4.2.0")

    ksp(project(":processor"))
}


kotlin {
    jvmToolchain {
        this.languageVersion.set(JavaLanguageVersion.of(17))
    }
}
