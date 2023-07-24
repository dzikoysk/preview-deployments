package com.dzikoysk.preview.cli

import org.apache.commons.exec.CommandLine
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.lang.ProcessBuilder.Redirect
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

    fun createProcess(command: String, dir: Path): Process {
        val parsedCommand = splitArgsStr(command)
        val processDir = dir.toAbsolutePath().normalize().toFile()

        println("Running command: $command")
        println("Parsed command: ${parsedCommand.joinToString(" ")}")
        println("Working directory: $processDir")

        val process = ProcessBuilder()
            .command("sh", "-c", command)
            .directory(processDir)
            .redirectOutput(Redirect.INHERIT)
            .redirectError(Redirect.INHERIT)
            .start()

        val preGobbler = StreamGobbler(process.inputStream) { println("$command | $it") }
        executorService.submit(preGobbler)

        return process
    }

    private fun splitArgsStr(argsStr: String?): List<String> {
        val execCommandLine = CommandLine("sh")
        execCommandLine.addArguments(argsStr, true)
        return execCommandLine.arguments.map { it.trim() }.filter { it.isNotEmpty() }
    }

}