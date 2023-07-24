package com.dzikoysk.preview.webhook

import com.dzikoysk.preview.runner.RunnerService
import io.javalin.Javalin
import io.javalin.http.bodyAsClass

class WebhookService(
    private val runnerService: RunnerService
) {

    private val app = Javalin
        .create {
            it.showJavalinBanner = false
        }
        .get("/") {
            it.result("Preview webhook is running")
        }
        .post("/api/webhooks/notify") {
            val webhook = it.bodyAsClass<GithubWebhook>()

            when {
                webhook.action == "push" || (webhook.ref != null && webhook.before != null) ->
                    runnerService.updatePreview(webhook.ref!!.substringAfter("refs/heads/"))
                webhook.action == "delete" && webhook.ref_type == "branch" ->
                    runnerService.deletePreview(webhook.ref!!.substringAfter("refs/heads/"))
                else ->
                    println("Unknown webhook action: $webhook")
            }
        }

    fun run(port: Int) {
        app.start(port)
    }

    fun stop() {
        app.stop()
    }

}