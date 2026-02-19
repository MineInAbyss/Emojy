package com.mineinabyss.emojy

import com.mineinabyss.idofront.util.removeSuffix
import com.mineinabyss.idofront.util.toColor
import net.kyori.adventure.identity.Identity
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.ShadowColor
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.`object`.ObjectContents
import net.kyori.adventure.util.ARGBLike
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
        val colorable = NamedTextColor.WHITE.takeUnless { "c" in arguments || "colorable" in arguments }
        val shadowColor = arguments.indexOf("shadow").takeIf { it != -1 }?.let { arguments.elementAtOrNull(it+1) }?.toColor()
            ?.let { ShadowColor.shadowColor(it.asARGB()) }
        val sprite = ObjectContents.sprite(emote.atlas, emote.texture.removeSuffix(".png"))

        Tag.selfClosingInserting(Component.`object`(sprite).color(colorable).shadowColor(shadowColor))
    }
}