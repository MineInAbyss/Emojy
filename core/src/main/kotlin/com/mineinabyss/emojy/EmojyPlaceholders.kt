package com.mineinabyss.emojy

import com.mineinabyss.idofront.textcomponents.serialize
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.OfflinePlayer

class EmojyPlaceholders : PlaceholderExpansion() {
    override fun getIdentifier() = "emojy"

    override fun getAuthor() = "boy0000"

    override fun getVersion() = emojy.plugin.pluginMeta.version

    override fun onRequest(player: OfflinePlayer?, params: String): String? {
        val emote = emojy.emotes.firstOrNull { it.id == params }?.formattedUnicode() ?: emojy.gifs.firstOrNull { it.id == params }?.formattedUnicode()
        return emote?.serialize()
    }
}