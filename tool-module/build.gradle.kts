plugins {
    kotlin("plugin.serialization") version "1.9.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

application {
    mainClass.set("com.dzikoysk.preview.MainKt")
}

dependencies {
    implementation("com.charleskorn.kaml:kaml:0.54.0")
    implementation("io.javalin:javalin-bundle:5.6.1")
    implementation("org.apache.commons:commons-exec:1.3")
}