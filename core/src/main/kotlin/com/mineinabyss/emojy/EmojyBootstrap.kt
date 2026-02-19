package com.mineinabyss.emojy

import io.papermc.paper.plugin.bootstrap.BootstrapContext
import io.papermc.paper.plugin.bootstrap.PluginBootstrap
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver

class EmojyBootstrap : PluginBootstrap {

    override fun bootstrap(context: BootstrapContext) {
        runCatching {
            val instancesClass = Class.forName("net.kyori.adventure.text.minimessage.MiniMessageImpl\$Instances")
            val miniMessageImpl = instancesClass.getDeclaredField("INSTANCE").apply { isAccessible = true }.get(null) // MiniMessageImpl
            val parser = miniMessageImpl.javaClass.getDeclaredField("parser").apply { isAccessible = true }.get(miniMessageImpl)

            val resolverField = parser.javaClass.getDeclaredField("tagResolver").apply { isAccessible = true }
            val oldResolver = resolverField.get(parser) as TagResolver
            val combinedResolver = TagResolver.resolver(oldResolver, EmojyTags.RESOLVER)
            resolverField.set(parser, combinedResolver)
            context.logger.info("Set EmojyTags Resolver in MM instance")
        }.onFailure {
            context.logger.error("Failed to edit MiniMessage resolver: ${it.message}")
            it.printStackTrace()
        }
    }
}