package com.dzikoysk.preview.ui

import com.dzikoysk.preview.config.ConfigService
import com.dzikoysk.preview.config.Credentials
import com.dzikoysk.preview.runner.RunnerService
import com.dzikoysk.preview.webhook.WebhookService
import io.javalin.Javalin
import io.javalin.http.BadRequestResponse
import io.javalin.http.bodyAsClass

class UiService(
    private val configService: ConfigService,
    private val credentials: Credentials,
    private val webhookService: WebhookService,
    private val runnerService: RunnerService
){

    fun initializeRouting(httpServer: Javalin) {
        httpServer
            .get("/") { ctx ->
                ctx.render(
                    filePath = "index.jte",
                    model = mapOf(
                        "model" to UiModel(
                            loggedIn = ctx.req().session.getAttribute("username") != null,
                            username = ctx.req().session.getAttribute("username")?.toString(),
                            webhookUrl = webhookService.getWebhookLocation(),
                            activeEnvironments = runnerService.getRunningEnvironments().map { env ->
                                RunningEnvironmentModel(
                                    branch = env.branch,
                                    services = env.services.map { service ->
                                        ServiceModel(
                                            name = service.name,
                                            url = service.config.public?.url,
                                            active = service.childProcesses.none { it.hasErrors }
                                        )
                                    }
                                )
                            },
                            config = configService.getConfigAsString().trim()
                        )
                    )
                )
            }
            .post("/api/ui/login") {
                val username = it.formParam("username") ?: throw BadRequestResponse("Missing username")
                val password = it.formParam("password") ?: throw BadRequestResponse("Missing password")

                when {
                    username == credentials.username && password == credentials.password -> {
                        it.req().session.setAttribute("username", username)
                        it.redirect("/")
                    }
                    else -> {
                        it.redirect("/?error=Invalid credentials")
                    }
                }
            }
            .get("/api/ui/logout") {
                it.req().session.invalidate()
                it.redirect("/")
            }
            .post("/api/ui/preview") {
                val branch = it.formParam("branch") ?: throw BadRequestResponse("Missing branch")
                runnerService.updatePreview(branch)
                it.redirect("/")
            }
            .post("/api/ui/config") {
                data class ConfigUpdate(val config: String)
                val config = it.bodyAsClass<ConfigUpdate>().config
                configService.updateConfig(config)
                it.redirect("/")
            }
    }

}