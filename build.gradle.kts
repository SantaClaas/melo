// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    //TODO might need to set to 8.3.2 if mozilla rust android plugin does not work
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.jetbrainsKotlinAndroid) apply false
    id("org.mozilla.rust-android-gradle.rust-android") version "0.9.4" apply false
}