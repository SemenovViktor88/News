plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.semenov.news.features.home.data"
    compileSdk {
        version = release(37)
    }
    defaultConfig {
        minSdk = 29
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(project(":features:home:domain"))
    implementation(project(":core:domain"))
    implementation(project(":core:network:data"))
    implementation(project(":core:db"))
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
