package com.mineinabyss.emojy

import com.mineinabyss.idofront.messaging.logError
import net.kyori.adventure.inventory.Book
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

object EmojyBook {

    private val frontPage =
        Component.text("Fonts:").style(Style.style(TextDecoration.UNDERLINED)).color(NamedTextColor.GOLD)
            .append(Component.newline().style(Style.empty()).children(
                emojy.emotes.map { it.font }.mapIndexed { index, font ->
                    Component.text(font.asString()).color(NamedTextColor.RED)
                        .clickEvent(ClickEvent.changePage(index + 2)).appendNewline()
                })
            )

    fun book(sender: CommandSender) {
        runCatching {
            sender.openBook(Book.builder().pages(
                frontPage, *emojy.emotes.groupBy { it.font }.map { (_, emotes) ->
                    Component.textOfChildren(*emotes.filter { it.checkPermission(sender as? Player) && it !in emojyConfig.emojyList.ignoredEmotes }
                        .take(10).map { it.formattedUnicode() }.toTypedArray())
                }.toTypedArray()
            ).build()
            )

        }.onFailure {
            sender.openBook(
                Book.builder().addPage(Component.text("Could not build book").color(NamedTextColor.RED)).build()
            )
            logError("Failed to generate book: ${it.message}")
        }
    }


}
