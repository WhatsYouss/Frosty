#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:dynamictransforms.glsl>
#include <minecraft:projection.glsl>

layout(location = 0) in vec3 Position;
layout(location = 1) in vec4 Color;
layout(location = 2) in vec2 UV0;
layout(location = 3) in ivec2 UV1;
layout(location = 4) in ivec2 UV2;
layout(location = 5) in vec3 Normal;

layout(location = 0) out vec4 vertexColor;
layout(location = 1) out vec2 localUv;
layout(location = 2) flat out ivec2 radiusAndMode;
layout(location = 3) flat out ivec2 pixelSize;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexColor = Color;
    localUv = UV0;
    radiusAndMode = UV1;
    pixelSize = UV2;
}
