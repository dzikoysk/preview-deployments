package com.dzikoysk.preview.config

import java.nio.file.Files
import java.nio.file.Path

class ConfigService(private val configPath: Path) {

    private var config: PreviewConfig
    private val listeners = mutableListOf<(PreviewConfig) -> Unit>()

    init {
        this.config = YamlConfig.default.decodeFromString(
            deserializer = PreviewConfig.serializer(),
            string = Files.readString(configPath)
        )
    }

    fun subscribe(listener: (PreviewConfig) -> Unit) {
        listeners.add(listener)
    }

    fun updateConfig(content: String) {
        this.config = YamlConfig.default.decodeFromString(PreviewConfig.serializer(), content)
        Files.writeString(configPath, content)
        listeners.forEach { it.invoke(this.config) }
    }

    fun getConfigAsString(): String =
        YamlConfig.default.encodeToString(PreviewConfig.serializer(), getConfig())

    fun getConfig(): PreviewConfig =
        config

}