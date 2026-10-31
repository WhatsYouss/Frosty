#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:dynamictransforms.glsl>

layout(location = 0) in vec4 vertexColor;
layout(location = 1) in vec2 localUv;
layout(location = 2) flat in ivec2 radiusAndMode;
layout(location = 3) flat in ivec2 pixelSize;
layout(location = 0) out vec4 fragColor;

void main() {
    vec2 size = vec2(pixelSize);
    vec2 position = localUv * size;
    float side = min(position.x, size.x - position.x);
    float bottom = size.y - position.y;
    int mode = radiusAndMode.y;
    bool topOnly = mode < 0;
    float vertical = topOnly ? position.y : min(position.y, bottom);
    float radius = float(radiusAndMode.x);
    float stroke = float(topOnly ? max(-mode - 1, 0) : mode);

    vec2 corner = max(vec2(radius - side, radius - vertical), 0.0);
    float distance = length(corner) - radius;
    if (topOnly) distance = max(distance, -bottom);

    float antialias = max(fwidth(distance), 0.75);
    float outer = 1.0 - smoothstep(-antialias * 0.5, antialias * 0.5, distance);
    float coverage = outer;
    if (stroke > 0.0) {
        float inner = 1.0 - smoothstep(-antialias * 0.5, antialias * 0.5,
                                       distance + stroke);
        coverage -= inner;
    }
    fragColor = vec4(vertexColor.rgb, vertexColor.a * coverage) * ColorModulator;
}
