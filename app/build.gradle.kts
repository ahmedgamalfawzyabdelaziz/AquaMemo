plugins {
    // هذه هي الـ plugins الأساسية لمشروع Android يستخدم Kotlin و View System
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
}
android {
    namespace = "com.ahmedgamal.aquamemo" // تأكد من اسم الـ namespace ده مطابق لمشروعك
    compileSdk = 35 // أو أحدث إصدار متاح عندك
    defaultConfig {
        applicationId = "com.ahmedgamal.aquamemo"
        minSdk = 26 // تأكد إن ده هو أقل إصدار بيدعمه تطبيقك
        targetSdk = 35 // أو أحدث إصدار متاح عندك
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
        // لو بتستخدم Data Binding أو View Binding، خلي السطر ده true
        // viewBinding = true
        // dataBinding = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}
dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation("com.google.android.material:material:1.12.0") // أو 1.13.0 لو متاح
    implementation("androidx.appcompat:appcompat:1.7.1") // أو أحدث إصدار متاح
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.activity)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(platform("com.google.firebase:firebase-bom:33.16.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")
}