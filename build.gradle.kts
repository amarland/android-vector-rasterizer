plugins {
    kotlin("jvm") version "1.8.20"
    application
}

group = "com.amarland"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.apache.xmlgraphics:batik-transcoder:1.16")
    implementation("com.github.ajalt.clikt:clikt:3.5.2")
    implementation("org.sejda.imageio:webp-imageio:0.1.6")
    implementation("org.jetbrains:annotations:24.0.1")

    testImplementation(platform("org.junit:junit-bom:5.9.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testImplementation("com.google.jimfs:jimfs:1.2")
}

tasks.test {
    useJUnitPlatform()
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
