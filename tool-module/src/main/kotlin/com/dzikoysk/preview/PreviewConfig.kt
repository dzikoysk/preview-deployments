package com.dzikoysk.preview

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

typealias RawString = String

@Serializable
data class PreviewConfig(
    val general: General,
    val branches: Map<String, RawString>,
    val variables: Map<String, RawString>? = null,
    val pre: Pre? = null,
    val services: Map<String, Service>
) {
    @Serializable
    data class General(
        val hostname: RawString,
        @SerialName("webhook-port")
        val webhookPort: Int,
        @SerialName("port-range")
        val portRange: RawString,
        @SerialName("working-directory")
        val workingDirectory: RawString,
        @SerialName("nginx-config")
        val nginxConfig: RawString,
        @SerialName("git-source")
        val gitSource: RawString,
        @SerialName("ssh-key")
        val sshKey: RawString? = null
    )

    @Serializable
    data class Pre(
        val commands: List<RawString>? = null
    )

    @Serializable
    data class Service(
        val source: RawString? = null,
        val public: Public? = null,
        @SerialName("start-commands")
        val startCommands: List<RawString>,
        @SerialName("stop-commands")
        val stopCommands: List<RawString>,
        val environment: Map<String, RawString>? = null
    ) {
        @Serializable
        data class Public(
            val port: RawString,
            val url: RawString
        )
    }
}