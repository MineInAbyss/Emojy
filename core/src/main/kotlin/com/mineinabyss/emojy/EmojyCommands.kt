package com.mineinabyss.emojy

import com.mineinabyss.emojy.config.EmojyConfig
import com.mineinabyss.idofront.commands.execution.IdofrontCommandExecutor
import com.mineinabyss.idofront.messaging.broadcastVal
import com.mineinabyss.idofront.messaging.success
import com.mineinabyss.idofront.textcomponents.miniMsg
import net.kyori.adventure.inventory.Book
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.translation.GlobalTranslator
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class EmojyCommands : IdofrontCommandExecutor(), TabCompleter {
    override val commands = commands(emojy.plugin) {
        "emojy" {
            "lang" {
                emojy.languages.map { it.locale }.joinToString { it.toString() }.broadcastVal()
                sender.sendMessage(emojy.languages.any { it.locale == (sender as Player).locale() }.toString())
            }
            "test" {
                action {
                    val lang = (sender as? Player)?.locale()?.takeIf { it in emojy.languages.map { it.locale } } ?: emojy.languages.last().locale
                    sender.sendMessage(GlobalTranslator.render(("<lang:mineinabyss.tutorial.welcome.1>" + " : <lang:mineinabyss.tutorial.welcome.2>").miniMsg(), lang))
                    sender.sendMessage(GlobalTranslator.render(("<lang:mineinabyss.tutorial.welcome.1>" + "</lang>" +  " : <lang:mineinabyss.tutorial.welcome.2>").miniMsg(), lang))
                }
            }
            "list" {
                action {
                    val emotes = emojy.emotes.filter { it.checkPermission(sender as? Player) && it !in emojyConfig.emojyList.ignoredEmotes }.toSet()
                    val gifs = emojy.gifs.filter { it.checkPermission(sender as? Player) && it !in emojyConfig.emojyList.ignoredGifs }.toSet()

                    val emoteList = when (sender) {
                        is Player -> Component.textOfChildren(*emotes.map { it.formattedUnicode(true) }.toTypedArray())
                        else -> emojy.emotes.joinToString(", ") { it.id }.miniMsg()
                    }

                    val gifList = when (sender) {
                        is Player -> Component.textOfChildren(*gifs.map { it.formattedUnicode(true) }.toTypedArray())

                        else -> emojy.gifs.joinToString(", ") { it.id }.miniMsg()
                    }

                    sender.sendRichMessage("<green>List of emojis:")
                    sender.sendMessage(emoteList)
                    sender.sendRichMessage("<#f35444>List of GIFs")
                    sender.sendMessage(gifList)
                }
            }
            "reload" {
                action {
                    emojy.plugin.createEmojyContext()
                    EmojyGenerator.generateResourcePack()
                    sender.success("Emojy has been reloaded!")
                }
            }
        }
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): List<String> {
        return if (command.name == "emojy") when (args.size) {
            1 -> listOf("list", "reload").filter { it.startsWith(args[0]) }
            else -> emptyList()
        }
        else return emptyList()
    }
}
