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
  if(tex.a<.01)discard;
  float luminance=dot(tex.rgb,vec3(.2126,.7152,.0722));
  vec3 projection=mix(vec3(.10,.38,.54),vec3(.46,.94,1.0),luminance);
  // Retain skin markings and some hue, with a visibly projected blue/cyan treatment.
  vec3 light=mix(tex.rgb,projection,.68);
  float rim=pow(1.0-abs(normalize(surfaceNormal).z),2.0);
  light*=mix(vec3(1),vertexColor.rgb,.13);
  light+=vec3(.03,.15,.20)*rim;
  float height=clamp((localPosition.y+.53)/2.06,0.0,1.0);
  float sweep=1.0-smoothstep(.008,.045,abs(fract(GameTime*240.0)-height));
  light+=vec3(.18,.65,.76)*sweep*.23;
  // Depth resolves skin overlays first; transparency/scanlines are applied once afterward.
  fragColor=vec4(light,tex.a)*ColorModulator;
}
