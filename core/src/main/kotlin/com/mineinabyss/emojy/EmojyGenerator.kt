package com.mineinabyss.emojy

import com.aaaaahhhhhhh.bananapuncher714.gifconverter.GifConverter
import com.mineinabyss.emojy.config.Gif
import com.mineinabyss.emojy.config.Gifs
import com.mineinabyss.idofront.font.Space
import com.mineinabyss.idofront.font.Space.Companion.toNumber
import com.mineinabyss.idofront.messaging.broadcastVal
import com.mineinabyss.idofront.resourcepacks.ResourcePacks
import net.kyori.adventure.key.Key
import team.unnamed.creative.ResourcePack
import team.unnamed.creative.base.Writable
import team.unnamed.creative.font.BitMapFontProvider
import team.unnamed.creative.font.Font
import team.unnamed.creative.font.FontProvider
import team.unnamed.creative.font.SpaceFontProvider
import team.unnamed.creative.serialize.minecraft.MinecraftResourcePackWriter
import team.unnamed.creative.texture.Texture
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.io.path.Path
import kotlin.math.ceil
import kotlin.math.sqrt

object EmojyGenerator {
    val gifFolder = emojy.plugin.dataFolder.resolve("gifs").apply { mkdirs() }
    private val emotesFolder = emojy.plugin.dataFolder.resolve("emotes").apply { mkdirs() }
    private val spaceProvider = FontProvider.space(Space.entries.asSequence().filterNot(Space.NULL::equals).associate { it.unicode to it.toNumber() })

    fun generateResourcePack() {
        val resourcePack = ResourcePack.resourcePack()
        emojy.plugin.dataFolder.resolve("assets").deleteRecursively()

        val textureFiles = emotesFolder.walkTopDown().filter { it.isFile }.associateBy { it.name }
        val fontSpaceProvider = FontProvider.space().advance("\uE101", -1).build()
        emojy.emotes.forEach { emote ->
            resourcePack.font(emote.font)?.takeIf { emote.isMultiBitmap }?.let { font ->
                when {
                    // Add a -1 advance to the font for ease of use
                    // Mainly needed for ESC menu and default font due to no other font being supported
                    font.providers().none { it is SpaceFontProvider } ->
                        resourcePack.font(font.toBuilder().addProvider(fontSpaceProvider).build())
                    // If the font has already added an entry for the emote, skip it
                    font.providers().any { it is BitMapFontProvider && it.file() == emote.texture } ->
                        return@forEach emojy.logger.w("Skipping ${emote.id}-font because it is a bitmap and already added")
                            .let { null }
                }
            }

            emote.appendFont(resourcePack).addTo(resourcePack)
            val texture = textureFiles[emote.texture.value().substringAfterLast("/")]
            if (texture == null && ResourcePacks.defaultVanillaResourcePack?.texture(emote.texture) == null)
                emojy.logger.w("Could not find texture for ${emote.id}")
            Texture.texture(emote.texture, texture?.let(Writable::file) ?: Writable.EMPTY).addTo(resourcePack)
        }

        emojy.gifs.forEach {
            it.generateSplitGif(resourcePack)
            it.font().addTo(resourcePack)
        }
        Font.font(emojyConfig.spaceFont, spaceProvider).addTo(resourcePack)

        generateGifShaderFiles(resourcePack)

        resourcePack.packMeta(48, "")

        File("C:\\Users\\Sivert\\AppData\\Roaming\\ModrinthApp\\profiles\\MineInAbyss - 1.21\\resourcepacks\\pack").deleteRecursively()
        MinecraftResourcePackWriter.minecraft().writeToDirectory(File("C:\\Users\\Sivert\\AppData\\Roaming\\ModrinthApp\\profiles\\MineInAbyss - 1.21\\resourcepacks\\pack"), resourcePack)

        MinecraftResourcePackWriter.minecraft().writeToZipFile(emojy.plugin.dataFolder.resolve("pack.zip"), resourcePack)
    }

