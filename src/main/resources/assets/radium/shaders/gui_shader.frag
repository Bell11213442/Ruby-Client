#version 150

uniform vec2 resolution;
uniform float time;
out vec4 fragColor;

void main() {
    vec2 p = (gl_FragCoord.xy * 2.0 - resolution) / min(resolution.x, resolution.y);
    for (int i = 1; i < 7; i++) {
        p.x += 0.3 / float(i) * sin(float(i) * 2.0 * p.y + time);
        p.y += 0.3 / float(i) * cos(float(i) * 1.5 * p.x + time);
    }
    fragColor = vec4(0.5 + 0.5 * cos(p.x), 0.5 + 0.5 * cos(p.y), 0.5 + 0.5 * sin(p.x + p.y), 1.0);
}