import org.jetbrains.kotlin.config.KotlinCompilerVersion

plugins {
    id("com.android.library")
    id("dagger.hilt.android.plugin")
    kotlin("android")
    id("kotlin-parcelize")
    kotlin("kapt")
}

android {
    compileSdk = 32

    defaultConfig {
        minSdk = 28
        targetSdk = 32
        version = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")

        buildConfigField(
            "Boolean",
            "LOG_STATES_FLOW",
            if (project.hasProperty("log.statesFlow")) {
                project.properties["log.statesFlow"] as String
            } else {
                System.getenv("LOG_STATES_FLOW")
            }
        )

        buildConfigField(
            "Boolean",
            "LOG_STATES_UPDATES_FLOW",
            if (project.hasProperty("log.stateUpdatesFlow")) {
                project.properties["log.stateUpdatesFlow"] as String
            } else {
                System.getenv("LOG_STATE_UPDATES_FLOW")
            }
        )

        externalNativeBuild {
            cmake {
                cppFlags("-std=c++17")
                arguments("-DCMAKE_VERBOSE_MAKEFILE=ON")
            }
        }
    }

    buildTypes {
        named("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "$project.rootDir/tools/proguard-rules.pro"
            )
        }
    }

    externalNativeBuild {
        cmake {
            // AGP doesn't allow us to use project.buildDir (or subdirs) for CMake's generated
            // build files (ninja build files, CMakeCache.txt, etc.). Use a staging directory that
            // lives alongside the project's buildDir.
            buildStagingDirectory = File("${project.buildDir}/../nativeBuildStaging")
            path = File("${project.buildDir}/../src/main/cpp/CMakeLists.txt")
            version = "3.10.2"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions { jvmTarget = "1.8" }

    buildFeatures {
        viewBinding = true
        compose = true
    }

    composeOptions { kotlinCompilerExtensionVersion = "1.2.0-beta02" }
}

kapt { correctErrorTypes = true }

dependencies {
    implementation(project(":core"))
    implementation(project(":tangram"))

    implementation(kotlin("stdlib", KotlinCompilerVersion.VERSION))
    implementation("androidx.compose.runtime:runtime:1.2.0-beta02")
    implementation("androidx.compose.compiler:compiler:1.2.0-beta02")
    implementation("androidx.compose.ui:ui:1.2.0-beta02")
    implementation("androidx.compose.ui:ui-tooling:1.2.0-beta02")
    implementation("androidx.compose.foundation:foundation:1.2.0-beta02")
    implementation("androidx.compose.material:material:1.2.0-beta02")
    implementation("androidx.compose.material:material-icons-core:1.2.0-beta02")
    implementation("androidx.compose.material:material-icons-extended:1.2.0-beta02")
    implementation("androidx.palette:palette:1.0.0")
    implementation("com.google.accompanist:accompanist-insets:0.20.1")
    implementation("androidx.core:core-splashscreen:1.0.0-rc01")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")
    implementation("androidx.core:core-ktx:1.7.0")
    implementation("androidx.fragment:fragment-ktx:1.3.6")
    implementation("androidx.concurrent:concurrent-futures:1.1.0")
    implementation("androidx.preference:preference:1.2.0")

    implementation("androidx.navigation:navigation-fragment-ktx:2.3.5")
    implementation("androidx.navigation:navigation-ui-ktx:2.3.5")

    implementation("androidx.appcompat:appcompat:1.4.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.3")
    implementation("com.google.android.material:material:1.6.0")
    implementation("com.facebook.shimmer:shimmer:0.5.0")
    implementation("biz.laenger.android:vpbs:0.0.6")
    implementation("io.github.hokofly:hoko-blur:1.3.7")

    implementation("androidx.lifecycle:lifecycle-viewmodel-savedstate:2.4.0")
    implementation("androidx.lifecycle:lifecycle-runtime:2.4.0")
    implementation("androidx.lifecycle:lifecycle-common-java8:2.4.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.4.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.4.0")

    implementation("com.jakewharton:disklrucache:2.0.2")
    implementation(files("../libs/bitmap-lru-cache.jar"))

    implementation("com.squareup.okhttp3:okhttp:4.9.1")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.1")

    implementation("ru.beryukhov:flowreactivenetwork:1.0.2")

    kapt("androidx.room:room-compiler:2.3.0")
    implementation("androidx.room:room-ktx:2.3.0")

    implementation("com.google.dagger:hilt-android:2.40")
    kapt("com.google.dagger:hilt-android-compiler:2.40")
    kapt("androidx.hilt:hilt-compiler:1.0.0")
    implementation("androidx.hilt:hilt-lifecycle-viewmodel:1.0.0-alpha03")

    implementation("androidx.camera:camera-camera2:1.2.0-alpha04")
    implementation("androidx.camera:camera-lifecycle:1.2.0-alpha04")
    implementation("androidx.camera:camera-view:1.2.0-alpha04")

    implementation("com.google.android.gms:play-services-location:19.0.1")
    implementation("com.github.jintin:FancyLocationProvider:2.0.0")

    implementation("com.jakewharton.timber:timber:4.7.1")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
}
