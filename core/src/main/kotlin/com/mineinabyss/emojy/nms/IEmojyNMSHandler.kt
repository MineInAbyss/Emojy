package com.mineinabyss.emojy.nms

import org.bukkit.entity.Player

interface IEmojyNMSHandler {

    fun inject(player: Player) {

    }

    fun uninject(player: Player) {

    }

    val supported get() = false
}
