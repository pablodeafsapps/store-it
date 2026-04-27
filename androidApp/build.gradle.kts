plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.android.junit)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.detekt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "org.deafsapps.storeit.androidapp"
    compileSdk = 36

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
}

configurations.matching { it.name.contains("androidTest", ignoreCase = true) }.configureEach {
    resolutionStrategy.eachDependency {
        if (requested.group == "androidx.compose.ui") {
            useVersion(libs.versions.compose.ui.get())
            because("Align transitive Compose UI from android-test-compose with project")
        }
    }
}

dependencies {
    implementation(projects.shared)
    implementation(project.dependencies.platform(libs.firebase.bom))
    implementation(libs.firebase.common)
    implementation(libs.koin.androidx.compose)
    implementation(libs.koin.annotations)
    ksp(libs.koin.ksp.compiler)

    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.coil.compose)
    debugImplementation(libs.compose.uiTooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.junit.jupiter.api)
    androidTestRuntimeOnly(libs.junit.jupiter.engine)
    androidTestImplementation(libs.mannodermaus.android.test.compose)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}

ksp {
    arg("KOIN_USE_COMPOSE_VIEWMODEL", "true")
    arg("KOIN_CONFIG_CHECK", "true")
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom(file("${rootProject.projectDir}/config/detekt/detekt.yml"))
    baseline = file("${rootProject.projectDir}/config/detekt/baseline.xml")
}
