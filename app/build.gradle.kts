plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.helpme"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.helpme"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }


    buildTypes {
        release {
            // signingConfig = signingConfigs.release
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
//            ndk {
//                abiFilters += listOf("arm64-v8a")
//            }
        }
    }

    // signingConfigs {
    //     release {
    //         storeFile file("Keystore file")
    //         storePassword "shadyali"
    //         keyAlias "key0"
    //         keyPassword "shadyali"
    //     }
    // }

    splits {
        abi {
            isEnable = true
            reset()
            // Only include arm64-v8a to decrease APK size.
            include("arm64-v8a")
            isUniversalApk = false
        }
    }
    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    // ...existing config...
}

dependencies {
    // Core Android dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    
    // Compose dependencies - use api to expose only necessary components
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui) {
        exclude(group = "androidx.compose.animation")
        exclude(module = "animation-core")
    }
    implementation(libs.androidx.ui.graphics)
    runtimeOnly(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3) {
        exclude(group = "androidx.compose.animation")
    }
    
    // PyTorch dependencies - ultra minimal configuration
    implementation(libs.pytorch.android) {
        exclude(group = "com.facebook.soloader", module = "nativeloader")
        exclude(group = "com.facebook.soloader", module = "annotation")
        exclude(group = "com.facebook.soloader", module = "soloader")
        exclude(group = "com.android.support", module = "support-annotations")
        exclude(group = "org.pytorch", module = "pytorch_android_torchvision")
        exclude(group = "org.pytorch", module = "pytorch_android_vision")
        exclude(group = "org.pytorch", module = "pytorch_android_fbjni")
        exclude(group = "com.android.support", module = "appcompat-v7")
        exclude(group = "com.android.support", module = "support-v4")
        exclude(group = "org.pytorch", module = "pytorch_android_build")
    }
    implementation(libs.pytorch.android.torchvision) {
        exclude(group = "org.pytorch", module = "pytorch_android")
        exclude(group = "com.facebook.soloader", module = "nativeloader")
        exclude(group = "com.facebook.soloader", module = "annotation")
        exclude(group = "org.pytorch", module = "pytorch_android_vision")
        exclude(group = "com.android.support", module = "support-v4")
        exclude(group = "com.android.support", module = "appcompat-v7")
        exclude(group = "org.pytorch", module = "pytorch_android_fbjni")
        exclude(group = "org.pytorch", module = "pytorch_android_build")
    }
    
    // UI dependencies - use api to expose only necessary components
    implementation(libs.androidx.appcompat) {
        exclude(group = "androidx.lifecycle", module = "lifecycle-viewmodel")
    }
    implementation(libs.androidx.cardview)
    implementation(libs.androidx.constraintlayout.v221)
    implementation(libs.material) {
        exclude(group = "androidx.recyclerview", module = "recyclerview")
        exclude(group = "androidx.transition", module = "transition")
    }
}