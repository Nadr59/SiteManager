plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.nadr59.sitemanager"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.nadr59.sitemanager"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "2.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {

    // ═══════════════════════════════════════
    // ═══ Core Android ═══
    // ═══════════════════════════════════════
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-compose:1.9.3")

    // ═══════════════════════════════════════
    // ═══ Compose BOM ═══
    // ═══════════════════════════════════════
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // ═══════════════════════════════════════
    // ═══ Lifecycle + ViewModel + Compose ═══
    // ═══════════════════════════════════════
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")

    // ═══════════════════════════════════════
    // ═══ Navigation Compose ═══
    // ═══════════════════════════════════════
    implementation("androidx.navigation:navigation-compose:2.8.3")

    // ═══════════════════════════════════════
    // ═══ Hilt ═══
    // ═══════════════════════════════════════
    implementation("com.google.dagger:hilt-android:2.52")
    ksp("com.google.dagger:hilt-compiler:2.52")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // ═══════════════════════════════════════
    // ═══ Room ═══
    // ═══════════════════════════════════════
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // ═══════════════════════════════════════
    // ═══ Network ═══
    // ═══════════════════════════════════════
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // ═══════════════════════════════════════
    // ═══ JSON ═══
    // ═══════════════════════════════════════
    implementation("com.google.code.gson:gson:2.11.0")

    // ═══════════════════════════════════════
    // ═══ HTML Parsing ═══
    // ═══════════════════════════════════════
    implementation("org.jsoup:jsoup:1.18.1")

    // ═══════════════════════════════════════
    // ═══ Coroutines ═══
    // ═══════════════════════════════════════
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // ═══════════════════════════════════════
    // ═══ DataStore ═══
    // ═══════════════════════════════════════
    implementation("androidx.datastore:datastore-preferences:1.1.1")
}
