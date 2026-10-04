#version 150
uniform sampler2D Sampler0;
uniform sampler2D Sampler1;
layout(std140) uniform SamplerInfo { vec2 OutSize; vec2 InSize; };
layout(std140) uniform PanelInfo {
    vec4 Rect;
    vec4 Radius;
    vec4 Tint;
    vec4 Optics0;
    vec4 Optics1;
    vec4 Effects;
};
in vec2 texCoord;
out vec4 fragColor;

vec3 roundedBox(vec2 point, vec2 halfSize, vec4 radius) {
    radius.xy = point.x > 0.0 ? radius.xy : radius.zw;
    float corner = point.y > 0.0 ? radius.x : radius.y;
    vec2 edge = abs(point) - (halfSize - corner);
    vec2 signPoint = vec2(point.x < 0.0 ? -1.0 : 1.0, point.y < 0.0 ? -1.0 : 1.0);
    float outside = length(max(edge, 0.0));
    float distance = (max(edge.x, edge.y) > 0.0 ? outside : max(edge.x, edge.y)) - corner;
    vec2 normal = outside > 0.0001 ? max(edge, 0.0) / outside : (edge.x > edge.y ? vec2(1.0, 0.0) : vec2(0.0, 1.0));
    return vec3(distance, signPoint * normal);
}

void main() {
    vec2 coord = gl_FragCoord.xy;
    vec2 uv = coord / InSize;
    vec3 base = texture(Sampler0, uv).rgb;
    vec2 center = Rect.xy + Rect.zw * 0.5;
    vec3 shape = roundedBox(coord - center, Rect.zw * 0.5, Radius);
    float distance = shape.x;
    if (distance > 0.0) {
        // Fade before the 32 px composite scissor so its rectangular edge is invisible.
        float shadow = exp(-distance / max(Effects.y, 0.01)) * Effects.z;
        float shadowFade = 1.0 - smoothstep(24.0, 32.0, distance);
        fragColor = vec4(mix(base, vec3(0.0), shadow * shadowFade * 0.18), 1.0);
        return;
    }

    vec2 normal = normalize(shape.yz);
    float depth = -distance;
    float thickness = Optics0.x;
    float incident = asin(pow(clamp(1.0 - depth / thickness, 0.0, 1.0), 2.0));
    float transmitted = asin(sin(incident) / Optics0.y);
    float edgeFactor = depth >= thickness ? 0.0 : -tan(transmitted - incident);
    vec2 offset = -normal * edgeFactor * 0.08 * vec2(InSize.y / InSize.x, 1.0);
    vec3 refracted = vec3(
        texture(Sampler1, uv + offset * 1.015).r,
        texture(Sampler1, uv + offset).g,
        texture(Sampler1, uv + offset * 0.985).b
    );
    vec3 color = mix(refracted, Tint.rgb, Tint.a * 0.8);
    float rim = pow(clamp(1.0 - depth / max(Optics1.z, 1.0), 0.0, 1.0), 3.0);
    color = mix(color, mix(vec3(1.0), Tint.rgb, 0.35), rim * Optics1.y);
    float glare = 0.5 + 0.5 * sin((coord.x + coord.y) * 0.025 + Effects.x);
    color += glare * rim * 0.14;
    float alpha = smoothstep(0.5, -0.5, distance);
    fragColor = vec4(mix(base, color, alpha), 1.0);
}
