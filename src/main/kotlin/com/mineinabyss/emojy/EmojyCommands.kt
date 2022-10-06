package com.mineinabyss.emojy

import com.mineinabyss.idofront.commands.execution.IdofrontCommandExecutor
import com.mineinabyss.idofront.commands.extensions.actions.playerAction
import com.mineinabyss.idofront.messaging.miniMsg
import com.mineinabyss.idofront.messaging.serialize
import com.mineinabyss.idofront.messaging.success
import net.kyori.adventure.title.TitlePart
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class EmojyCommands : IdofrontCommandExecutor(), TabCompleter {
    override val commands = commands(emojy) {
        "emojy" {
            "test" {
                playerAction {
                    player.sendTitlePart(TitlePart.TITLE, "<red>:pog:Hello World!".miniMsg())
                }
            }
            "list" {
                action {
                    val msg = if (sender is Player) emojyConfig.emotes.values.joinToString("") { emote ->
                        emote.getFormattedUnicode(", ").serialize()
                    } else emojyConfig.emotes.keys.joinToString(", ") { it }

                    sender.sendRichMessage("<green>List of emojis:")
                    sender.sendRichMessage(msg)
                }
            }
            "reload" {
                action {
                    emojyConfig = EmojyConfig.data
                    EmojyGenerator.reloadFontFiles()
                    if (emojyConfig.generateResourcePack)
                        EmojyGenerator.generateResourcePack()
                    sender.success("Config reloaded!")
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
        return if (command.name == "emojy") {
            when (args.size) {
                1 -> listOf("list", "reload").filter { it.startsWith(args[0]) }
                else -> emptyList()
            }
        } else return emptyList()
    }
}
