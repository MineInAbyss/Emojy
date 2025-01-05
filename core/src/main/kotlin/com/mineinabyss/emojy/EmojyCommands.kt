package com.mineinabyss.emojy

import com.mineinabyss.idofront.commands.brigadier.commands
import com.mineinabyss.idofront.messaging.success
import com.mineinabyss.idofront.textcomponents.miniMsg
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player

object EmojyCommands {

    fun registerCommands() {
        emojy.plugin.commands {
            "emojy" {
                "list" {
                    requiresPermission("")
                    executes {
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
                "reload" {
                    executes {
                        emojy.plugin.createEmojyContext()
                        EmojyGenerator.generateResourcePack()
                        sender.success("Emojy has been reloaded!")
                    }
                }
            }
        }
    }
}