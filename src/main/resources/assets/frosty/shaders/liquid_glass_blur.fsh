#version 330
#extension GL_ARB_separate_shader_objects : require
uniform sampler2D DiffuseSampler;
layout(std140) uniform SamplerInfo { vec2 OutSize; vec2 InSize; };
layout(std140) uniform BlurConfig { vec4 Params; float Weights[65]; };
layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;
void main() {
    int radius = clamp(int(Params.z + 0.5), 0, 64);
    vec2 delta = Params.xy / OutSize;
    vec4 color = texture(DiffuseSampler, texCoord) * Weights[0];
    for (int index = 1; index <= radius; index++) {
        vec2 offset = delta * float(index);
        color += texture(DiffuseSampler, texCoord + offset) * Weights[index];
        color += texture(DiffuseSampler, texCoord - offset) * Weights[index];
    }
    fragColor = color;
}
