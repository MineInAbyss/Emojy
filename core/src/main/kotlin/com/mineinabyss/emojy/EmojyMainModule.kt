package com.mineinabyss.emojy

import com.github.shynixn.mccoroutine.bukkit.launch
import com.mineinabyss.dependencies.*
import com.mineinabyss.emojy.config.*
import com.mineinabyss.emojy.nms.EmojyNMSHandlers
import com.mineinabyss.emojy.nms.IEmojyNMSHandler
import com.mineinabyss.emojy.translator.EmojyLanguage
import com.mineinabyss.emojy.translator.EmojyTranslator
import com.mineinabyss.idofront.config.config
import com.mineinabyss.idofront.features.*
import com.mineinabyss.idofront.textcomponents.miniMsg
import com.mineinabyss.idofront.time.ticks
import kotlinx.coroutines.delay
import net.kyori.adventure.text.Component
import net.kyori.adventure.translation.GlobalTranslator
import org.bukkit.entity.Player
import java.util.*
import kotlin.io.path.div

val EmojyMainModule = module("emojy") {
    singleConfig<EmojyConfig>("config.yml") { default = EmojyConfig() }
    singleConfig<EmojyTemplates>("templates.yml") { default = EmojyTemplates() }
    singleConfig<Emotes>("emotes.yml") { default = Emotes() }
    singleConfig<Gifs>("gifs.yml") { default = Gifs() }
    single<Array<Emote>> { get<Emotes>().emotes.map { Emote.from(it, get(), get()) }.toTypedArray() }
    single<Array<Gif>> { get<Gifs>().gifs.map { Gif.from(it, get()) }.toTypedArray() }
    val plugin = plugin
    single<IEmojyNMSHandler> { EmojyNMSHandlers.setup(plugin) }
    single<Array<EmojyLanguage>> {
        get<EmojyConfig>().supportedLanguages.map { langKey ->
            EmojyLanguage(
                Locale.forLanguageTag(langKey.replace("_", "-").replaceFirstChar { it.uppercase() }),
                config<Map<String, String>>().single(plugin.dataPath / "languages" / "$langKey.yml").read()
            )
        }.toTypedArray()
    }
    val generator by single { new(::EmojyGenerator) }

    val translator = GlobalTranslator.translator()
    val source by single { new(::EmojyTranslator) }
    translator.addSource(source)

    addCloseable {
        translator.removeSource(source)
    }

    task(plugin.launch {
        delay(1.ticks) // Let emojy context register since it's used in Emote class
        generator.generateResourcePack()
    })
}.mainCommand {
    "list" {
        permission = null
        executes {
            val emojyConfig = get<EmojyConfig>()
            val emotes = emojy.emotes.filter { it.checkPermission(sender as? Player) && it !in emojyConfig.emojyList.ignoredEmotes }.toSet()
            val gifs = emojy.gifs.filter { it.checkPermission(sender as? Player) && it !in emojyConfig.emojyList.ignoredGifs }.toSet()

            val emoteList = when (sender) {
                is Player -> Component.textOfChildren(*emotes.map { it.formattedComponent(true) }.toTypedArray())
                else -> emotes.joinToString(", ") { it.id }.miniMsg()
            }

            val gifList = when (sender) {
                is Player -> Component.textOfChildren(*gifs.map { it.formattedUnicode(true) }.toTypedArray())
                else -> gifs.joinToString(", ") { it.id }.miniMsg()
            }

            sender.sendRichMessage("<green>List of emojis:")
            sender.sendMessage(emoteList)
            sender.sendRichMessage("<#f35444>List of GIFs")
            sender.sendMessage(gifList)
        }
    }
}