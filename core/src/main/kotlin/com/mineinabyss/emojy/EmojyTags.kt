package com.mineinabyss.emojy

import com.mineinabyss.idofront.util.removeSuffix
import net.kyori.adventure.identity.Identity
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.`object`.ObjectContents
import org.bukkit.Bukkit

object EmojyTags {
    private const val EMOJY = "emojy"
    val RESOLVER = TagResolver.resolver(mutableSetOf(EMOJY)) { args, ctx ->
        val player = ctx.target()?.get(Identity.UUID)?.map(Bukkit::getPlayer)?.orElseGet {
            ctx.target()?.get(Identity.NAME)?.map(Bukkit::getPlayer)?.orElse(null)
        }

        val emoteId = args.popOr("A glyph value is required").value()
        val emote = emojy.emotes.find { it.id == emoteId }
        if (emote == null || emote.atlas == null || !emote.checkPermission(player)) return@resolver null
        val arguments = mutableListOf<String>()
        while (args.hasNext()) arguments.add(args.pop().value())

        Tag.selfClosingInserting(Component.`object`(ObjectContents.sprite(emote.atlas, emote.texture.removeSuffix(".png"))))
    }

    fun containsTag(string: String): Boolean {
        return string.contains("<$EMOJY:")
    }
}