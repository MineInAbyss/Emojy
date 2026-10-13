package com.mineinabyss.emojy

import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.ShadowColor
import net.kyori.adventure.text.minimessage.Context
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.`object`.ObjectContents

object EmojyTags {
    private const val EMOJY = "emojy"

    // implemented directly rather than via TagResolver.resolver since adventure 5 forbids a null return from that handler
    val RESOLVER = object : TagResolver {
        override fun has(name: String) = name == EMOJY

        override fun resolve(name: String, args: ArgumentQueue, ctx: Context): Tag? {
            if (name != EMOJY) return null

            val emoteId = args.popOr("A glyph value is required").value()
            val emote = emoteRegistry.emotes.find { it.id == emoteId }
            if (emote == null || emote.atlas == null || !emote.checkPermission(ctx.target() as? Audience)) return null

            val arguments = buildList { while (args.hasNext()) add(args.pop().value()) }
            val colorable = NamedTextColor.WHITE.takeUnless { "c" in arguments || "colorable" in arguments }
            val shadowColor = arguments.indexOf("shadow").takeIf { it != -1 }?.let { arguments.elementAtOrNull(it + 1) }?.toArgb()
                ?.let(ShadowColor::shadowColor) ?: ShadowColor.none()
            val sprite = ObjectContents.sprite(emote.atlas, emote.spriteKey)

            return Tag.selfClosingInserting(Component.`object`(sprite).color(colorable).shadowColor(shadowColor))
        }
    }
}
