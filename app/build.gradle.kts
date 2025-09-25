plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp") // AÑADIR ESTA LÍNEA
}

android {
    namespace = "com.example.vetfinance"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.vetfinance"
        minSdk = 26
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // ---- Core y Lifecycle KTX ----
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // ---- Jetpack Compose ----
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)


    // ---- Hilt (Inyección de Dependencias) ----
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)     // CAMBIO: Agrega esta línea
    implementation(libs.hilt.navigation.compose)

    // ---- Navigation y Componentes de UI ----
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.material.icons.extended)


    // ---- Room (Base de Datos Local) ----
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)      // CAMBIO: Agrega esta línea
    implementation(libs.room.ktx)
    implementation(libs.room.paging)

    // ---- Paging (Paginación de datos) ----
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)

    // ---- Gráficos ----
    implementation(libs.ycharts)

    // ---- Calendario ----
    implementation(libs.calendar.compose)

    //---CSV---//
    implementation("org.apache.commons:commons-csv:1.11.0")

    //----Desugaring---//
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    // ---- Testing ----
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)


    implementation("androidx.work:work-runtime-ktx:2.9.0")
}