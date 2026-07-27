plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.scannerapp"
    compileSdk =37
    defaultConfig {
        applicationId = "com.example.scannerapp"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)


    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("com.google.zxing:core:3.5.3")



        implementation("androidx.camera:camera-core:1.4.2")
        implementation("androidx.camera:camera-camera2:1.4.2")
        implementation("androidx.camera:camera-lifecycle:1.4.2")
        implementation("androidx.camera:camera-view:1.4.2")

        implementation("com.google.mlkit:barcode-scanning:17.3.0")




}