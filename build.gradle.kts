// Top-level build file where you can add configuration options common to all sub-projects/modules.
buildscript {
    repositories {
        google()
        jcenter()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:7.0.0-alpha06")
        classpath(kotlin("gradle-plugin", version = "1.4.21-2"))
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.31.2-alpha")
    }
}

allprojects {
    repositories {
        google()
        jcenter()
    }
}

tasks.register<Delete>("clean").configure {
    delete(rootProject.buildDir)
}
