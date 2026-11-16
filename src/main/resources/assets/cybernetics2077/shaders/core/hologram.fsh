#version 150
uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float GameTime;
uniform float Role;
uniform float Region;
uniform float SelectedRegion;
uniform float Detail;
in vec2 texCoord;
in vec4 vertexColor;
in vec3 localPosition;
in vec3 surfaceNormal;
out vec4 fragColor;

bool role(float value){return abs(Role-value)<0.1;}
bool selected(float value){return abs(SelectedRegion-value)<0.1;}
void main(){
  vec3 n=normalize(surfaceNormal);
  float depth=clamp((localPosition.y-0.12)/1.25,0.0,1.0);
  vec3 tissue=mix(vec3(.78,.36,.46),vec3(.22,.46,.59),depth);
  vec3 key=normalize(vec3(-.45,-.72,-1.0));
  vec3 cool=normalize(vec3(.85,.1,-.5));
  float diffuse=max(0.0,dot(n,key));
  float side=max(0.0,dot(n,cool));
  float rim=pow(1.0-abs(n.z),2.8);
  float spec=pow(max(0.0,dot(n,normalize(key+vec3(0,0,-1)))),22.0);
  float coldSpec=pow(max(0.0,dot(reflect(-cool,n),vec3(0,0,-1))),45.0);
  float matchRegion=(abs(Region-SelectedRegion)<.1?1.0:0.0)*Detail;
  vec3 color=vertexColor.rgb;
  float alpha=1.0;
  if(role(0.0)){
    vec3 detail=texture(Sampler0,fract(texCoord*vec2(3,5))).rgb;
    float grain=(detail.r-.48)*.12;
    color=tissue*(.32+.38*diffuse+.10*side)+vec3(1.0,.78,.83)*spec*.82+vec3(.25,.65,.78)*coldSpec*.85;
    color+=tissue*rim*.50+grain;
    alpha=.12+.09*diffuse+.28*rim+.15*spec;
    bool head=localPosition.y<0.0;
    // The reference peels away only the selected anatomy's covering layer.
    if(head&&selected(0.0))alpha*=1.0-.94*Detail*(1.0-smoothstep(-.28,-.12,localPosition.y));
    if((selected(4.0)||selected(6.0)||selected(1.0)||selected(7.0)))alpha*=1.0-.65*Detail;
    if(head&&selected(3.0))alpha*=1.0-.38*Detail;
    if(selected(8.0))color=mix(color,color+vec3(.06,.15,.17),Detail*.5);
    alpha*=1.0+detail.g*.06;
  }else if(role(10.0)||role(11.0)){
    vec3 mantle=mix(vec3(.30,.28,.33),tissue,.32);
    color=mantle*(.45+.55*diffuse)+vec3(.78,.64,.69)*spec*.65+vec3(.16,.38,.48)*coldSpec*.4;
    alpha=role(10.0)?(.44+.18*spec):(.30+.12*spec);
    if(selected(0.0)&&role(10.0))alpha*=1.0-.96*Detail*(1.0-smoothstep(-.22,-.07,localPosition.y));
    if(selected(4.0)||selected(6.0)||selected(1.0)||selected(7.0))alpha*=1.0-.78*Detail;
  }else if(role(1.0)){
    color=tissue*(.45+.45*diffuse)+vec3(.65,.36,.42)*spec*.18;
    alpha=.21;
    if(selected(4.0)||selected(6.0)||selected(1.0)||selected(7.0))alpha*=1.0-.75*Detail;
    if(localPosition.y<0.0&&selected(0.0))alpha*=1.0-.95*Detail;
  }else if(role(2.0)){
    float show=max(matchRegion,selected(4.0)?Detail:0.0);
    color=mix(tissue*.26+vec3(.035,.047,.065),vec3(.80,.76,.77),show);
    color*=.38+.62*diffuse;color+=vec3(.08,.12,.15)*side;
    if(selected(6.0)||selected(1.0))alpha=mix(1.0,.22,Detail);
  }else if(role(3.0)){
    float show=max(matchRegion,(selected(6.0)||selected(1.0))?Detail:0.0);
    if(selected(3.0))show*=.25;
    color=mix(tissue*.18,vec3(.94,.68,.30),show);
    color*=.6+.4*diffuse;
  }else if(role(4.0)){
    color=mix(tissue*.3,vec3(.72,.25,.33),selected(7.0)?Detail:0.0)*(.45+.55*diffuse);
  }else if(role(5.0)){
    if(!selected(0.0)||Detail<.02)discard;
    color=vec3(.76,.72,.73)*(.25+.75*diffuse)+vec3(.3,.24,.26)*spec*.20;
    alpha=Detail;
  }else if(role(6.0)){
    float show=selected(3.0)?Detail:0.0;
    color=mix(vec3(.24,.31,.33),vec3(1.0,.73,.43),show);
    color*=.65+.35*diffuse;
  }else if(role(7.0)){
    if(!selected(8.0)||Detail<.02)discard;
    float cell=fract(sin(dot(floor(localPosition.xy*70.0),vec2(12.98,78.23)))*43758.54);
    color=mix(vec3(.22,.50,.55),vec3(.68,.44,.32),step(.83,cell));alpha=Detail*.57;
  }else if(role(8.0)){
    color=mix(tissue,vec3(.80,.70,.74),.32)*(.55+.45*diffuse);
    alpha=.46+.30*rim;
    if(localPosition.y<-.18&&selected(0.0))alpha*=1.0-.85*Detail;
  }else{
    color=mix(vec3(.12,.15,.18),vec3(.88,.64,.33),(selected(1.0)&&abs(Region-1.0)<.1)?Detail:0.0)*(.65+.35*diffuse);
  }
  // Quiet rolling scanner sheen; surface reflections carry the presentation.
  float scan=pow(.5+.5*sin(localPosition.y*120.0-GameTime*3300.0),24.0);
  color+=tissue*scan*.025;
  fragColor=vec4(color,alpha)*ColorModulator;
}
