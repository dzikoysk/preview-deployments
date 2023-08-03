package com.dzikoysk.preview

import com.dzikoysk.preview.cli.CliService
import com.dzikoysk.preview.config.ConfigService
import com.dzikoysk.preview.config.Credentials
import com.dzikoysk.preview.config.PreviewConfig
import com.dzikoysk.preview.routing.RoutingService
import com.dzikoysk.preview.runner.RunnerService
import com.dzikoysk.preview.ui.UiService
import com.dzikoysk.preview.webhook.WebhookService
import gg.jte.ContentType
import gg.jte.TemplateEngine
import gg.jte.resolve.DirectoryCodeResolver
import io.javalin.Javalin
import io.javalin.rendering.template.JavalinJte
import io.javalin.util.JavalinLogger
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.absolute

// java -jar app.jar <port=8080> <username=admin> <password=admin> <path=preview.yml>
fun main(args: Array<String>) {
    FeaturePreviewApp().start(
        port = args.getOrNull(0)?.toIntOrNull() ?: 8080,
        credentials = Credentials(
            username = args.getOrNull(1) ?: "admin",
            password = args.getOrNull(2) ?: "admin"
        ),
        configFile = Paths.get(args.getOrNull(3) ?: "preview.yml")
    )
}

class FeaturePreviewApp {

    private val logger = CachedLogger()
    private lateinit var httpServer: Javalin
    private lateinit var runnerService: RunnerService
    private lateinit var configService: ConfigService

    fun start(port: Int, credentials: Credentials, configFile: Path) {
        if (!configFile.toFile().exists()) {
            logger.log("Config file not found at ${configFile.toAbsolutePath()}")
            return
        }
        this.configService = ConfigService(configFile)

//    val codeResolver = ResourceCodeResolver("templates")
        val codeResolver = DirectoryCodeResolver(Paths.get(".").absolute().normalize())
        val templatingEngine = TemplateEngine.create(codeResolver, ContentType.Html)
        JavalinJte.init(templatingEngine)

        configService.subscribe {
            stop()
            initialize(port, credentials, it)
        }
        initialize(port, credentials, configService.getConfig())

        val printingHook = Thread { stop() }
        Runtime.getRuntime().addShutdownHook(printingHook)
    }

    private fun initialize(port: Int, credentials: Credentials, config: PreviewConfig) {
        val workDir = Paths.get(config.general.workingDirectory).absolute().normalize()

        val cliService = CliService(
            logger = logger
        )

        val routingService = RoutingService(
            config = config,
            workDir = workDir,
            cliService = cliService

        )
        this.runnerService = RunnerService(
            logger = logger,
            config = config,
            workDir = workDir,
            cliService = cliService,
            routingService = routingService
        )

        this.httpServer = Javalin.create {
            it.showJavalinBanner = false
        }

        val webhookService = WebhookService(
            logger = logger,
            runnerService = runnerService
        )
        webhookService.initializeRouting(httpServer)

        val uiService = UiService(
            logger = logger,
            configService = configService,
            credentials = credentials,
            webhookService = webhookService,
            runnerService = runnerService
        )
        uiService.initializeRouting(httpServer)

        httpServer.start(port)
        logger.log("Feature Preview is running on http://localhost:$port")
    }

    fun stop() {
        logger.log("Stopping services...")
        httpServer.stop()
        runnerService.disposeAllPreviews()
    }

}