package com.mineinabyss.emojy.nms

object EmojyNMSHandlers {

    private val SUPPORTED_VERSION = arrayOf("v1_19_R1", "v1_19_R2")
    private var handler: IEmojyNMSHandler? = null

    fun getHandler(): IEmojyNMSHandler? {
        if (handler != null) {
            return handler
        } else {
            setup()
        }
        return handler
    }

    fun setup() {
        if (handler != null) return
        SUPPORTED_VERSION.forEach { version ->
            try {
                handler = Class.forName("com.mineinabyss.emojy.nms.${version}.EmojyNMSHandler").getConstructor()
                    .newInstance() as IEmojyNMSHandler
            } catch (e: Exception) {
                throw RuntimeException(e)
            }
        }
    }
}
