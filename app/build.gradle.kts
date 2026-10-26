plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

val baseUrl = libs.versions.baseUrl.get()
val apiKey = libs.versions.apiKey.get()

android {
    namespace = "com.semenov.news"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.semenov.news"
        minSdk = 29
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "BASE_URL", "\"$baseUrl\"")
        buildConfigField("String", "API_KEY", "\"$apiKey\"")
    }

    buildTypes {
        debug {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(":screens:home"))
    implementation(project(":screens:categories"))
    implementation(project(":screens:settings"))
    implementation(project(":features:news:data"))
    implementation(project(":features:auth:domain"))
    implementation(project(":features:auth:data"))
    implementation(project(":core:domain"))
    implementation(project(":core:storage:data"))
    implementation(project(":core:logging"))
    implementation(project(":core:ui:mvi:domain"))
    implementation(project(":core:ui:mvi:presentation"))
    implementation(project(":core:ui:interactions:domain"))
    implementation(project(":core:ui:interactions:data"))
    implementation(project(":core:ui:interactions:presentation"))
    implementation(project(":core:ui:navigation:domain"))
    implementation(project(":core:ui:navigation:presentation"))
    implementation(project(":core:ui:designsystem"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.hilt.android)
    implementation(libs.timber)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    debugImplementation(project(":core:storage:domain"))
    debugImplementation(project(":core:db"))
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(project(":core:storage:domain"))
    androidTestImplementation(project(":core:db"))
    androidTestImplementation(libs.room.ktx)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
