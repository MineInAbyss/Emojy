package com.mineinabyss.emojy

import com.mineinabyss.emojy.config.SPACE_PERMISSION
import com.mineinabyss.idofront.font.Space
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.TextReplacementConfig
import net.kyori.adventure.text.TranslatableComponent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.ShadowColor
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.`object`.ObjectContents
import net.kyori.adventure.translation.GlobalTranslator
import java.util.*
import java.util.regex.Pattern

val spaceRegex: Regex = "(?<!\\\\):space_(-?\\d+):".toRegex()
val escapedSpaceRegex: Regex = "\\\\(:space_(-?\\d+):)".toRegex()
val colorableRegex: Regex = "\\|(c|colorable)".toRegex()
val bitmapIndexRegex: Regex = "\\|([0-9]+)(?:\\.\\.([0-9]+))?:".toRegex()

private val randomKey = Key.key("random")
private val randomComponent = Component.text("random").font(randomKey)
private val defaultKey = Key.key("default")

fun Audience?.hasPermission(permission: String): Boolean = this == null || emoteRegistry.hasPermission(this, permission)

internal fun Component.withUsageHover(id: String) = insertion(":$id:").hoverEvent(
    HoverEvent.showText(MiniMessage.miniMessage().deserialize("<red>Type <i>:</i>$id<i>:</i> or <i><u>Shift + Click</i> this to use this emote"))
)

fun spaceString(space: Int) = "<font:${emoteRegistry.spaceFont.asMinimalString()}>${Space.of(space)}</font>"
fun spaceComponent(space: Int) = Component.textOfChildren(Component.text(Space.of(space)).font(emoteRegistry.spaceFont))

val SPACE_REPLACEMENT_CONFIG = TextReplacementConfig.builder()
    .match(spaceRegex.pattern)
    .replacement { matchResult, _ ->
        spaceComponent(matchResult.group(1).toIntOrNull() ?: return@replacement null)
    }.build()

val SPRITE_REPLACEMENT_CONFIG = TextReplacementConfig.builder()
    .match(Pattern.compile("""<(emojy):(\w+(?::(?:c|colorable|s|shadow)(?::[\w#]+)?)*)>"""))
    .replacement { result, _ ->
        val args = result.group().substringAfter("<emojy:").substringBefore(">").split(":")
        val id = args.firstOrNull() ?: return@replacement null
        val emote = emoteRegistry.emotes.find { it.id == id }?.takeIf { it.atlas != null }
            ?: return@replacement Component.text("<emojy:$id>")

        val sprite = ObjectContents.sprite(emote.atlas!!, emote.spriteKey)
        val color = NamedTextColor.WHITE.takeUnless { args.any { it == "colorable" || it == "c" } }
        val shadow = args.elementAtOrNull(args.indexOfFirst { it == "shadow" || it == "s" } + 1)
            ?.toArgb()?.let(ShadowColor::shadowColor) ?: ShadowColor.none()

        Component.`object`(sprite).color(color).shadowColor(shadow)
    }.build()

// Same formats idofront's toColor accepts, falling back to opaque white
internal fun String.toArgb(): Int = runCatching {
    when {
        startsWith("#") -> drop(1).padStart(8, 'F').toLong(16).toInt()
        startsWith("0x") -> drop(2).padStart(8, 'F').toLong(16).toInt()
        "," in this -> {
            val channels = replace(" ", "").split(",").map(String::toInt)
            require(channels.all { it in 0..255 })
            when (channels.size) {
                3 -> (0xFF shl 24) or (channels[0] shl 16) or (channels[1] shl 8) or channels[2]
                4 -> (channels[0] shl 24) or (channels[1] shl 16) or (channels[2] shl 8) or channels[3]
                else -> null
            }
        }
        else -> NamedTextColor.NAMES.value(this)?.value()?.or(0xFF shl 24)
    }
}.getOrNull() ?: 0xFFFFFFFF.toInt()

