import java.lang.Boolean.TRUE

extension {
    name = "extensions/all/misc/signature/spoof-signature.rve"
}

android {
    namespace = "app.revanced.extension"
    compileSdk = 35

    defaultConfig {
        minSdk = 21
    }

    buildTypes {
        release {
            isMinifyEnabled = TRUE
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.hiddenapi)
}
