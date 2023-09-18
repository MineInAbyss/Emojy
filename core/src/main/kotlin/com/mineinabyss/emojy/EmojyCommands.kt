package com.mineinabyss.emojy

import com.mineinabyss.idofront.commands.execution.IdofrontCommandExecutor
import com.mineinabyss.idofront.messaging.success
import com.mineinabyss.idofront.textcomponents.miniMsg
import com.mineinabyss.idofront.textcomponents.serialize
import net.kyori.adventure.inventory.Book
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.bukkit.entity.Player

class EmojyCommands : IdofrontCommandExecutor(), TabCompleter {
    override val commands = commands(emojy.plugin) {
        "emojy" {
            "list" {
                action {
                    val emotes = emojy.config.emotes.filter { it.checkPermission(sender as? Player) }.toSet()
                    val gifs = emojy.config.gifs.filter { it.checkPermission(sender as? Player) }.toSet()

                    val emoteList = if (sender is Player) emotes.joinToString("") { emote ->
                        emote.getFormattedUnicode(" ", true).serialize()
                    }.miniMsg() else emojy.config.emotes.joinToString(", ") { it.id }.miniMsg()

                    val gifList = if (sender is Player) gifs.joinToString("") { gif ->
                        gif.getFormattedUnicode(" ").serialize()
                    }.miniMsg() else emojy.config.gifs.joinToString(", ") { it.id }.miniMsg()

                    when (emojy.config.listType) {
                        ListType.BOOK -> sender.openBook(
                            Book.builder().addPage(
                                "<green>List of emojis:<newline>".miniMsg().append(emoteList)
                                    .append("<newline><newline><#f35444>List of gifs:<newline>".miniMsg())
                                    .append(gifList)
                            ).build()
                        )
                        ListType.BOOK2 -> sender.openBook(EmojyBook.book)
                        ListType.CHAT -> {
                            sender.sendRichMessage("<green>List of emojis:")
                            sender.sendMessage(emoteList)
                            sender.sendRichMessage("<#f35444>List of GIFs")
                            sender.sendMessage(gifList)
                        }
                    }
                }
            }
            "reload" {
                action {
                    emojy.plugin.createEmojyContext()
                    emojy.plugin.generateFiles()
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
