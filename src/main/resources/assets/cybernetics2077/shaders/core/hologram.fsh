#version 150
uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float GameTime;
in vec2 texCoord;
in vec4 vertexColor;
in vec3 localPosition;
in vec3 surfaceNormal;
out vec4 fragColor;
void main(){
  vec4 tex=texture(Sampler0,texCoord);
  if(tex.a<0.01)discard;
  float rim=pow(1.0-abs(normalize(surfaceNormal).z),1.7);
  float scan=pow(.5+.5*sin(localPosition.y*95.0-GameTime*9200.0),18.0);
  // Keep the player's skin recognizable. The mild tint also highlights selected regions.
  vec3 tint=mix(vec3(1.0),vertexColor.rgb,.27);
  vec3 light=tex.rgb*tint*(.94+rim*.06);
  light+=vec3(.20,.55,.68)*scan*.055+tint*rim*.025;
  // Skin layers resolve with normal depth before the complete avatar becomes translucent.
  fragColor=vec4(light,tex.a)*ColorModulator;
}
