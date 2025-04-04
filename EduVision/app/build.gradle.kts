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

    packagingOptions {
        pickFirst("lib/**/libc++_shared.so")
    }
}

// Add a custom task to verify assets
tasks.register("validateAssets") {
    doLast {
        val assetsDir = file("src/main/assets")
        val requiredFiles = mapOf(
            "encoder_traced.ptl" to 1024L * 1024, // Minimum 1MB
            "decoder_traced.ptl" to 1024L * 1024, // Minimum 1MB
            "tokenizer.json" to 1024L // Minimum 1KB
        )
        
        requiredFiles.forEach { (fileName, minSize) ->
            val file = File(assetsDir, fileName)
            if (!file.exists()) {
                throw GradleException("Required asset missing: $fileName")
            }
            if (file.length() < minSize) {
                throw GradleException("Asset file too small: $fileName (${file.length()} bytes)")
            }
            println("Validated asset: $fileName (${file.length()} bytes)")
        }
    }
}

tasks.named("preBuild") {
    dependsOn("validateAssets")
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("org.pytorch:pytorch_android:2.1.0")
    implementation("org.pytorch:pytorch_android_torchvision:2.1.0")
    implementation("androidx.camera:camera-core:1.3.1")
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("com.github.yalantis:ucrop:2.2.8") {
        exclude(group = "com.android.support")
    }
}