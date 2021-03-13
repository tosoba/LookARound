import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()

plugins {
    id("java-library")
    id("kotlin")
    id("kotlin-kapt")
    id("net.ltgt.apt") version "0.15"
}

apply(plugin = "net.ltgt.apt-idea")

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

kapt { correctErrorTypes = true }

dependencies {
    implementation(project(":core"))

    implementation(kotlin("stdlib", KotlinCompilerVersion.VERSION))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")

    implementation("org.mapstruct:mapstruct:1.4.1.Final")
    kapt("org.mapstruct:mapstruct-processor:1.4.1.Final")

    implementation("com.google.dagger:dagger:2.31.2")
    kapt("com.google.dagger:dagger-compiler:2.31.2")

    implementation("fr.dudie:nominatim-api:3.4")

    implementation("com.squareup.okhttp3:logging-interceptor:4.9.0")

    testImplementation("junit:junit:4.13.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.4.2")
}
