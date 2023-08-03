package com.dzikoysk.preview.routing

import com.dzikoysk.preview.config.PreviewConfig
import com.dzikoysk.preview.cli.CliService
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

typealias PublicUrl = String
typealias InternalPort = Int

class RoutingService(
    private val config: PreviewConfig,
    private val workDir: Path,
    private val cliService: CliService
) {

    private val routes = mutableListOf<Pair<PublicUrl, InternalPort>>()

    fun registerRoute(publicUrl: PublicUrl, internalPort: InternalPort) {
        routes.add(publicUrl to internalPort)
    }

    fun regenerateConfig() {
        val nginxConfig = Paths.get(config.general.nginxConfig)
        Files.writeString(
            nginxConfig, """
            map ${'$'}http_upgrade ${'$'}connection_upgrade {
              default upgrade;
              '' close;
            }
            ${routes.joinToString("\n") { (publicUrl, internalPort) ->
                """
            server {
                server_name ${publicUrl.substringBeforeLast(":")};
                listen ${publicUrl.substringAfterLast(":", missingDelimiterValue = internalPort.toString())};
                listen [::]:${publicUrl.substringAfterLast(":", missingDelimiterValue = internalPort.toString())};
                access_log /var/log/nginx/reverse-access.log;
                error_log /var/log/nginx/reverse-error.log;
            
                client_max_body_size 50m; # maximum allowed artifact upload size
            
                location / {
                    proxy_pass http://localhost:${internalPort};
                    proxy_set_header   Host              ${'$'}host;
                    proxy_set_header   X-Real-IP         ${'$'}remote_addr;
                    proxy_set_header   X-Forwarded-For   ${'$'}proxy_add_x_forwarded_for;
                    proxy_set_header   X-Forwarded-Proto ${'$'}scheme;
                    proxy_set_header   Upgrade           ${'$'}http_upgrade;
                    proxy_set_header   Connection        ${'$'}connection_upgrade;
                    proxy_http_version 1.1;
                }
            }
                """
            }}
            """.trimIndent(),
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING,
            StandardOpenOption.WRITE
        )

        cliService.createProcess(
            service = "nginx",
            command = "nginx -t && nginx -s reload",
            dir = workDir
        ).process.waitFor()
    }

    fun unregisterRoute(url: PublicUrl) {
        routes.removeIf { (publicUrl, _) -> publicUrl == url }
    }

}