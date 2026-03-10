plugins {
    id(libs.plugins.android.library.get().pluginId)
}

android {
    namespace = "app.revanced.extension"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
