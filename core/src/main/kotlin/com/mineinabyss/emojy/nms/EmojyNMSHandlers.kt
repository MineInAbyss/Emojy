package com.mineinabyss.emojy.nms

import com.mineinabyss.emojy.EmojyPlugin

object EmojyNMSHandlers {

    private val SUPPORTED_VERSION = arrayOf("v1_20_R4", "v1_21_R1")

    fun setup(emojy: EmojyPlugin): IEmojyNMSHandler {
        SUPPORTED_VERSION.forEach { version ->
            runCatching {
                return Class.forName("com.mineinabyss.emojy.nms.${version}.EmojyNMSHandler").getConstructor(EmojyPlugin::class.java)
                    .newInstance(emojy) as IEmojyNMSHandler
            }.onFailure { it.printStackTrace() }
        }

        throw IllegalStateException("Unsupported server version")
    }
}
