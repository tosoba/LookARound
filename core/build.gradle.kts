import org.jetbrains.kotlin.config.KotlinCompilerVersion

plugins {
    id("java-library")
    id("kotlin")
    id("kotlin-kapt")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    implementation(kotlin("stdlib", KotlinCompilerVersion.VERSION))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")

    implementation("com.squareup.okhttp3:logging-interceptor:4.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.9.0")
    implementation("com.squareup.retrofit2:retrofit:2.7.1")

    implementation("com.google.dagger:dagger:2.31")
    kapt("com.google.dagger:dagger-compiler:2.31")
}