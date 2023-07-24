package com.dzikoysk.preview.runner

import com.dzikoysk.preview.PreviewConfig
import com.dzikoysk.preview.routing.RoutingService
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger

class RunnerService(
    private val config: PreviewConfig,
    private val workDir: Path,
    private val routingService: RoutingService,
) {

    private val idAssigner = AtomicInteger()
    private val availablePorts = AtomicInteger(config.general.portRange.split("-")[0].toInt())
    private val environments = mutableMapOf<String, PreviewEnvironment>()

    init {
        Files.createDirectories(workDir)
    }

    fun updatePreview(branch: String) {
        if (config.branches.containsKey(branch) || config.branches.containsKey("*")) {
            val currentEnvironment = environments[branch]
            currentEnvironment?.destroyPreview()

            val environment = currentEnvironment ?: run {
                val id = idAssigner.getAndIncrement()
                val subdomain = branch.replace("/", "-")
                val url = "${subdomain}.${config.general.hostname}"
                val environment = PreviewEnvironment(
                    config = config,
                    workDir = workDir,
                    id = id,
                    url = url,
                    reservePort = { availablePorts.incrementAndGet() },
                    routingService = routingService,
                    branch = branch
                )
                environments[branch] = environment
                environment
            }

            environment.createPreview()
            routingService.regenerateConfig()
        }
    }

    fun deletePreview(branch: String) {
        if (environments.containsKey(branch)) {
            environments[branch]!!.destroyPreview()
            environments.remove(branch)
        }
    }

    fun disposeAllPreviews() {
        environments.toList().forEach { (branch, _) ->
            deletePreview(branch)
        }
    }

}