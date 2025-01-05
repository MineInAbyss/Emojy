package com.mineinabyss.emojy.nms

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer

interface IEmojyNMSHandler {

    companion object {
        val legacyHandler = LegacyComponentSerializer.legacy('§')
    }

    val supported get() = false
}
