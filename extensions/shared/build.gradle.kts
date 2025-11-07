import java.lang.Boolean.TRUE

plugins {
    alias(libs.plugins.protobuf)
}

extension {
    name = "extensions/shared.rve"
}

android {
    namespace = "app.revanced.extension"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }

    buildTypes {
        release {
            isMinifyEnabled = TRUE

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

dependencies {
    compileOnly(libs.annotation)
    compileOnly(libs.preference)

    implementation(libs.collections4)
    implementation(libs.disklrucache)
    implementation(libs.gson)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.lang3)
    implementation(libs.nanojson)
    implementation(libs.okhttp3)
    implementation(libs.protobuf.javalite)

    implementation(libs.regex)
    implementation(libs.retrofit)
    implementation(libs.rxjava2)
    implementation(libs.rxjava2.android)
    implementation(project(":extensions:shared:j2v8"))

    coreLibraryDesugaring(libs.desugar.jdk.libs)
    compileOnly(project(":extensions:shared:stub"))
}

protobuf {
    protoc {
        artifact = libs.protobuf.protoc.get().toString()
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create("java") {
                    option("lite")
                }
            }
        }
    }
}
