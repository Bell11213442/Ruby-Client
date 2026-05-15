#version 150

uniform sampler2D DiffuseSampler;
uniform float Radius;
uniform vec2 TexelSize;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 color = vec4(0.0);
    float total = 0.0;
    
    float blurSize = Radius * TexelSize.x;
    int sampleCount = int(Radius * 2.0) + 1;
    
    for (int x = -sampleCount; x <= sampleCount; x++) {
        for (int y = -sampleCount; y <= sampleCount; y++) {
            vec2 offset = vec2(float(x), float(y)) * blurSize;
            float weight = 1.0 / (1.0 + length(offset) * 10.0);
            color += texture(DiffuseSampler, texCoord + offset) * weight;
            total += weight;
        }
    }
    
    fragColor = color / total;
}