    private fun generateGifShaderFiles(resourcePack: ResourcePack) {
        val fsh = Writable.stringUtf8(
            """
                #version 150

                #moj_import <fog.glsl>

                uniform sampler2D Sampler0;
                uniform vec4 ColorModulator;
                uniform float FogStart, FogEnd;
                uniform vec4 FogColor;

                in float vertexDistance;
                in vec4 vertexColor;
                in vec2 texCoord0;

                out vec4 fragColor;

                void main() {
                  vec4 v = texture(Sampler0, texCoord0) * vertexColor * ColorModulator;
                  if (v.w == 0 ) {
                    discard;
                  }
                  fragColor = linear_fog(v, vertexDistance, FogStart, FogEnd, FogColor);
                }
            """.trimIndent()
        )
        val vsh = Writable.stringUtf8(
            """
                #version 150

                in vec3 Position;
                in vec4 Color;
                in vec2 UV0;
                in ivec2 UV2;

                uniform sampler2D Sampler0, Sampler2;
                uniform mat4 ModelViewMat, ProjMat;
                uniform float GameTime;

                out float vertexDistance;
                out vec4 vertexColor;
                out vec2 texCoord0;

                void main() {
                  gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.);
                  vertexDistance = length((ModelViewMat * vec4(Position, 1.)).xyz);
                  texCoord0 = UV0;

                  if ( Color.xyz == vec3( 254 ) / 255.0 ) {
                    vec2 dimensions = textureSize( Sampler0, 0 );
                    vec2 texShift = 1 / dimensions;

                    // Just in case the texture is not its own image
                    // Otherwise we could just fetch the pixel at 0, 0
                    ivec2 quadrantUV = ivec2( UV0 * dimensions );
                    vec4 quadrant = texelFetch( Sampler0, quadrantUV, 0 );
                    vec2 newUV0 = UV0;
                    if ( quadrant.a == ( 149.0 / 255.0 ) ) {
                      vec4 infoPix1 = vec4( 0 );
                      vec4 infoPix2 = vec4( 0 );
                      vertexColor = vec4( 1 );
                      if ( quadrant.r == 1.0 / 255.0 ) {
                        infoPix1 = texelFetch( Sampler0, quadrantUV + ivec2( 1, 0 ), 0 );
                        infoPix2 = texelFetch( Sampler0, quadrantUV + ivec2( 0, 1 ), 0 );
                        newUV0 = newUV0 + ( quadrant.gb * 255 + 1 ) / dimensions;
                      } else if ( quadrant.r == 0.0 / 255.0 ) {
                        infoPix1 = texelFetch( Sampler0, quadrantUV - ivec2( 1, 0 ), 0 );
                        infoPix2 = texelFetch( Sampler0, quadrantUV + ivec2( 0, 1 ), 0 );
                        newUV0 = newUV0 + ( quadrant.gb * 255 + vec2( -1, 1 ) ) / dimensions;
                      } else if ( quadrant.r == 3.0 / 255.0 ) {
                        infoPix1 = texelFetch( Sampler0, quadrantUV - ivec2( 1, 0 ), 0 );
                        infoPix2 = texelFetch( Sampler0, quadrantUV - ivec2( 0, 1 ), 0 );
                        newUV0 = newUV0 + ( quadrant.gb * 255 - 1 ) / dimensions;
                      } else if ( quadrant.r == 2.0 / 255.0 ) {
                        infoPix1 = texelFetch( Sampler0, quadrantUV + ivec2( 1, 0 ), 0 );
                        infoPix2 = texelFetch( Sampler0, quadrantUV - ivec2( 0, 1 ), 0 );
                        newUV0 = newUV0 + ( quadrant.gb * 255 + vec2( 1, -1 ) ) / dimensions;
                      } else {
                        vertexColor = Color * texelFetch( Sampler2, UV2 / 16, 0 );
                        return;
                      }

                      // Get timing info
                      float totalTime = infoPix1.r * 256 + infoPix1.g;
                      float startTime = infoPix1.b * 256 + infoPix1.a;
                      float endTime = infoPix2.r * 256 + infoPix2.g;

                      float lower = startTime / totalTime;
                      float upper = endTime / totalTime;
                      float total = totalTime / 4705.882352941176;
                      float whole = 0;
                      float time = modf( GameTime / total, whole );

                      vertexColor = vec4( time >= lower && time < upper );

                      texCoord0 = newUV0;
                    } else {
                      vertexColor = Color * texelFetch( Sampler2, UV2 / 16, 0 );
                    }
                  } else if ( Color.xyz == vec3( floor( 254 / 4. ) / 255. ) ) {
                    // Get rid of shadows
                    vertexColor = vec4( 0 );
                  } else {
                    vertexColor = Color * texelFetch(Sampler2, UV2 / 16, 0);
                  }
                }
            """.trimIndent()
        )
        val json = Writable.stringUtf8(
            """
                {
                  "blend": {
                    "func": "add",
                    "srcrgb": "srcalpha",
                    "dstrgb": "1-srcalpha"
                  },
                  "vertex": "rendertype_text",
                  "fragment": "rendertype_text",
                  "attributes": [
                    "Position",
                    "Color",
                    "UV0",
                    "UV2"
                  ],
                  "samplers": [
                    {
                      "name": "Sampler0"
                    },
                    {
                      "name": "Sampler2"
                    }
                  ],
                  "uniforms": [
                    {
                      "name": "ModelViewMat",
                      "type": "matrix4x4",
                      "count": 16,
                      "values": [ 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1 ]
                    },
                    {
                      "name": "ProjMat",
                      "type": "matrix4x4",
                      "count": 16,
                      "values": [ 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1 ]
                    },
                    {
                      "name": "GameTime",
                      "type": "float",
                      "count": 1,
                      "values": [ 0 ]
                    },
                    {
                      "name": "ColorModulator",
                      "type": "float",
                      "count": 4,
                      "values": [ 1, 1, 1, 1 ]
                    },
                    {
                      "name": "FogStart",
                      "type": "float",
                      "count": 1,
                      "values": [ 0 ]
                    },
                    {
                      "name": "FogEnd",
                      "type": "float",
                      "count": 1,
                      "values": [ 1 ]
                    },
                    {
                      "name": "FogColor",
                      "type": "float",
                      "count": 4,
                      "values": [ 0, 0, 0, 0 ]
                    }
                  ]
                }
            """.trimIndent()
        )

        resourcePack.unknownFile("assets/minecraft/shaders/core/rendertype_text.json", json)
        resourcePack.unknownFile("assets/minecraft/shaders/core/rendertype_text.fsh", fsh)
        resourcePack.unknownFile("assets/minecraft/shaders/core/rendertype_text.vsh", vsh)
    }
}
