// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false

}
buildscript {
    dependencies {
        classpath("com.google.gms:google-services:4.4.0")
    }
}