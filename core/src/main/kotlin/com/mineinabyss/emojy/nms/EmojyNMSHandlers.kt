package com.mineinabyss.emojy.nms

object EmojyNMSHandlers {

    private val SUPPORTED_VERSION = arrayOf("v1_19_R1", "v1_19_R2", "v1_19_R3", "v1_20_R1", "v1_20_R2", "v1_20_R3")
    private var handler: IEmojyNMSHandler? = null

    fun getHandler(): IEmojyNMSHandler? {
        when {
            handler != null -> return handler
            else -> setup()
        }
        return handler
    }

    private fun setup() {
        if (handler != null) return
        SUPPORTED_VERSION.forEach { version ->
            runCatching {
                handler = Class.forName("com.mineinabyss.emojy.nms.${version}.EmojyNMSHandler").getConstructor()
                    .newInstance() as IEmojyNMSHandler
            }
        }
    }
}
