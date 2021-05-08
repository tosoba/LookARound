buildscript {
    repositories {
        google()
        jcenter()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:4.2.0")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.4.32")
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.35")
    }
}

allprojects {
    repositories {
        google()
        jcenter()
        maven("https://jitpack.io")
        mavenCentral()
    }
}

tasks.register<Delete>("clean").configure { delete(rootProject.buildDir) }