private fun Component.asFlatTextContent(): String = buildString {
    val component = this@asFlatTextContent
    if (component is TextComponent) append(component.content())
    if (component is TranslatableComponent) component.arguments().forEach { append(it.asComponent().asFlatTextContent()) }
    component.children().forEach { append(it.asFlatTextContent()) }
    (component.hoverEvent()?.value() as? Component)?.let { append(it.asFlatTextContent()) }
}

fun String.transformEmotes(insert: Boolean = false): String {
    var content = this

    for (emote in emoteRegistry.emotes) emote.baseRegex.findAll(this).forEach { match ->

        val colorable = colorableRegex in match.value
        val bitmapMatch = bitmapIndexRegex.find(match.value)
        val startIndex = bitmapMatch?.groupValues?.get(1)?.toIntOrNull() ?: -1
        val endIndex = bitmapMatch?.groupValues?.get(2)?.toIntOrNull()?.coerceAtLeast(startIndex) ?: startIndex

        if (startIndex != -1) content = content.replace(match.value, (startIndex..endIndex).joinToString(":space_-1:") { index ->
            ":${emote.id}|$index${if (colorable) "|c" else ""}:"
        })

        for (bitmapIndex in startIndex..endIndex) {
            content = content.replaceFirst(
                emote.baseRegex, MiniMessage.miniMessage().serialize(emote.formattedComponent(
                    insert = insert,
                    colorable = colorable,
                    bitmapIndex = bitmapIndex
                ))
            )
        }
    }

    for (gif in emoteRegistry.gifs) gif.baseRegex.findAll(this).forEach { _ ->
        content = content.replaceFirst(gif.baseRegex, MiniMessage.miniMessage().serialize(gif.formattedUnicode(insert = insert)))
    }

    spaceRegex.findAll(this).forEach { match ->
        val space = match.groupValues[1].toIntOrNull() ?: return@forEach
        val spaceRegex = "(?<!\\\\):space_(-?$space+):".toRegex()

        content = content.replaceFirst(spaceRegex, spaceString(space))
    }

    return content
}

fun Component.transformEmotes(locale: Locale? = null, insert: Boolean = false): Component {
    var component = GlobalTranslator.render(this, locale ?: Locale.US)
    val serialized = component.asFlatTextContent()

    for (emote in emoteRegistry.emotes) emote.baseRegex.findAll(serialized).forEach { match ->

        val colorable = colorableRegex in match.value
        val bitmapMatch = bitmapIndexRegex.find(match.value)
        val startIndex = bitmapMatch?.groupValues?.get(1)?.toIntOrNull() ?: -1
        val endIndex = bitmapMatch?.groupValues?.get(2)?.toIntOrNull()?.coerceAtLeast(startIndex) ?: startIndex

        if (startIndex != -1) component = component.replaceText(
            TextReplacementConfig.builder()
                .matchLiteral(match.value)
                .replacement((startIndex..endIndex).joinToString(":space_-1:") { index ->
                    ":${emote.id}|$index${if (colorable) "|c" else ""}:"
                })
                .build()
        )

        for (bitmapIndex in startIndex..endIndex) {
            component = component.replaceText(emote.replacementConfig(false, insert, colorable, bitmapIndex))
        }
    }

    for (gif in emoteRegistry.gifs) gif.baseRegex.findAll(serialized).forEach { _ ->
        component = component.replaceText(
            TextReplacementConfig.builder()
                .match(gif.baseRegex.pattern).once()
                .replacement(gif.formattedUnicode(insert = false))
                .build()
        )
    }


    component = component.replaceText(SPACE_REPLACEMENT_CONFIG).replaceText(SPRITE_REPLACEMENT_CONFIG)

    return component
}

