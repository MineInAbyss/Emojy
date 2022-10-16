package com.mineinabyss.emojy.packets

import com.mineinabyss.idofront.textcomponents.miniMsg
import com.mineinabyss.idofront.textcomponents.serialize
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer

object PacketHelpers {
    val gson = GsonComponentSerializer.gson()
    fun String.readJson(): Component {
        return gson.deserialize(this).serialize().replace("\\", "").miniMsg()
    }
}
