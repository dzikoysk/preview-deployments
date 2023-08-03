package com.dzikoysk.preview.cli

import com.dzikoysk.preview.CachedLogger
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.file.Path
import java.util.concurrent.Executors
import java.util.function.Consumer

class CliService(private val logger: CachedLogger) {

    private class StreamGobbler(
        private val inputStream: InputStream,
        private val consumer: Consumer<String>
    ) : Runnable {
        override fun run() {
            BufferedReader(InputStreamReader(inputStream))
                .lines()
                .flatMap { it.split("\n").stream() }
                .forEach(consumer)
        }
    }

    private val executorService = Executors.newCachedThreadPool()

    data class ShellProcess(
        val process: Process,
        var hasErrors: Boolean = false
    )

    private enum class OsType {
        UNIX,
        WINDOWS,
    }

    fun createProcess(
        service: String,
        command: String,
        dir: Path,
        env: Map<String, String> = emptyMap()
    ): ShellProcess {
        val processDir = dir.toAbsolutePath().normalize().toFile()
        logger.log("$service | Running command: $command (dir: $processDir)")

        val os = when {
            System.getProperty("os.name").contains("Windows") -> OsType.WINDOWS
            else -> OsType.UNIX
        }

        val process = ProcessBuilder()
            .also {
                when (os) {
                    OsType.UNIX -> it.command("sh", "-c", command)
                    OsType.WINDOWS -> it.command("cmd.exe", "/c", command)
                }
            }
            .directory(processDir)
            .also { it.environment().putAll(env) }
            .start()

        val shellProces = ShellProcess(
            process = process,
            hasErrors = false
        )

        val standardGobbler = StreamGobbler(process.inputStream) {
            logger.log("$service | INFO | $it")
        }
        executorService.submit(standardGobbler)

        val errorGobbler = StreamGobbler(process.errorStream) {
            shellProces.hasErrors = true
            logger.log("$service | ERR | $it")
        }
        executorService.submit(errorGobbler)

        return shellProces
    }

}