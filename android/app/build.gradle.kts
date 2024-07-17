// Helped make sense of kotlin DSL for rust plugin with https://github.com/MarijnS95/AndroidVulkanInterop/blob/master/app/build.gradle.kts
plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
//    id("org.mozilla.rust-android-gradle.rust-android")
}

// Clear these when rebuilding by putting them in the build directory
// Path to the by uniffi generated kotlin bindings code
val bindingsOutDirectory = layout.buildDirectory.dir("generated/source/java").get().asFile.path
// Path to directory that stores the libmelt.so for different architectures
val librariesDirectory = layout.buildDirectory.dir("rustJniLibs").get().asFile.path

android {
    namespace = "dev.claas.melo"
    compileSdk = 34

    defaultConfig {
        applicationId = "dev.claas.melo"
        minSdk = 34
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    ndkVersion = "26.3.11579264"
    sourceSets {
//        main.java.srcDirs += "${layout.buildDirectory}/generated/source/uniffi/java"
        named("main") {
            java.srcDirs(bindingsOutDirectory)
            jniLibs.srcDirs(librariesDirectory)
        }
    }
}

dependencies {
//    implementation(libs.jna)
    implementation("net.java.dev.jna:jna:5.13.0@aar")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}


tasks.register<Exec>("cargoBuild") {
    //    cargo ndk
    //    --target aarch64-linux-android
    //    --target armv7-linux-androideabi
    //    --target i686-linux-android
    //    --target x86_64-linux-android
    //    --platform 26
    //    --output-dir ./shared/generated/rust/jniLibs -- build

    workingDir = File(project.projectDir.parent)
    val platform = android.ndkVersion.substringBefore('.')
    // Prevent stripping so we can generate the uniffi bindings with the included data
    //TODO optimize later and strip?
    commandLine(
        "sh",
        "-c",
        "cargo ndk --target aarch64-linux-android --target armv7-linux-androideabi --target i686-linux-android --target x86_64-linux-android --platform $platform --output-dir $librariesDirectory --no-strip --  build --package meltcore",
    )
}

tasks.register<Exec>("generateUniFfiBindings") {
    workingDir = project.projectDir
    dependsOn("cargoBuild")

    // We only need one variant like "arm64-v8a" to generate the Kotlin bindings code
//    val libraryPath = "${layout.buildDirectory.asFile}/rustJniLibs/android/arm64-v8a/libmeltcore.so"

    // Since we are using the uniffi procedural macros in rust we can't use an .udl file to generate
    // the binding code and need to point to a library to read the metadata and generate the binding
    // code. The target architecture in this case does not matter. There should be a better way than
    // needing to know where the file is located for binding generation without .udl file
    val libraryFile = File(librariesDirectory).resolve("arm64-v8a/libmeltcore.so").path

    ///Users/claas/Developer/melo/app/build/rustJniLibs/android/arm64-v8a/libmeltcore.so
        ///Users/claas/Developer/melo/app/build/rustJniLibs/android/arm64-v8a/libmeltcore.so
//        layout.buildDirectory.file("rustJniLibs/android/arm64-v8a/libmeltcore.so").get().asFile.path
//    val outDirectory = layout.buildDirectory.dir("generated/source/uniffi/java").get().asFile.path
//    val outDirectory = layout.projectDirectory.dir("generated/java").asFile.path

    commandLine(
        "sh",
        "-c",
        "cargo run --package uniffi-bindgen generate --library $libraryFile --language kotlin --out-dir $bindingsOutDirectory"
    )

//    sourceSets {
//        named("main") {
//            java.srcDirs(outDirectory)
//        }
//    }
//        sourceSets["main"].java.srcDirs.add(File(outDirectory))

}


//tasks.whenTaskAdded {
//    if (name == "mergeDebugJniLibFolders" || name == "mergeReleaseJniLibFolders" ) {
//        dependsOn("cargoBuild")
//    }
//}

//tasks.whenObjectAdded {
//    if ((this.name == "mergeDebugJniLibFolders" || this.name == "mergeReleaseJniLibFolders")) {
//        this.dependsOn("cargoBuild")
//        // fix mergeDebugJniLibFolders  UP-TO-DATE
//        this.inputs.dir(buildDir.resolve("rustJniLibs/android"))
//    }
//}
//tasks.matching { it.name.matches(/merge.*JniLibFolders/) }.configureEach {
//    it.inputs.dir(new File(buildDir, "rustJniLibs/android"))
//    it.dependsOn("cargoBuild")
//}

tasks.whenTaskAdded {
    if (name == "javaPreCompileDebug" || name == "javaPreCompileRelease") {
        dependsOn("generateUniFfiBindings")
    }
}
