#version 330
#extension GL_ARB_separate_shader_objects : require
layout(location = 0) in vec3 Position;
layout(location = 0) out vec2 texCoord;
void main() {
    texCoord = Position.xy;
    gl_Position = vec4(Position.xy * 2.0 - 1.0, 0.0, 1.0);
}
