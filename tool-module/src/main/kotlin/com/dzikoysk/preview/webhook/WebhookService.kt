package com.dzikoysk.preview.webhook

import com.dzikoysk.preview.runner.RunnerService
import io.javalin.Javalin
import io.javalin.http.bodyAsClass

class WebhookService(
    private val runnerService: RunnerService
) {

    private val webhookLocation = "/api/webhooks/notify"

    fun initializeRouting(httpServer: Javalin) {
        httpServer.post(webhookLocation) {
            val webhook = it.bodyAsClass<GithubWebhook>()
            triggerAction(webhook)
        }
    }

    private fun triggerAction(webhook: GithubWebhook) {
        when {
            webhook.action == "push" || (webhook.ref != null && webhook.before != null) ->
                runnerService.updatePreview(webhook.ref!!.substringAfter("refs/heads/"))
            webhook.action == "delete" && webhook.ref_type == "branch" ->
                runnerService.deletePreview(webhook.ref!!.substringAfter("refs/heads/"))
            else ->
                println("Unknown webhook action: $webhook")
        }
    }

    fun getWebhookLocation(): String =
        webhookLocation

}