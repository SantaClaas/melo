plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
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
        named("main") {
            java.srcDirs(bindingsOutDirectory)
            jniLibs.srcDirs(librariesDirectory)
        }
    }
}

dependencies {
    implementation(libs.jna) {
        artifact {
            type = "aar"
        }
    }
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

    // Run in repository root
    workingDir = project.projectDir.resolve("../..")
    val platform = android.ndkVersion.substringBefore('.')
    // Prevent stripping so we can generate the uniffi bindings with the included data
    //TODO optimize for release build and enable stripping to reduce size after we created bindings
    commandLine(
        "sh",
        "-c",
        "cargo ndk --target aarch64-linux-android --target armv7-linux-androideabi --target i686-linux-android --target x86_64-linux-android --platform $platform --output-dir $librariesDirectory --no-strip --  build --package meltcore",
    )
}

tasks.register<Exec>("generateUniFfiBindings") {
    workingDir = project.projectDir
    dependsOn("cargoBuild")

    // Since we are using the uniffi procedural macros in rust we can't use an .udl file to generate
    // the binding code and need to point to a library to read the metadata and generate the binding
    // code.
    // The target architecture in this case does not matter we only need one.
    // There should be a better way than needing to know where the file is located for binding
    // generation without .udl file
    val libraryFile = File(librariesDirectory).resolve("arm64-v8a/libmeltcore.so").path

    commandLine(
        "sh",
        "-c",
        "cargo run --package uniffi-bindgen generate --library $libraryFile --language kotlin --out-dir $bindingsOutDirectory"
    )
}


tasks.whenTaskAdded {
    if (name == "javaPreCompileDebug" || name == "javaPreCompileRelease") {
        dependsOn("generateUniFfiBindings")
    }
}
