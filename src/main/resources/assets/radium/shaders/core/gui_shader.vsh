#version 150

in vec3 Position;
in vec2 UV;

out vec2 texCoord;

uniform mat4 ProjMat;
uniform mat4 ModelViewMat;

void main() {
    texCoord = UV;
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
}