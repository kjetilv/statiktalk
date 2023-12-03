plugins {
    kotlin("jvm")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":api"))
    implementation("org.antlr:ST4:4.3.4")
    implementation("com.google.devtools.ksp:symbol-processing-api:1.9.20-1.0.14")
}


kotlin {
    jvmToolchain {
        this.languageVersion.set(JavaLanguageVersion.of(17))
    }
}
