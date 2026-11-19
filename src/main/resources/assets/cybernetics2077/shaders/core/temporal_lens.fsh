#version 150
uniform sampler2D Sampler0;uniform float Intensity;uniform float Pulse;uniform float Aspect;
in vec2 uv;out vec4 fragColor;
void main(){vec2 c=uv-0.5;vec2 p=c*vec2(Aspect,1.0);float r=length(p);
float edge=smoothstep(0.25,0.9,r);vec2 offset=c*Intensity*edge*(0.0012+Pulse*0.005);
vec2 warped=clamp(uv+c*Pulse*Intensity*0.009*sin(r*22.0),vec2(0.001),vec2(0.999));
vec3 scene=texture(Sampler0,warped).rgb;
scene.r=texture(Sampler0,clamp(warped+offset,vec2(0.001),vec2(0.999))).r;
scene.b=texture(Sampler0,clamp(warped-offset,vec2(0.001),vec2(0.999))).b;
float l=dot(scene,vec3(0.2126,0.7152,0.0722));scene=mix(scene,vec3(l),Intensity*0.18);
scene*=mix(vec3(1.0),vec3(0.91,1.02,0.95),Intensity*(0.25+edge*0.75));
scene+=vec3(0.16,0.26,0.025)*Pulse*Intensity*(0.2+edge)*0.2;
scene+=vec3(0.0,0.018,0.025)*edge*Intensity;fragColor=vec4(scene,1.0);}
