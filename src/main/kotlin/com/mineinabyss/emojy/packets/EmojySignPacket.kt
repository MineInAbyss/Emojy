package com.mineinabyss.emojy.packets

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketEvent
import com.mineinabyss.emojy.emojy


class EmojySignPacket : PacketAdapter(
    emojy, PacketType.Play.Server.TILE_ENTITY_DATA
) {
    // TODO Make this work
    override fun onPacketSending(event: PacketEvent) {
        //event.onTileEntityDataSending()
    }

}

//private fun PacketEvent.onTileEntityDataSending() {
//    try {
//        val blockPos = packet.blockPositionModifier.read(0)
//        val location = Location(player.world, blockPos.x.toDouble(), blockPos.y.toDouble(), blockPos.z.toDouble())
//        val block = location.block
//        if (!MaterialTags.SIGNS.isTagged(block)) return
//        val outPacket = packet.shallowClone()
//        val signData: NbtCompound = getTileEntityData(outPacket)
//        val outgoingSignData = replaceSignData(signData, getText(signData))
//        setTileEntityData(outPacket, outgoingSignData)
//        packet = outPacket
//
//        //player.sendSignChange(location, lines)
//    } catch (e: Exception) {
//        when (e) {
//            is NullPointerException, is FieldAccessException -> {}
//        }
//    }
//}
//
//fun getText(tileEntitySignData: NbtCompound?): Array<String?> {
//    assert(tileEntitySignData != null)
//    val lines = arrayOfNulls<String>(4)
//    for (i in 0..3) {
//        val rawLine = tileEntitySignData!!.getString("Text" + (i + 1)).miniMsg().replaceEmoteIds().serialize()
//        lines[i] = rawLine
//    }
//    return lines
//}
//
//fun getTileEntityData(packet: PacketContainer): NbtCompound {
//    return packet.nbtModifier.read(0) as NbtCompound
//}
//
//fun setTileEntityData(packet: PacketContainer, tileEntityData: NbtCompound?) {
//    packet.nbtModifier.write(0, tileEntityData)
//}
//
//private fun replaceSignData(previousSignData: NbtCompound, newSignText: Array<String?>): NbtCompound? {
//    val newSignData = NbtFactory.ofCompound(previousSignData.name)
//
//    // Copy the previous tile entity data (shallow copy):
//    for (key in previousSignData.keys) {
//        newSignData.put(key, previousSignData.getValue<Any>(key))
//    }
//
//    // Replace the sign text:
//    setText(newSignData, newSignText)
//    return newSignData
//}
//
//fun setText(tileEntitySignData: NbtCompound?, lines: Array<String?>) {
//    assert(tileEntitySignData != null)
//    assert(lines.size == 4)
//    for (i in 0..3) {
//        tileEntitySignData!!.put("Text" + (i + 1), lines[i])
//    }
//}
//
////private fun List<String>.getSignLines(player: Player?, lineNumber: Int) : List<String> {
////    var line1 = this.substringAfter("Text$lineNumber:\'").substringBefore("\',Text2")
////    var line2 = this.substringAfter("Text2:\'").substringBefore("\',Text3")
////    var line3 = this.substringAfter("Text3:\'").substringBefore("\',Text4")
////    var line4 = this.substringAfter("Text4:\'").substringBefore("\'}")
////
////    line1 = line1.replace(line1, line1.miniMsg().replaceEmoteIds(player).serialize())
////    line2 = line2.replace(line2, line2.miniMsg().replaceEmoteIds(player).serialize())
////    line3 = line3.replace(line3, line3.miniMsg().replaceEmoteIds(player).serialize())
////    line4 = line4.replace(line4, line4.miniMsg().replaceEmoteIds(player).serialize())
////
////    return listOf(line1, line2, line3, line4)
////    //return lines.toTypedArray()
////}
