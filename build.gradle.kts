import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.10"
    application
}

group = "com.amarland"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.apache.xmlgraphics:batik-transcoder:1.14")
    implementation("com.github.ajalt.clikt:clikt:3.5.0")
    implementation("org.sejda.imageio:webp-imageio:0.1.6")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
}

@Suppress("PropertyName")
val _mainClassName = "com.amarland.androidvectorrasterizer.Main"

application {
    mainClass.set(_mainClassName)
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = _mainClassName
    }
}
