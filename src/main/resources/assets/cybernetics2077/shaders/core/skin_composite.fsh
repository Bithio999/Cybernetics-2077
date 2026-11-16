#version 150
uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
in vec2 texCoord0;
in vec4 vertexColor;
out vec4 fragColor;
void main(){
  vec4 tex=texture(Sampler0,texCoord0);
  vec2 stepSize=1.0/vec2(textureSize(Sampler0,0));
  float inner=1.0,nearby=0.0;
  for(int k=0;k<8;k++){
    float angle=float(k)*0.78539816;
    vec2 direction=vec2(cos(angle),sin(angle));
    inner=min(inner,texture(Sampler0,texCoord0+direction*stepSize).a);
    nearby=max(nearby,texture(Sampler0,texCoord0+direction*stepSize*2.0).a);
  }
  vec3 cyan=vec3(.26,.86,1.0);
  float contour=max(0.0,tex.a-inner);
  float glow=max(0.0,nearby-tex.a);
  // Physical-pixel scan spacing remains consistent at every GUI scale and zoom level.
  float scan=mod(floor(gl_FragCoord.y),3.0)<1.0?.68:1.0;
  float opacity=vertexColor.a*ColorModulator.a;
  vec3 color=(tex.rgb+cyan*contour*.30)*scan+cyan*glow*.16;
  fragColor=vec4(color*vertexColor.rgb*ColorModulator.rgb*opacity,(tex.a*scan+glow*.12)*opacity);
}
