package com.mineinabyss.emojy.translator

import net.kyori.adventure.text.ComponentLike
import net.kyori.adventure.text.minimessage.Context
import net.kyori.adventure.text.minimessage.ParsingException
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.ArgumentQueue
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import java.util.*


class EmojyArgumentTag(argumentComponents: List<ComponentLike?>) : TagResolver {
    private val argumentComponents: List<ComponentLike?>

    init {
        this.argumentComponents = Objects.requireNonNull(argumentComponents, "argumentComponents")
    }

    @Throws(ParsingException::class)
    override fun resolve(name: String, arguments: ArgumentQueue, ctx: Context): Tag {
        val index = arguments.popOr("No argument number provided").asInt().orElseThrow {
            ctx.newException(
                "Invalid argument number",
                arguments
            )
        }
        if (index < 0 || index >= argumentComponents.size) {
            throw ctx.newException("Invalid argument number", arguments)
        }
        return Tag.inserting(argumentComponents[index]!!)
    }

    override fun has(name: String): Boolean {
        return name == NAME || name == NAME_1
    }

    companion object {
        private const val NAME = "argument"
        private const val NAME_1 = "arg"
    }
}
