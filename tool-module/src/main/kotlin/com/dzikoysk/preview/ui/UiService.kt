package com.dzikoysk.preview.ui

import com.dzikoysk.preview.webhook.WebhookService
import io.javalin.Javalin
import io.javalin.http.BadRequestResponse

class UiService(
    private val credentials: Pair<String, String>,
    private val webhookService: WebhookService
){

    fun initializeRouting(httpServer: Javalin) {
        httpServer
            .get("/") {
                it.render(
                    filePath = "index.kte",
                    model = mapOf(
                        "model" to UiModel(
                            loggedIn = it.req().session.getAttribute("username") != null,
                            webhookUrl = webhookService.getWebhookLocation()
                        )
                    )
                )
            }
            .post("/login") {
                val username = it.formParam("username") ?: throw BadRequestResponse("Missing username")
                val password = it.formParam("password") ?: throw BadRequestResponse("Missing password")

                when {
                    username == "admin" && password == "admin" -> {
                        it.req().session.setAttribute("username", username)
                        it.redirect("/")
                    }
                    else -> {
                        it.redirect("/?error=Invalid credentials")
                    }
                }
            }
            .get("/logout") {
                it.req().session.invalidate()
                it.redirect("/")
            }
    }

}