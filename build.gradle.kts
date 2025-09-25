// ruta: /build.gradle.kts

plugins {
    // Asegúrate de tener la versión más reciente o la misma que tus librerías
    id("com.google.dagger.hilt.android") version "2.51.1" apply false
    id("com.android.application") version "8.4.1" apply false // CORREGIDO: 8.13.0 no es una versión válida
    id("org.jetbrains.kotlin.android") version "1.9.23" apply false
    id("com.google.devtools.ksp") version "1.9.23-1.0.19" apply false // AÑADIR ESTA LÍNEA
}