package com.dzikoysk.preview.runner

import com.dzikoysk.preview.CachedLogger
import com.dzikoysk.preview.LoggerLevel.ERR
import com.dzikoysk.preview.config.PreviewConfig
import com.dzikoysk.preview.cli.CliService
import com.dzikoysk.preview.cli.CliService.ShellProcess
import com.dzikoysk.preview.config.RawString
import com.dzikoysk.preview.routing.RoutingService
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.TimeUnit.SECONDS

class PreviewEnvironment(
    private val logger: CachedLogger,
    private val config: PreviewConfig,
    workDir: Path,
    private val id: Int,
    private val url: String,
    val branch: String,
    private val reservePort: () -> Int,
    private val routingService: RoutingService,
    private val cliService: CliService
) {

    data class ServiceProcess(
        val name: String,
        val config: PreviewConfig.Service,
        val childProcesses: List<ShellProcess>
    )

    val services = mutableListOf<ServiceProcess>()
    private val branchDir = workDir.resolve(id.toString())

    fun createPreview() {
        Files.createDirectories(branchDir)

        if (config.general.gitSource != null) {
            val sshCommand = when {
                config.general.sshKey != null -> "-c 'core.sshCommand=ssh -o StrictHostKeyChecking=accept-new -i ${config.general.sshKey}'"
                else -> ""
            }

            when {
                Files.exists(branchDir.resolve(".git")) ->
                cliService.createProcess(
                        service = "Git",
                        command = "git $sshCommand pull --force; git checkout $branch --force",
                        dir = branchDir
                    ).process.waitFor()
                else ->
                    cliService.createProcess(
                        service = "Git",
                        command = "git $sshCommand clone ${config.general.gitSource} .; git checkout $branch",
                        dir = branchDir
                    ).process.waitFor()
            }
        }

        val variables = (config.variables ?: emptyMap())
            .mapValues { (_, value) ->
                value
                    .replace("id()", id.toString())
                    .replace("url()", url)
                    .let {
                        when {
                            "port()" in it -> it.replace("port()", reservePort().toString())
                            else -> it
                        }
                    }
            }
            .toMap()

        fun RawString.getProcessedValue(): String =
            variables.entries.fold(this) { acc, (key, value) ->
                acc.replace("\${$key}", value)
            }

        config.pre?.commands
            ?.map { it.getProcessedValue() }
            ?.forEach { command ->
                val preProcess = cliService.createProcess(
                    service = "[pre]",
                    command = command,
                    dir = branchDir
                )
                val exitCode = preProcess.process.waitFor()
                logger.log(
                    service = "[pre]",
                    message = "Pre process $command exited with code $exitCode"
                )
            }

        config.services
            .mapValues { (_, service) ->
                service.copy(
                    source = service.source?.getProcessedValue(),
                    public = service.public?.let {
                        it.copy(
                            port = it.port.getProcessedValue(),
                            url = it.url.getProcessedValue()
                        )
                    },
                    startCommands = service.startCommands.map { it.getProcessedValue() },
                    stopCommands = service.stopCommands.map { it.getProcessedValue() },
                    environment = service.environment?.mapValues { (_, value) -> value.getProcessedValue() }
                )
            }
            .onEach { (_, service) ->
                if (service.public != null) {
                    routingService.registerRoute(
                        publicUrl = service.public.url,
                        internalPort = service.public.port.toInt()
                    )
                }
            }
            .forEach { (name, service) ->
                services.add(
                    ServiceProcess(
                        name = name,
                        config = service,
                        childProcesses = service.startCommands.map {
                            cliService.createProcess(
                                service = name,
                                command = it,
                                dir = branchDir.resolve(service.source ?: ""),
                                env = service.environment ?: emptyMap()
                            )
                        }
                    )
                )
            }
    }

    fun destroyPreview() {
        for (serviceProcess in services) {
            try {
                logger.log(
                    service = serviceProcess.name,
                    message = "Stopping process ${serviceProcess.name}"
                )
                serviceProcess.config.public?.let {
                    routingService.unregisterRoute(it.url)
                }
                serviceProcess.childProcesses.forEach {
                    try {
                        it.process.destroyForcibly()
                    } catch (e: Exception) {
                        logger.log(ERR, serviceProcess.name, "Failed to stop process ${serviceProcess.name}")
                        e.printStackTrace(logger.stacktracePrintStream(serviceProcess.name))
                    }
                }
                serviceProcess.config.stopCommands.forEach {
                    try {
                        when (it) {
                            "\$exit" -> { /* process already stopped */
                            }

                            else -> cliService.createProcess(
                                service = serviceProcess.name,
                                command = it,
                                dir = branchDir
                            ).process.waitFor()
                        }
                    } catch (e: Exception) {
                        logger.log(ERR, serviceProcess.name, "Failed to stop process ${serviceProcess.name}")
                        e.printStackTrace(logger.stacktracePrintStream(serviceProcess.name))
                    }
                }
            } catch (e: Exception) {
                logger.log(ERR, serviceProcess.name, "Failed to stop process ${serviceProcess.name}")
                e.printStackTrace(logger.stacktracePrintStream(serviceProcess.name))
            }
        }

        services.clear()

        Files.walk(branchDir).use { stream ->
            stream
                .sorted(Comparator.reverseOrder())
                .forEach {
                    runCatching {
                        Files.deleteIfExists(it)
                    }
                }
        }
        Files.deleteIfExists(branchDir)
    }

}