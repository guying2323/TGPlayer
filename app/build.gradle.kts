import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}

android {
    namespace = "com.tgplayer.android"
    compileSdk = 37

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    // Only arm64-v8a (armV8) is packaged, unless -PtmAbis=... overrides it.
    val packagedAbis: List<String> = (findProperty("tmAbis") as String?)
        ?.split(",")
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?: listOf("arm64-v8a")

    defaultConfig {
        applicationId = "com.tgplayer.android"
        minSdk = 26
        targetSdk = 35
        versionCode = 30
        versionName = "0.22.0"

        buildConfigField("int", "TG_API_ID", localProps.getProperty("TG_API_ID") ?: "0")
        buildConfigField("String", "TG_API_HASH", "\"${localProps.getProperty("TG_API_HASH") ?: ""}\"")

        ndk {
            abiFilters += packagedAbis
        }
    }

    val keystorePropsFile = rootProject.file("keystore.properties")
    val keystoreProps = Properties().apply {
        if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
    }

    signingConfigs {
        if (keystorePropsFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            if (keystorePropsFile.exists()) signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += setOf("META-INF/*.version", "kotlin/**", "DebugProbesKt.bin")
        jniLibs.useLegacyPackaging = false
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.tdl.coroutines)
    implementation(libs.zxing.core)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.nextlib.media3ext)
    implementation(libs.androidx.work)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
