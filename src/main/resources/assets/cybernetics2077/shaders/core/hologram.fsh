#version 150
uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float GameTime;
in vec2 texCoord;
in vec4 vertexColor;
in vec3 localPosition;
in vec3 surfaceNormal;
out vec4 fragColor;
void main() {
  vec4 tex=texture(Sampler0,texCoord);
  if(tex.a<0.01)discard;
  float rim=pow(1.0-abs(normalize(surfaceNormal).z),1.7);
  float scan=pow(0.5+0.5*sin(localPosition.y*95.0-GameTime*9200.0),18.0);
  float wash=0.94+0.06*sin(GameTime*3800.0);
  vec3 light=tex.rgb*vertexColor.rgb*(0.90+rim*0.25)*wash;
  light+=vec3(0.24,0.53,0.66)*scan*0.22+vertexColor.rgb*rim*0.08;
  fragColor=vec4(light,tex.a*vertexColor.a)*ColorModulator;
}
