plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(11)
}

dependencies {
    api(libs.androidx.annotation)
    api(libs.kotlinx.coroutines.core)
}
