package com.dzikoysk.preview.cli

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.file.Path
import java.util.concurrent.Executors
import java.util.function.Consumer

object CliService {

    private class StreamGobbler(private val inputStream: InputStream, private val consumer: Consumer<String>) : Runnable {
        override fun run() {
            BufferedReader(InputStreamReader(inputStream)).lines().forEach(consumer)
        }
    }

    private val executorService = Executors.newCachedThreadPool()

    data class ShellProcess(
        val process: Process,
        var hasErrors: Boolean = false
    )

    fun createProcess(
        service: String,
        command: String,
        dir: Path,
        env: Map<String, String> = emptyMap()
    ): ShellProcess {
        val processDir = dir.toAbsolutePath().normalize().toFile()

        println("$service | Running command: $command (dir: $processDir)")

        val process = ProcessBuilder()
            .command("sh", "-c", command)
            .directory(processDir)
            .inheritIO()
            .also { it.environment().putAll(env) }
            .start()

        val shellProces = ShellProcess(
            process = process,
            hasErrors = false
        )

        val standardGobbler = StreamGobbler(process.inputStream) { println("$service | INFO | $it") }
        executorService.submit(standardGobbler)

        val errorGobbler = StreamGobbler(process.errorStream) {
            shellProces.hasErrors = true
            println("$service | ERR | $it")
        }
        executorService.submit(errorGobbler)

        return shellProces
    }

}