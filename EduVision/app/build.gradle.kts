plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.image2latex"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.image2latex"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Add permission for writing to external storage
        manifestPlaceholders["appAuthRedirectScheme"] = "com.example.image2latex"
    }

    sourceSets {
        getByName("main") {
            assets {
                srcDirs("src/main/assets")
            }
        }
    }

    buildFeatures {
        viewBinding = true
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    // Fix deprecated packaging calls
    packaging {
        jniLibs {
            pickFirsts.add("lib/**/libc++_shared.so")
        }
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    
    // HTTP client for API communication
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    
    // Image processing and UI components
    implementation("androidx.camera:camera-core:1.3.1")
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("com.github.yalantis:ucrop:2.2.8") {
        exclude(group = "com.android.support")
    }
    
    // Add HTML export support
    implementation("androidx.webkit:webkit:1.7.0")
    
    // Improved UI components
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("com.google.android.material:material:1.11.0")
}