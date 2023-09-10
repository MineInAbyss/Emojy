package com.mineinabyss.emojy

import net.kyori.adventure.inventory.Book
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.Style
import net.kyori.adventure.text.format.TextDecoration

object EmojyBook {

    private val frontPage = Component.text("Fonts:").style(Style.style(TextDecoration.UNDERLINED)).color(NamedTextColor.GOLD)
        .append(Component.newline().style(Style.empty()).children(
            emojy.config.emotes.map { it.font }.mapIndexed { index, font ->
                Component.text(font.asString()).color(NamedTextColor.RED).clickEvent(ClickEvent.changePage(index + 2)).appendNewline()
            })
    )

    val book = Book.builder().pages(frontPage, *emojy.config.emotes.associateBy { it.font }.map { (_, emote) ->
        Component.empty().append(emote.getFormattedUnicode(" ", true)).appendNewline()
    }.toTypedArray()).build()



}
