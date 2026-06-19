plugins {
    kotlin("jvm") version "1.9.23"
    application
}

group = "com.example.vetfinance"
version = "1.0.0"

application {
    mainClass.set("com.example.vetfinance.vpc.MainKt")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "17"
}

tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = "17"
    targetCompatibility = "17"
}

dependencies {
    implementation("org.xerial:sqlite-jdbc:3.45.3.0")
    implementation("org.apache.commons:commons-csv:1.11.0")
    implementation("com.formdev:flatlaf:3.4.1")
    implementation("org.slf4j:slf4j-simple:2.0.13")

    testImplementation(kotlin("test-junit5"))
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
}

tasks.test {
    useJUnitPlatform()
}
