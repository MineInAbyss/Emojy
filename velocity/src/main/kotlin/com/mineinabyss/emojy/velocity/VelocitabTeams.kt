package com.mineinabyss.emojy.velocity

import com.velocitypowered.proxy.protocol.MinecraftPacket
import net.kyori.adventure.text.Component
import net.william278.velocitab.packet.UpdateTeamsPacket

// Kept apart so Velocitab classes are only loaded when it is installed
object VelocitabTeams {
    // Velocitab sends one instance to every viewer, so each viewer gets its own copy
    fun transform(packet: MinecraftPacket, transform: (Component) -> Component): MinecraftPacket {
        if (packet !is UpdateTeamsPacket) return packet
        return UpdateTeamsPacket(
            packet.plugin(), packet.teamName(), packet.mode(),
            packet.displayName()?.let(transform), packet.friendlyFlags(), packet.nametagVisibility(),
            packet.collisionRule(), packet.color(),
            packet.prefix()?.let(transform), packet.suffix()?.let(transform), packet.entities(),
        )
    }
}
