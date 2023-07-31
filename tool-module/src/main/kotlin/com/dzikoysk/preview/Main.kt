package com.dzikoysk.preview

import java.nio.file.Paths

fun main(args: Array<String>) {
    val configFile = Paths.get(args.getOrNull(0) ?: "preview.yml")
    val app = FeaturePreviewApp()
    app.start(configFile)
}