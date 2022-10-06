package com.mineinabyss.emojy.packets

import com.mineinabyss.idofront.messaging.miniMsg
import com.mineinabyss.idofront.messaging.serialize
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

object PacketHelpers {
    // Ugly but works :)
    fun String.readJson(): Component {
        val color = substringAfter("color\":\"").substringBefore("\",")
        val msg = this.substringBeforeLast("\"}").substringAfter("\"text\":\"")
        return Component.text(msg).color(NamedTextColor.NAMES.value(color))
    }

    fun Component.removeUnwanted(): Component {
        return this.serialize().replace("\\<", "<").miniMsg().mergeStyle("".miniMsg().clickEvent(null).hoverEvent(null).insertion(null))
    }
}
