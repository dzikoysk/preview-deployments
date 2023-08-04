package com.dzikoysk.preview.ui

import com.dzikoysk.preview.CachedLogger
import com.dzikoysk.preview.config.ConfigService
import com.dzikoysk.preview.config.Credentials
import com.dzikoysk.preview.runner.RunnerService
import com.dzikoysk.preview.webhook.WebhookService
import io.javalin.Javalin
import io.javalin.http.BadRequestResponse
import io.javalin.http.Context
import io.javalin.http.UnauthorizedResponse
import io.javalin.http.bodyAsClass

const val USERNAME_SESSION_ATTRIBUTE = "username"

class UiService(
    private val logger: CachedLogger,
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
                            loggedIn = ctx.sessionAttribute<String>(USERNAME_SESSION_ATTRIBUTE) != null,
                            username = ctx.sessionAttribute<String>(USERNAME_SESSION_ATTRIBUTE),
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
                            config = configService.getConfigAsString().trim(),
                            logs = logger.getMessages()
                        )
                    )
                )
            }
            .post("/api/ui/login") {
                val username = it.formParam("username") ?: throw BadRequestResponse("Missing username")
                val password = it.formParam("password") ?: throw BadRequestResponse("Missing password")

                when {
                    username == credentials.username && password == credentials.password -> {
                        it.sessionAttribute(USERNAME_SESSION_ATTRIBUTE, username)
                        it.redirect("/")
                    }
                    else -> it.redirect("/?error=Invalid credentials")
                }
            }
            .get("/api/ui/logout") {
                it.req().session.invalidate()
                it.redirect("/")
            }
            .post("/api/ui/preview") {
                it.throwIfNotLoggedIn()
                val branch = it.formParam("branch") ?: throw BadRequestResponse("Missing branch")
                runnerService.updatePreview(branch)
                it.redirect("/")
            }
            .post("/api/ui/config") {
                it.throwIfNotLoggedIn()
                data class ConfigUpdate(val config: String)
                val config = it.bodyAsClass<ConfigUpdate>().config
                configService.updateConfig(config)
                it.redirect("/")
            }
    }

    private fun Context.throwIfNotLoggedIn() {
        if (this.sessionAttribute<String>(USERNAME_SESSION_ATTRIBUTE) == null) {
            throw UnauthorizedResponse("Not logged in")
        }
    }

}