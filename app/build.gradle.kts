import org.jetbrains.kotlin.config.KotlinCompilerVersion

plugins {
    id("com.android.application")
    id("dagger.hilt.android.plugin")
    kotlin("android")
    id("kotlin-parcelize")
    kotlin("kapt")
}

android {
    compileSdk = 32

    defaultConfig {
        applicationId = "com.lookaround"
        minSdk = 23
        targetSdk = 32
        version = "1.0"
        versionCode = 1

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    packagingOptions { resources { excludes += "META-INF/DEPENDENCIES" } }

    buildFeatures { compose = true }

    composeOptions { kotlinCompilerExtensionVersion = "1.2.0-alpha07" }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures { viewBinding = true }
}

kapt { correctErrorTypes = true }

dependencies {
    implementation(project(":core"))
    implementation(project(":core-android"))
    implementation(project(":repo-nominatim"))
    implementation(project(":repo-overpass"))
    implementation(project(":repo-photon"))
    implementation(project(":tangram"))
    implementation(project(":ui-about"))
    implementation(project(":ui-camera"))
    implementation(project(":ui-main"))
    implementation(project(":ui-map"))
    implementation(project(":ui-place"))
    implementation(project(":ui-place-list"))
    implementation(project(":ui-place-map-list"))
    implementation(project(":ui-place-categories"))
    implementation(project(":ui-recent-searches"))
    implementation(project(":ui-settings"))

    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation(kotlin("stdlib", KotlinCompilerVersion.VERSION))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")

    implementation("androidx.appcompat:appcompat:1.4.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.3")
    implementation("com.google.android.material:material:1.6.0")
    implementation("com.kirich1409.viewbindingpropertydelegate:vbpd-noreflection:1.4.1")
    implementation("com.google.accompanist:accompanist-insets:0.20.1")
    implementation("com.google.accompanist:accompanist-coil:0.15.0")
    implementation("io.github.hokofly:hoko-blur:1.3.7")
    implementation("com.github.xcc3641:ExViewPagerBottomSheet:1.3")
    implementation("androidx.palette:palette:1.0.0")

    implementation("androidx.compose.runtime:runtime:1.2.0-alpha07")
    implementation("androidx.compose.compiler:compiler:1.2.0-alpha07")
    implementation("androidx.compose.ui:ui:1.2.0-alpha07")
    implementation("androidx.compose.ui:ui-tooling:1.2.0-alpha07")
    implementation("androidx.compose.foundation:foundation:1.2.0-alpha07")
    implementation("androidx.compose.material:material:1.2.0-alpha07")
    implementation("androidx.compose.material:material-icons-core:1.2.0-alpha07")
    implementation("androidx.compose.material:material-icons-extended:1.2.0-alpha07")

    implementation("androidx.core:core-ktx:1.7.0")
    implementation("androidx.fragment:fragment-ktx:1.3.6")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.4.0")

    implementation("com.google.dagger:hilt-android:2.40")
    kapt("com.google.dagger:hilt-android-compiler:2.40")
    kapt("androidx.hilt:hilt-compiler:1.0.0")
    implementation("androidx.hilt:hilt-lifecycle-viewmodel:1.0.0-alpha03")

    implementation("com.squareup.okhttp3:logging-interceptor:4.9.1")

    implementation("ru.beryukhov:flowreactivenetwork:1.0.2")

    implementation("com.google.android.gms:play-services-location:19.0.1")
    implementation("com.github.jintin:FancyLocationProvider:2.0.0")

    implementation("org.permissionsdispatcher:permissionsdispatcher:4.8.0")
    kapt("org.permissionsdispatcher:permissionsdispatcher-processor:4.8.0")

    implementation("com.jakewharton.timber:timber:4.7.1")

    implementation("com.dropbox.mobile.store:store4:4.0.4-KT15")
    implementation("com.google.code.gson:gson:2.8.6")
    implementation("androidx.room:room-runtime:2.3.0")
    kapt("androidx.room:room-compiler:2.3.0")
    implementation("androidx.room:room-ktx:2.3.0")

    implementation("com.squareup.moshi:moshi:1.9.2")
    implementation("com.squareup.moshi:moshi-adapters:1.9.2")
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.1")
    implementation("com.squareup.retrofit2:converter-moshi:2.6.2")
    implementation("com.squareup.retrofit2:converter-gson:2.7.1")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")

    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.8.1")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
}
