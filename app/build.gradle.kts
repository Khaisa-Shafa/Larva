plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("kotlin-parcelize")
    id("androidx.navigation.safeargs.kotlin")
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.android.libraries.mapsplatform.secrets.gradle.plugin)
}

android {
    namespace = "com.dicoding.skripsiapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.dicoding.skripsiapp"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    buildFeatures{
        viewBinding = true
        mlModelBinding = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    //navigation component
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    //hilt + firebase
    implementation(platform("com.google.firebase:firebase-bom:34.9.0"))
    implementation(libs.hilt.android)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.tensorflow.lite)
    implementation(libs.tensorflow.lite.support)
    implementation(libs.tensorflow.lite.metadata)
    implementation(libs.tensorflow.lite.gpu)
    implementation(libs.tensorflow.lite.task.vision)
    implementation(libs.core.ktx)
    implementation(libs.core.ktx)
    implementation(libs.androidx.core.core.ktx)
    implementation(libs.androidx.core.core.ktx)
//    implementation(project(":libuvc"))
//    implementation(libs.core.ktx)
    kapt(libs.hilt.android.compiler)

//    implementation 'org.tensorflow:tensorflow-lite:2.14.0'
//    implementation 'org.tensorflow:tensorflow-lite-support:0.4.4'
//    implementation 'org.tensorflow:tensorflow-lite-metadata:0.4.4'

    //firebase
    implementation(libs.firebase.auth)

    //loading button
    implementation(libs.loading.button.android)

    //Glide
    implementation(libs.glide)

    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    //circular image
    implementation(libs.circleimageview)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.firebase.appcheck.playintegrity)

    //datastore
    implementation(libs.androidx.datastore.preferences)

    //coil
    implementation(libs.coil)
    implementation(libs.coil.compose)
    implementation(libs.shimmer)

    //swipe refresh
    implementation(libs.androidx.swiperefreshlayout)

    //gms
    implementation(libs.play.services.base)
    implementation(libs.play.services.location)
    implementation(libs.play.services.maps)

    //retrofit
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.gson)

    //testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    //room
    implementation(libs.androidx.room.runtime)
    kapt (libs.androidx.room.compiler)
    implementation (libs.androidx.room.ktx)

    //uvc
//    implementation (project(":libausbc"))

    //camera
    implementation (libs.androidx.camera.lifecycle)
    implementation (libs.androidx.camera.view)
    implementation (libs.androidx.camera.core)
    implementation (libs.androidx.camera.camera2)
    implementation (libs.androidx.camera.video)

//    implementation (libs.androidx.concurrent.futures)
    implementation("androidx.concurrent:concurrent-futures:1.1.0")
    implementation (libs.androidx.concurrent.futures.ktx)

    implementation (libs.androidx.core)
    implementation (libs.androidx.core.ktx)

    implementation(libs.guava)
    implementation(libs.androidx.concurrent.futures)


}

// Allow references to generated code
kapt {
    correctErrorTypes = true
}

configurations.all {
    resolutionStrategy {
//        force (libs.androidx.camera.core)
//        force (libs.androidx.camera.camera2)
//        force (libs.androidx.camera.lifecycle)
//        force (libs.androidx.camera.view)
//        force (libs.androidx.camera.video)

        // ✅ TAMBAH INI:
        force (libs.androidx.core)
        force (libs.androidx.core.ktx)
    }

    exclude(group = "com.google.ai.edge.litert", module = "litert-api")
    exclude(group = "com.google.ai.edge.litert", module = "litert-support-api")
}