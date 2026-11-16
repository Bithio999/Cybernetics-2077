#version 150
uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
in vec2 texCoord0;
in vec4 vertexColor;
out vec4 fragColor;
void main(){
  vec4 tex=texture(Sampler0,texCoord0);
  float opacity=vertexColor.a*ColorModulator.a;
  fragColor=vec4(tex.rgb*vertexColor.rgb*ColorModulator.rgb*opacity,tex.a*opacity);
}
