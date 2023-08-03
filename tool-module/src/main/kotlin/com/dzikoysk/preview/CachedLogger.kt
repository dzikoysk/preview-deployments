package com.dzikoysk.preview

import java.io.OutputStream
import java.io.PrintStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class CachedLogger {

    private val cached = mutableListOf<String>()
    private val defaultFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    fun log(message: String) {
        val messageWithDate = "${LocalDateTime.now().format(defaultFormat)} | $message"
        cached.add(messageWithDate)
        println(messageWithDate)
    }

    fun printStream(): PrintStream {
        return PrintStream(object : OutputStream() {
            private val line = StringBuilder()

            override fun write(char: Int) {
                if (char == '\n'.toInt()) {
                    log(line.toString())
                    line.clear()
                    return
                }
                line.append(char.toChar())
            }

            override fun flush() {
                log(line.toString())
                line.clear()
            }
        })
    }

    fun getMessages(): List<String> =
        cached.toList()

}