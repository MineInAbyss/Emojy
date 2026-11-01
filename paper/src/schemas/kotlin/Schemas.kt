import com.mineinabyss.emojy.config.EmojyConfig
import com.mineinabyss.emojy.config.EmojyTemplates
import com.mineinabyss.emojy.config.Emotes
import com.mineinabyss.emojy.config.Gifs
import com.mineinabyss.jsonschema.writer.writeSchemas

fun main(args: Array<String>) = writeSchemas(args) {
    schema("emojy/config", listOf("**/plugins/Emojy/config.yml"), EmojyConfig.serializer())
    schema("emojy/templates", listOf("**/plugins/Emojy/templates.yml"), EmojyTemplates.serializer())
    schema("emojy/emotes", listOf("**/plugins/Emojy/emotes.yml"), Emotes.serializer())
    schema("emojy/gifs", listOf("**/plugins/Emojy/gifs.yml"), Gifs.serializer())
}
