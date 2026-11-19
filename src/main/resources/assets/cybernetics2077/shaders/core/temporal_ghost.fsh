#version 150
uniform sampler2D Sampler0;
uniform float ExclusionEnabled;uniform vec3 ExclusionMin;uniform vec3 ExclusionMax;
in vec3 echoPosition;in vec2 uv;in vec4 color;in vec3 viewNormal;in vec3 viewPosition;out vec4 fragColor;
void main(){vec4 skin=texture(Sampler0,uv);if(skin.a<0.1)discard;
float cameraFade=smoothstep(0.25,0.95,length(viewPosition));
float bodyFade=1.0;
if(ExclusionEnabled>0.5){vec3 outside=max(max(ExclusionMin-echoPosition,echoPosition-ExclusionMax),vec3(0.0));bodyFade=smoothstep(0.015,0.16,length(outside));}
if(cameraFade*bodyFade<0.005)discard;
float rim=pow(1.0-abs(dot(normalize(viewNormal),normalize(-viewPosition))),2.2);
float detail=dot(skin.rgb,vec3(0.2126,0.7152,0.0722));
vec3 tint=color.rgb*(0.65+detail*0.35)+rim*color.rgb*0.45;
fragColor=vec4(tint,skin.a*color.a*(0.38+rim*0.62)*cameraFade*bodyFade);}
