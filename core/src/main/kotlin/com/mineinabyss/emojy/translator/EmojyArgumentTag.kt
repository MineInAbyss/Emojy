package com.mineinabyss.emojy.translator

import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.text.minimessage.Context
import net.kyori.adventure.text.minimessage.ParsingException
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import java.util.*


class EmojyArgumentTag(private val argumentComponents: List<ComponentLike>) : TagResolver {

    @Throws(ParsingException::class)
    override fun resolve(name: String, arguments: ArgumentQueue, ctx: Context): Tag {
        val index = arguments.popOr("No argument number provided").asInt().orElse(-1).takeUnless { it < 0 || it >= argumentComponents.size }
            ?: throw ctx.newException("Invalid argument number", arguments)
        return Tag.inserting(argumentComponents[index])
    }

    override fun has(name: String): Boolean {
        return name == NAME || name == NAME_1
    }

    companion object {
        private const val NAME = "argument"
        private const val NAME_1 = "arg"
    }
}
