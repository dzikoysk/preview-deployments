package com.dzikoysk.preview

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.EncodeDefault.Mode.NEVER
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

typealias RawString = String

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class PreviewConfig(
    val general: General,
    val branches: Map<String, RawString> = mutableMapOf("*" to "preview"),
    @EncodeDefault(NEVER)
    val variables: Map<String, RawString>? = null,
    @EncodeDefault(NEVER)
    val pre: Pre? = null,
    val services: Map<String, Service>
) {
    @Serializable
    data class General(
        val hostname: RawString = "localhost",
        @SerialName("webhook-port")
        val webhookPort: Int,
        @SerialName("port-range")
        val portRange: RawString,
        @SerialName("working-directory")
        val workingDirectory: RawString,
        @SerialName("nginx-config")
        val nginxConfig: RawString,
        @SerialName("git-source")
        @EncodeDefault(NEVER)
        val gitSource: RawString? = null,
        @SerialName("ssh-key")
        @EncodeDefault(NEVER)
        val sshKey: RawString? = null
    )

    @Serializable
    data class Pre(
        @EncodeDefault(NEVER)
        val commands: List<RawString>? = null
    )

    @Serializable
    data class Service(
        @EncodeDefault(NEVER)
        val source: RawString? = null,
        @EncodeDefault(NEVER)
        val public: Public? = null,
        @SerialName("start-commands")
        val startCommands: List<RawString>,
        @SerialName("stop-commands")
        val stopCommands: List<RawString>,
        @EncodeDefault(NEVER)
        val environment: Map<String, RawString>? = null
    ) {
        @Serializable
        data class Public(
            val port: RawString,
            val url: RawString
        )
    }
}