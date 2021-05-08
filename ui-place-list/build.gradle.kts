plugins {
    id("com.android.library")
    id("dagger.hilt.android.plugin")
    kotlin("android")
    id("kotlin-parcelize")
    kotlin("kapt")
}

android {
    compileSdk = 30

    defaultConfig {
        minSdk = 21
        targetSdk = 30
        version = "1.0"

        buildConfigField(
            "String",
            "NEXTZEN_API_KEY",
            if (project.hasProperty("nextzen.apiKey")) {
                "\"${project.properties["nextzen.apiKey"] as String}\""
            } else {
                System.getenv("NEXTZEN_API_KEY")
            }
        )

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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

    buildFeatures { compose = true }

    composeOptions { kotlinCompilerExtensionVersion = "1.0.0-beta03" }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
        useIR = true
    }

    buildFeatures { viewBinding = true }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":core-android"))
    implementation(project(":core-android-model"))
    implementation(project(":ui-main"))

    implementation(kotlin("stdlib", org.jetbrains.kotlin.config.KotlinCompilerVersion.VERSION))
    implementation("androidx.appcompat:appcompat:1.3.0-rc01")
    implementation("androidx.constraintlayout:constraintlayout:2.0.4")
    implementation("com.google.android.material:material:1.3.0")
    implementation("dev.chrisbanes.accompanist:accompanist-insets:0.6.0")
    implementation("dev.chrisbanes.accompanist:accompanist-coil:0.6.0")
    implementation("com.kirich1409.viewbindingpropertydelegate:vbpd-noreflection:1.4.1")

    implementation("androidx.core:core-ktx:1.3.2")
    implementation("androidx.fragment:fragment-ktx:1.3.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.3.1")

    implementation("com.jakewharton:disklrucache:2.0.2")
    implementation(files("../libs/bitmap-lru-cache.jar"))
    implementation("com.mapzen.tangram:tangram:0.13.0")

    implementation("com.google.dagger:hilt-android:2.35")
    kapt("com.google.dagger:hilt-android-compiler:2.35")
    kapt("androidx.hilt:hilt-compiler:1.0.0")
    implementation("androidx.hilt:hilt-lifecycle-viewmodel:1.0.0-alpha03")

    implementation("androidx.compose.runtime:runtime:1.0.0-beta03")
    implementation("androidx.compose.compiler:compiler:1.0.0-beta03")
    implementation("androidx.compose.ui:ui:1.0.0-beta03")
    implementation("androidx.compose.ui:ui-tooling:1.0.0-beta03")
    implementation("androidx.compose.foundation:foundation:1.0.0-beta03")
    implementation("androidx.compose.material:material:1.0.0-beta03")
    implementation("androidx.compose.material:material-icons-core:1.0.0-beta03")
    implementation("androidx.compose.material:material-icons-extended:1.0.0-beta03")

    implementation("com.jakewharton.timber:timber:4.7.1")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.2")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.3.0")
}
