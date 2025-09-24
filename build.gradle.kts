// ruta: /build.gradle.kts (el que está en la carpeta raíz del proyecto)

plugins {
    // Asegúrate de tener la versión más reciente o la misma que tus librerías
    id("com.google.dagger.hilt.android") version "2.51.1" apply false
    id("com.android.application") version "8.13.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.23" apply false
}