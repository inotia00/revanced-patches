import java.lang.Boolean.FALSE

plugins {
    id(libs.plugins.android.library.get().pluginId)
}

android {
    namespace = "com.eclipsesource.v8"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }

    buildTypes {
        release {
            isMinifyEnabled = FALSE

            // 'libj2v8.so' is already included in the patch.
            ndk {
                abiFilters.add("")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
