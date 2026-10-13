package com.mineinabyss.emojy

import com.mineinabyss.emojy.config.Emote
import com.mineinabyss.emojy.config.Gif
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.key.Key
import net.kyori.adventure.permission.PermissionChecker

interface EmoteRegistry {
    val emotes: Array<Emote>
    val gifs: Array<Gif>
    val spaceFont: Key
    val requirePermissions: Boolean

    // Adventure's checker reports permissions only granted through op or a permission default as unset, so platforms that know better override this
    fun hasPermission(audience: Audience, permission: String): Boolean =
        audience.get(PermissionChecker.POINTER).map { it.test(permission) }.orElse(true)

    companion object {
        @Volatile
        var instance: EmoteRegistry? = null
    }
}

val emoteRegistry get() = EmoteRegistry.instance ?: error("Emojy not loaded")
