#version 150

#moj_import <minecraft:fog.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV2;

uniform sampler2D Sampler2;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform int FogShape;

out float vertexDistance;
out vec4 vertexColor;
out vec2 texCoord0;

//Emojy uniforms - end
uniform sampler2D Sampler0;
uniform float GameTime;
//Emojy uniforms - end

void main() {
  gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

  vertexDistance = fog_distance(Position, FogShape);
  vertexColor = Color * texelFetch(Sampler2, UV2 / 16, 0);
  texCoord0 = UV0;

  //Emojy GIFs - start
  vec2 dimensions = textureSize(Sampler0, 0);
  ivec2 quadrantUV = ivec2(UV0 * dimensions);
  vec4 quadrant = texelFetch(Sampler0, quadrantUV, 0);

  bool isAnimatedGlyph = Color.xyz == vec3(254) / 255.0 && quadrant.a == 149.0 / 255.0;
  bool isAnimatedGlyphShadow = Color.xyz == vec3(floor(254.0 / 4.0) / 255.0) && quadrant.a == 149.0 / 255.0;

  if (isAnimatedGlyph || isAnimatedGlyphShadow) {
    vec2 newUV0 = UV0;
    vec4 infoPix1 = vec4(0);
    vec4 infoPix2 = vec4(0);
    vertexColor = vec4(1);

    // Determine texture fetch offsets based on quadrant color value
    if (quadrant.r == 1.0 / 255.0) {
      infoPix1 = texelFetch(Sampler0, quadrantUV + ivec2(1, 0), 0);
      infoPix2 = texelFetch(Sampler0, quadrantUV + ivec2(0, 1), 0);
      newUV0 += (quadrant.gb * 255.0 + 1.0) / dimensions;
    } else if (quadrant.r == 0.0 / 255.0) {
      infoPix1 = texelFetch(Sampler0, quadrantUV - ivec2(1, 0), 0);
      infoPix2 = texelFetch(Sampler0, quadrantUV + ivec2(0, 1), 0);
      newUV0 += (quadrant.gb * 255.0 - vec2(1.0, -1.0)) / dimensions;
    } else if (quadrant.r == 3.0 / 255.0) {
      infoPix1 = texelFetch(Sampler0, quadrantUV - ivec2(1, 0), 0);
      infoPix2 = texelFetch(Sampler0, quadrantUV - ivec2(0, 1), 0);
      newUV0 += (quadrant.gb * 255.0 - 1.0) / dimensions;
    } else if (quadrant.r == 2.0 / 255.0) {
      infoPix1 = texelFetch(Sampler0, quadrantUV + ivec2(1, 0), 0);
      infoPix2 = texelFetch(Sampler0, quadrantUV - ivec2(0, 1), 0);
      newUV0 += (quadrant.gb * 255.0 + vec2(1.0, -1.0)) / dimensions;
    } else {
      vertexColor = Color * texelFetch(Sampler2, UV2 / 16, 0);
      return;
    }

    float totalTime = infoPix1.r * 256.0 + infoPix1.g;
    float startTime = infoPix1.b * 256.0 + infoPix1.a;
    float endTime = infoPix2.r * 256.0 + infoPix2.g;

    float lower = startTime / totalTime;
    float upper = endTime / totalTime;
    float total = totalTime / 4705.882352941176;

    float time = mod(GameTime / total, 1.0);
    float visible = float(time >= lower && time < upper);

    vertexColor = vec4(Color.rgb, Color.a * visible) * texelFetch(Sampler2, UV2 / 16, 0);
    texCoord0 = newUV0;
  }
  //Emojy GIFs - end
}
