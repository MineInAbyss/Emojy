package com.mineinabyss.emojy

import com.mineinabyss.idofront.commands.execution.IdofrontCommandExecutor
import com.mineinabyss.idofront.messaging.serialize
import com.mineinabyss.idofront.messaging.success
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class EmojyCommands : IdofrontCommandExecutor(), TabCompleter {
    override val commands = commands(emojy) {
        "emojy" {
            "list" {
                action {
                    val msg = if (sender is Player) emojyConfig.emotes.joinToString(", ") { emote ->
                        emote.getFormattedUnicode().serialize()
                    } else emojyConfig.emotes.joinToString(", ") { it.id }

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
