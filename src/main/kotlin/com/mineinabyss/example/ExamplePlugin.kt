package com.mineinabyss.example

import com.mineinabyss.geary.addon.autoscan
import com.mineinabyss.geary.papermc.dsl.gearyAddon
import com.mineinabyss.idofront.config.singleConfig
import com.mineinabyss.idofront.config.startOrAppendKoin
import com.mineinabyss.idofront.platforms.IdofrontPlatforms
import org.bukkit.plugin.java.JavaPlugin
import org.koin.dsl.module

class ExamplePlugin : JavaPlugin() {
    override fun onLoad() {
        IdofrontPlatforms.load(this, "mineinabyss")
    }

    override fun onEnable() {
        saveDefaultConfig()

        // Set up dependency injection
        startOrAppendKoin(module {
            single { ExampleCommands() }
            singleConfig(ExampleConfigData.serializer(), this@ExamplePlugin)
        })

        gearyAddon {
            autoscan("com.mineinabyss") {
                all()
            }
        }
    }
}
