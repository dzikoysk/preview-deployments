package com.dzikoysk.preview.runner

import com.dzikoysk.preview.CachedLogger
import com.dzikoysk.preview.cli.CliService
import com.dzikoysk.preview.config.PreviewConfig
import com.dzikoysk.preview.routing.RoutingService
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger

class RunnerService(
    private val logger: CachedLogger,
    private val config: PreviewConfig,
    private val workDir: Path,
    private val cliService: CliService,
    private val routingService: RoutingService,
) {

    companion object {
        private val idAssigner = AtomicInteger()
        private var availablePorts: Lazy<AtomicInteger>? = null
    }

    private val environments = mutableMapOf<String, PreviewEnvironment>()

    init {
        // TODO: Impl a better port handler
        if (availablePorts == null) {
            availablePorts = lazy { AtomicInteger(config.general.portRange.split("-")[0].toInt()) }
        }

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
                    logger = logger,
                    config = config,
                    workDir = workDir,
                    id = id,
                    url = url,
                    reservePort = { availablePorts!!.value.incrementAndGet() },
                    cliService = cliService,
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
            routingService.regenerateConfig()
        }
    }

    fun disposeAllPreviews() {
        environments.toList().forEach { (branch, _) ->
            deletePreview(branch)
        }
    }

    fun getRunningEnvironments(): List<PreviewEnvironment> =
        environments.values.toList()

}