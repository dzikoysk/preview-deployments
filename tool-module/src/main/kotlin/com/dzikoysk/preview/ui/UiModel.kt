package com.dzikoysk.preview.ui

class UiModel(
    val loggedIn: Boolean,
    val username: String?,
    val webhookUrl: String,
    val activeEnvironments: List<RunningEnvironmentModel>,
    val config: String,
    val logs: List<String>
)

class RunningEnvironmentModel(
    val branch: String,
    val services: List<ServiceModel>
)

class ServiceModel(
    val name: String,
    val url: String?,
    val active: Boolean
)