fun String.escapeEmoteIDs(player: Audience?): String {
    var content = this
    for (emote in emoteRegistry.emotes.filter { it.font == defaultKey && !it.checkPermission(player) }) emote.unicodes.forEach {
        content = content.replaceFirst(it, "<font:random>$it</font>")
    }

    for (emote in emoteRegistry.emotes) emote.baseRegex.findAll(this).forEach { match ->
        if (emote.checkPermission(player)) return@forEach

        content = content.replaceFirst("(?<!\\\\)${match.value}", "\\${match.value}")
    }

    for (gif in emoteRegistry.gifs) gif.baseRegex.findAll(this).forEach { match ->
        if (gif.checkPermission(player)) return@forEach
        content = content.replaceFirst("(?<!\\\\)${match.value}", "\\${match.value}")
    }

    spaceRegex.findAll(this).forEach { match ->
        if (player.hasPermission(SPACE_PERMISSION)) return@forEach
        val space = match.groupValues[1].toIntOrNull() ?: return@forEach

        content = content.replaceFirst("(?<!\\\\)${match.value}", "\\:space_$space:")
    }

    return content
}

fun Component.escapeEmoteIDs(player: Audience?): Component {
    var component = this
    val serialized = component.asFlatTextContent()

    // Replace all unicodes found in default font with a random one
    // This is to prevent use of unicodes from the font the chat is in
    for (emote in emoteRegistry.emotes.filter { it.font == defaultKey }) for (unicode in emote.unicodes)
        component = component.replaceText(TextReplacementConfig.builder().matchLiteral(unicode).replacement(randomComponent).build())

    for (emote in emoteRegistry.emotes) for (match in emote.baseRegex.findAll(serialized)) {
        if (emote.checkPermission(player)) continue

        component = component.replaceText(
            TextReplacementConfig.builder()
                .match("(?<!\\\\)${match.value}")
                .replacement(Component.text("\\${match.value}"))
                .build()
        )
    }

    for (gif in emoteRegistry.gifs) for (match in gif.baseRegex.findAll(serialized)) {
        if (gif.checkPermission(player)) continue
        component = component.replaceText(
            TextReplacementConfig.builder()
                .match("(?<!\\\\)${match.value}")
                .replacement(Component.text("\\${match.value}"))
                .build()
        )
    }

    for (match in spaceRegex.findAll(serialized)) {
        if (player.hasPermission(SPACE_PERMISSION)) continue
        val space = match.groupValues[1].toIntOrNull() ?: continue

        component = component.replaceText(
            TextReplacementConfig.builder()
                .match("(?<!\\\\)${match.value}")
                .replacement(Component.text("\\:space_$space:"))
                .build()
        )
    }

    return component
}

fun String.unescapeEmoteIds(): String {
    var content = this

    for (emote in emoteRegistry.emotes) emote.escapedRegex.findAll(this).forEach { match ->
        content = content.replaceFirst(emote.escapedRegex, match.value.removePrefix("\\"))
    }

    for (gif in emoteRegistry.gifs) gif.escapedRegex.findAll(this).forEach { match ->
        content = content.replaceFirst(gif.escapedRegex, match.value.removePrefix("\\"))
    }

    escapedSpaceRegex.findAll(this).forEach { match ->
        content = content.replaceFirst(match.value, match.value.removePrefix("\\"))
    }

    return content
}

fun Component.unescapeEmoteIds(): Component {
    var component = this
    val serialized = component.asFlatTextContent()

    for (emote in emoteRegistry.emotes) emote.escapedRegex.findAll(serialized).forEach { match ->
        component = component.replaceText(
            TextReplacementConfig.builder()
                .match(emote.escapedRegex.pattern).once()
                .replacement(match.value.removePrefix("\\"))
                .build()
        )
    }

    for (gif in emoteRegistry.gifs) gif.escapedRegex.findAll(serialized).forEach { match ->
        component = component.replaceText(
            TextReplacementConfig.builder()
                .match(gif.escapedRegex.pattern).once()
                .replacement(match.value.removePrefix("\\"))
                .build()
        )
    }

    escapedSpaceRegex.findAll(serialized).forEach { match ->
        component = component.replaceText(
            TextReplacementConfig.builder()
                .match(match.value).once()
                .replacement(match.value.removePrefix("\\"))
                .build()
        )
    }

    return component
}
