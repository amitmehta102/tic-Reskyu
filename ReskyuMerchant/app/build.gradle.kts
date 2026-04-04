plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
}

// Read GEMINI_API_KEY from local.properties using line-based parsing
// (java.util.Properties is not reliably available in KTS scripts)
fun readLocalProperty(key: String): String {
    val f = rootProject.file("local.properties")
    if (!f.exists()) return ""
    return f.readLines()
        .firstOrNull { it.startsWith("$key=") }
        ?.substringAfter("=")
        ?.trim() ?: ""
}
val geminiApiKey: String = readLocalProperty("GEMINI_API_KEY")


android {
    namespace = "com.reskyu.merchant"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.reskyu.merchant"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Inject Gemini API key from local.properties into BuildConfig
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
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
    buildFeatures {
        compose = true
        buildConfig = true   // enables BuildConfig.GEMINI_API_KEY
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)


    // Firebase (BOM manages all Firebase library versions)
    implementation(platform("com.google.firebase:firebase-bom:33.12.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")

    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.8.9")

    // Lifecycle ViewModel Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")

    // Material Icons Extended (Rounded icons for bottom nav + dashboard)
    implementation("androidx.compose.material:material-icons-extended")

    // OkHttp (used by CloudinaryUploadService)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Google Play Services — Location (FusedLocationProviderClient for GPS)
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // MPAndroidChart (used by EsgAnalyticsScreen weekly bar chart)
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Coil — image loading library (AsyncImage in PostListingScreen thumbnail)
    implementation("io.coil-kt:coil-compose:2.6.0")

    // WorkManager — background listing expiry (runs every 15 min even when app is closed)
    implementation("androidx.work:work-runtime-ktx:2.9.1")
}