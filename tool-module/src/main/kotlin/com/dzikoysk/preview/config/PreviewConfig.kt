package com.dzikoysk.preview.config

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.EncodeDefault.Mode.NEVER
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

typealias RawString = String

data class Credentials(
    val username: String,
    val password: String
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class PreviewConfig(
    val general: General = General(),
    val branches: Map<String, RawString> = mutableMapOf("*" to "preview"),
    @EncodeDefault(NEVER)
    val variables: Map<String, RawString>? = null,
    @EncodeDefault(NEVER)
    val pre: Pre? = null,
    val services: Map<String, Service> = mapOf("example" to Service())
) {
    @Serializable
    data class General(
        val hostname: RawString = "localhost",
        @SerialName("port-range")
        val portRange: RawString = "10000-11000",
        @SerialName("working-directory")
        val workingDirectory: RawString = "./",
        @SerialName("nginx-config")
        val nginxConfig: RawString = "/etc/nginx/conf.d/preview.conf",
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
        val startCommands: List<RawString> = listOf("echo 'Hello world!'"),
        @SerialName("stop-commands")
        val stopCommands: List<RawString> = listOf("echo 'Goodbye world!'"),
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