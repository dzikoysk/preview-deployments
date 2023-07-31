package com.dzikoysk.preview

import com.charleskorn.kaml.Yaml
import com.dzikoysk.preview.routing.RoutingService
import com.dzikoysk.preview.runner.RunnerService
import com.dzikoysk.preview.ui.UiService
import com.dzikoysk.preview.webhook.WebhookService
import gg.jte.ContentType
import gg.jte.TemplateEngine
import gg.jte.resolve.DirectoryCodeResolver
import io.javalin.Javalin
import io.javalin.rendering.template.JavalinJte
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.absolute
import kotlin.io.path.readText

class FeaturePreviewApp {

    fun start(configFile: Path) {
        if (!configFile.toFile().exists()) {
            println("Config file not found at ${configFile.toAbsolutePath()}")
            return
        }
        val config = Yaml.default.decodeFromString(PreviewConfig.serializer(), configFile.readText())
        val workDir = Paths.get(config.general.workingDirectory).absolute().normalize()

        val routingService = RoutingService(config, workDir)
        val runnerService = RunnerService(config, workDir, routingService)

//    val codeResolver = ResourceCodeResolver("templates")
        val codeResolver = DirectoryCodeResolver(Paths.get(".").absolute().normalize())
        val templatingEngine = TemplateEngine.create(codeResolver, ContentType.Html)
        JavalinJte.init(templatingEngine)

        val httpServer = Javalin.create {
            it.showJavalinBanner = false
        }

        val webhookService = WebhookService(runnerService)
        webhookService.initializeRouting(httpServer)

        val uiService = UiService(
            credentials = "admin" to "admin",
            webhookService = webhookService,
            runnerService = runnerService
        )
        uiService.initializeRouting(httpServer)

        httpServer.start()

        val printingHook = Thread {
            println("Stopping services...")
            httpServer.stop()
            runnerService.disposeAllPreviews()
        }
        Runtime.getRuntime().addShutdownHook(printingHook)
    }

}