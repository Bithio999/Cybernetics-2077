#version 150
in vec3 Position;in vec4 Color;in vec2 UV0;in vec3 Normal;
uniform vec4 ColorModulator;uniform mat4 ModelViewMat;uniform mat4 ProjMat;
out vec3 echoPosition;out vec2 uv;out vec4 color;out vec3 viewNormal;out vec3 viewPosition;
void main(){echoPosition=Position;vec4 p=ModelViewMat*vec4(Position,1.0);gl_Position=ProjMat*p;uv=UV0;color=Color*ColorModulator;viewNormal=mat3(ModelViewMat)*Normal;viewPosition=p.xyz;}
