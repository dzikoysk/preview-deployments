package com.dzikoysk.preview

import com.charleskorn.kaml.Yaml
import com.dzikoysk.preview.PreviewConfig.General
import kotlinx.serialization.encodeToString
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import kotlin.io.path.absolute

fun main() {
    val workingDirectory = Files.createTempDirectory("preview").absolute()
    val configFile = workingDirectory.resolve("preview.yml")
    val configContent = Yaml.default.encodeToString(
        PreviewConfig(
            general = General(
                webhookPort = 8090,
                portRange = "8091-8099",
                workingDirectory = workingDirectory.toString(),
                nginxConfig = workingDirectory.resolve("nginx").toString(),
            ),
            services = mapOf(
                "example" to PreviewConfig.Service(
                    startCommands = listOf("echo 'Hello world!'"),
                    stopCommands = listOf("echo 'Goodbye world!'"),
                )
            )
        )
    )
    Files.writeString(configFile, configContent, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)
    val app = FeaturePreviewApp()
    app.start(configFile)
}