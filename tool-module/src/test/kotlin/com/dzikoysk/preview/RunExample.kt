package com.dzikoysk.preview

import com.dzikoysk.preview.config.Credentials
import com.dzikoysk.preview.config.PreviewConfig
import com.dzikoysk.preview.config.PreviewConfig.General
import com.dzikoysk.preview.config.PreviewConfig.Service
import com.dzikoysk.preview.config.YamlConfig
import kotlinx.serialization.encodeToString
import java.nio.file.Files
import java.nio.file.StandardOpenOption
import kotlin.io.path.absolute

fun main() {
    val workingDirectory = Files.createTempDirectory("preview").absolute()
    val configFile = workingDirectory.resolve("preview.yml")
    val configContent = YamlConfig.default.encodeToString(
        PreviewConfig(
            general = General(
                portRange = "8091-8099",
                workingDirectory = workingDirectory.toString(),
                nginxConfig = workingDirectory.resolve("nginx").toString(),
            ),
            services = mapOf(
                "example" to Service(
                    startCommands = listOf("echo 'Hello world!'"),
                    stopCommands = listOf("echo 'Goodbye world!'"),
                )
            )
        )
    )
    Files.writeString(configFile, configContent, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)

    FeaturePreviewApp().start(
        port = 8090,
        credentials = Credentials(
            username = "admin",
            password = "admin"
        ),
        configFile = configFile
    )
}