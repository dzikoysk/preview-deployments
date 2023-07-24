package com.dzikoysk.preview

import com.charleskorn.kaml.Yaml
import com.dzikoysk.preview.routing.RoutingService
import com.dzikoysk.preview.runner.RunnerService
import com.dzikoysk.preview.webhook.WebhookService
import java.nio.file.Paths
import kotlin.io.path.absolute
import kotlin.io.path.readText

fun main(args: Array<String>) {
    val configFile = Paths.get(args.getOrNull(0) ?: "preview.yml")
    if (!configFile.toFile().exists()) {
        println("Config file not found at ${configFile.toAbsolutePath()}")
        return
    }
    val config = Yaml.default.decodeFromString(PreviewConfig.serializer(), configFile.readText())
    val workDir = Paths.get(config.general.workingDirectory).absolute().normalize()

    val routingService = RoutingService(config, workDir)
    val runnerService = RunnerService(config, workDir, routingService)

    val webhookService = WebhookService(runnerService)
    webhookService.run(config.general.webhookPort)

    val printingHook = Thread {
        println("Stopping services...")
        webhookService.stop()
        runnerService.disposeAllPreviews()
    }
    Runtime.getRuntime().addShutdownHook(printingHook)
}