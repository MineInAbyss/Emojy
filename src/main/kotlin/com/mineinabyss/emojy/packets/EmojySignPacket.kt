package com.mineinabyss.emojy.packets

import com.comphenix.protocol.PacketType.Play.Server.MAP_CHUNK
import com.comphenix.protocol.PacketType.Play.Server.TILE_ENTITY_DATA
import com.comphenix.protocol.events.InternalStructure
import com.comphenix.protocol.events.ListenerPriority
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketEvent
import com.comphenix.protocol.reflect.EquivalentConverter
import com.comphenix.protocol.wrappers.nbt.NbtCompound
import com.comphenix.protocol.wrappers.nbt.NbtFactory
import com.mineinabyss.emojy.emojy
import com.mineinabyss.emojy.packets.EmojySignPacket.TileEntityInfo.nbt
import com.mineinabyss.emojy.replaceEmoteIds
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer
import java.lang.reflect.Constructor
import java.lang.reflect.Field

// This entire class is HEAVILY inspired by https://github.com/blablubbabc/IndividualSigns
class EmojySignPacket : PacketAdapter(
    emojy, ListenerPriority.LOWEST, TILE_ENTITY_DATA, MAP_CHUNK
) {
    private val gson = GsonComponentSerializer.gson()
    override fun onPacketSending(event: PacketEvent) {
        when (event.packetType) {
            TILE_ENTITY_DATA -> event.onSendTileEntity()
            MAP_CHUNK -> event.onSendChunk()
        }
    }

    private fun PacketEvent.onSendTileEntity() {
        val signData = packet.nbtModifier.read(0) as? NbtCompound ?: return
        if (!signData.isSignData) return
        packet.nbtModifier.write(0, replaceSignData(signData))
    }

    private fun PacketEvent.onSendChunk() {
        val tileEntitiesInfo = packet.structures.read(0).getLists(TileEntityInfo.CONVERTER).read(0)

        tileEntitiesInfo.forEachIndexed { index, structure ->
            val signData = structure.nbt ?: return@forEachIndexed
            if (!signData.isSignData) return@forEachIndexed
            tileEntitiesInfo[index] = TileEntityInfo.cloneWithNewNbt(structure, replaceSignData(signData))
        }

        packet.structures.read(0).getLists(TileEntityInfo.CONVERTER).write(0, tileEntitiesInfo)
    }

    private val NbtCompound.isSignData: Boolean
        get() = "GlowingText" in this.keys

    private fun NbtCompound.getText(): Array<String> {
        return Array(4) { "" }.apply {
            for (i in 0..3) {
                val line = getString("Text${i + 1}")
                this[i] = line ?: ""
            }
        }
    }

    private fun replaceSignData(prevSignData: NbtCompound): NbtCompound {
        val newSignData = NbtFactory.ofCompound(prevSignData.name)
        for (key in prevSignData.keys)
            newSignData.put(key, prevSignData.getValue<Any>(key))

        newSignData.setText(newSignData.getText().map { gson.serialize(gson.deserialize(it).replaceEmoteIds(null, false)) }.toTypedArray())
        return newSignData
    }

    private fun NbtCompound.setText(lines: Array<String>) {
        for (i in 0..3)
            put("Text" + (i + 1), lines[i])
    }

    // This is more or less entirely copied from first-mentioned source as I am too dumb to understand it
    @Suppress("UNCHECKED_CAST", "DEPRECATED_IDENTITY_EQUALS")
    object TileEntityInfo {
        var CONVERTER: EquivalentConverter<InternalStructure>

        init {
            try {
                val converterField: Field = InternalStructure::class.java.getDeclaredField("CONVERTER")
                converterField.isAccessible = true
                CONVERTER = converterField.get(null) as EquivalentConverter<InternalStructure>?
                    ?: throw Exception("Failed to get converter")
            } catch (e: ReflectiveOperationException) {
                throw RuntimeException("Could not get the tile entity info converter!", e)
            }
        }

        private var CONSTRUCTOR: Constructor<*>? = null

        private val InternalStructure.packedXZ get() = integers.read(0)
        private val InternalStructure.y: Int get() = integers.read(1)
        private val InternalStructure.tileEntityType get() = modifier.read(2)

        val InternalStructure.nbt: NbtCompound?
            get() {
                return NbtFactory.asCompound(nbtModifier.read(0) ?: return null) ?: null
            }

        fun cloneWithNewNbt(tileEntityInfo: InternalStructure, nbt: NbtCompound): InternalStructure {
            if (CONSTRUCTOR == null) {
                for (constructor in tileEntityInfo.handle.javaClass.declaredConstructors) {
                    // We are looking for the only constructor with 4 parameters:
                    if (constructor.parameterCount === 4) {
                        constructor.isAccessible = true
                        CONSTRUCTOR = constructor
                        break
                    }
                }
                CONSTRUCTOR ?: throw RuntimeException("Could not find the tile entity info constructor!")
            }
            return try {
                val instance = CONSTRUCTOR!!.newInstance(
                    tileEntityInfo.packedXZ,
                    tileEntityInfo.y,
                    tileEntityInfo.tileEntityType,
                    nbt.handle
                )
                CONVERTER.getSpecific(instance)
            } catch (e: ReflectiveOperationException) {
                throw RuntimeException("Could not invoke the tile entity info constructor!", e)
            }
        }
    }
}


