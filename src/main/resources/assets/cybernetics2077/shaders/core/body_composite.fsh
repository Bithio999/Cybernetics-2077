#version 150
uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
in vec2 texCoord0;
in vec4 vertexColor;
out vec4 fragColor;
void main(){
  float mask=smoothstep(0.0,0.065,texCoord0.y)*smoothstep(0.0,0.065,1.0-texCoord0.y)*smoothstep(0.0,0.035,texCoord0.x)*smoothstep(0.0,0.035,1.0-texCoord0.x);
  // Premultiplied FBO colors and alpha fade together at the anatomical crop boundary.
  vec4 tex=texture(Sampler0,texCoord0);
  vec3 bloom=vec3(0.0);
  for(int k=0;k<8;k++){
    float a=float(k)*0.78539816;vec2 off=vec2(cos(a)/768.0,sin(a)/1024.0)*3.0;
    vec3 sampleColor=texture(Sampler0,texCoord0+off).rgb;
    bloom+=sampleColor*max(0.0,max(max(sampleColor.r,sampleColor.g),sampleColor.b)-0.55)*0.018;
  }
  tex.rgb+=bloom;tex.a=max(tex.a,min(.08,length(bloom)));
  float opacity=vertexColor.a*ColorModulator.a*mask;
  fragColor=vec4(tex.rgb*vertexColor.rgb*ColorModulator.rgb*opacity,tex.a*opacity);
}
