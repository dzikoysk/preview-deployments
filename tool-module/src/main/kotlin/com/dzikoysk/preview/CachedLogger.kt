package com.dzikoysk.preview

import com.dzikoysk.preview.LoggerLevel.ERR
import java.io.OutputStream
import java.io.PrintStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

enum class LoggerLevel {
    INFO, ERR
}

class CachedLogger {

    private val cached = mutableListOf<String>()
    private val defaultFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun log(level: LoggerLevel = LoggerLevel.INFO, service: String? = null, message: String) {
        val now = LocalDateTime.now().format(defaultFormat)
        val messageWithDate = "$now | ${level.name} | ${service?.let { "$it | " } ?: ""}$message"
        cached.add(messageWithDate)
        println(messageWithDate)
    }

    fun stacktracePrintStream(service: String? = null): PrintStream {
        return PrintStream(object : OutputStream() {
            private val line = StringBuilder()

            override fun write(char: Int) {
                if (char == '\n'.code) {
                    log(ERR, service, line.toString())
                    line.clear()
                    return
                }
                line.append(char.toChar())
            }

            override fun flush() {
                log(ERR, service, line.toString())
                line.clear()
            }
        })
    }

    fun getMessages(): List<String> =
        cached.toList()

}