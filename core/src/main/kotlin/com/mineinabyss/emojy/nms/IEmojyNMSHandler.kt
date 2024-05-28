package com.mineinabyss.emojy.nms

import org.bukkit.entity.Player
import java.util.Locale

interface IEmojyNMSHandler {

    val locals: MutableSet<Locale>

    fun addLocaleCodec(locale: Locale)

    val supported get() = false
}
