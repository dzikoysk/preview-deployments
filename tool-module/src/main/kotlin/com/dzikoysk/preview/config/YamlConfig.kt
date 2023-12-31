package com.dzikoysk.preview.config

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration

object YamlConfig {

    val default: Yaml = Yaml(
        configuration = YamlConfiguration(
            breakScalarsAt = Int.MAX_VALUE
        )
    )

}