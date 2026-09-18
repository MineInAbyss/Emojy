package com.mineinabyss.emojy

import io.papermc.paper.plugin.bootstrap.BootstrapContext
import io.papermc.paper.plugin.bootstrap.PluginBootstrap
import net.kyori.adventure.text.minimessage.MiniMessage
import java.lang.reflect.Field

class EmojyBootstrap : PluginBootstrap {

    override fun bootstrap(context: BootstrapContext) {
        runCatching {
            val instancesClass = Class.forName("net.kyori.adventure.text.minimessage.MiniMessageImpl\$Instances")
            val instanceField = instancesClass.getDeclaredField("INSTANCE").apply { isAccessible = true }

            // Standard tags plus ours, which is what the shared instance is expected to be
            val withEmojyTags = MiniMessage.builder()
                .editTags { it.resolver(EmojyTags.RESOLVER) }
                .build()

            instanceField.setStaticFinal(withEmojyTags)
            context.logger.info("Set EmojyTags Resolver in MM instance")
        }.onFailure {
            context.logger.error("Failed to edit MiniMessage resolver: ${it.cause?.message ?: it.message}")
            it.printStackTrace()
        }
    }
}

/**
 * MiniMessage exposes no way to add a resolver to the instance everything else parses with, so the
 * static field holding it has to be replaced. Field.set and VarHandles both refuse a final field,
 * leaving Unsafe as the one route the JVM still permits.
 *
 * Note this replaces the instance rather than editing the parser inside it, because MiniMessageParser
 * became a record and records cannot be mutated by any means.
 */
private fun Field.setStaticFinal(value: Any) {
    val unsafe = Class.forName("sun.misc.Unsafe")
        .getDeclaredField("theUnsafe").apply { isAccessible = true }.get(null)
    val unsafeClass = unsafe.javaClass
    val base = unsafeClass.getMethod("staticFieldBase", Field::class.java).invoke(unsafe, this)
    val offset = unsafeClass.getMethod("staticFieldOffset", Field::class.java).invoke(unsafe, this) as Long
    unsafeClass.getMethod("putObject", Any::class.java, Long::class.javaPrimitiveType, Any::class.java)
        .invoke(unsafe, base, offset, value)
}
