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

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
}

application {
    mainClass.set("Main")
}
