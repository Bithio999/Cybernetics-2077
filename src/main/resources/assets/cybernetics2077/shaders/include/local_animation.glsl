// Original local-time sampling. Vanilla's installed shader retains lighting/fog/cutout handling.
uniform sampler2DArray CyberFrames;
uniform sampler2D CyberTimeline;
uniform sampler2D CyberCells;
uniform vec4 CyberRects[64];
uniform vec4 CyberAnimations[64];
uniform int CyberCount;
uniform vec3 CyberCameraCell;
uniform vec3 CyberCameraRem;
uniform float CyberPartial;
in vec3 cyberWorld;

vec3 cyberClock(ivec3 node) {
    uint hash = (uint(node.x) * 73856093u ^ uint(node.y) * 19349663u ^ uint(node.z) * 83492791u) & 131071u;
    for (int i=0; i<16; i++) {
        uint pixel=hash*2u;
        ivec2 uv=ivec2(int(pixel&1023u),int(pixel>>10u));
        vec4 key=texelFetch(CyberCells,uv,0);
        if (key.w==0.0) break;
        if (all(equal(ivec3(key.xyz),node))) return texelFetch(CyberCells,uv+ivec2(1,0),0).xyz;
        hash=(hash+1u)&131071u;
    }
    return vec3(0.0,1.0,0.0);
}

// Four-node tetrahedral interpolation is continuous without eight spatial texture lookups.
vec3 cyberLocalClock() {
    vec3 p=(cyberWorld+CyberCameraRem)/4.0;
    ivec3 base=ivec3(floor(p))+ivec3(CyberCameraCell);
    vec3 f=fract(p);
    ivec3 a,b; float hi,mid,lo;
    if (f.x>=f.y) {
        if (f.y>=f.z) { a=ivec3(1,0,0);b=ivec3(1,1,0);hi=f.x;mid=f.y;lo=f.z; }
        else if (f.x>=f.z) { a=ivec3(1,0,0);b=ivec3(1,0,1);hi=f.x;mid=f.z;lo=f.y; }
        else { a=ivec3(0,0,1);b=ivec3(1,0,1);hi=f.z;mid=f.x;lo=f.y; }
    } else {
        if (f.x>=f.z) { a=ivec3(0,1,0);b=ivec3(1,1,0);hi=f.y;mid=f.x;lo=f.z; }
        else if (f.y>=f.z) { a=ivec3(0,1,0);b=ivec3(0,1,1);hi=f.y;mid=f.z;lo=f.x; }
        else { a=ivec3(0,0,1);b=ivec3(0,1,1);hi=f.z;mid=f.y;lo=f.x; }
    }
    return cyberClock(base)*(1.0-hi)+cyberClock(base+a)*(hi-mid)
        +cyberClock(base+b)*(mid-lo)+cyberClock(base+ivec3(1)) * lo;
}

vec4 cyberSample(vec2 uv) {
    vec4 nativeColor=texture(Sampler0,uv);
    for (int i=0;i<64;i++) {
        if (i>=CyberCount) break;
        vec4 rect=CyberRects[i];
        if (uv.x<rect.x || uv.y<rect.y || uv.x>=rect.z || uv.y>=rect.w) continue;
        vec3 clock=cyberLocalClock();
        if(clock.z<0.001) return nativeColor;
        vec4 info=CyberAnimations[i];
        float time=mod(info.z-clock.x-(1.0-clock.y)*CyberPartial,info.y);
        int index=int(info.x+floor(time));
        vec4 step=texelFetch(CyberTimeline,ivec2(index&1023,index>>10),0);
        vec2 local=clamp((uv-rect.xy)/(rect.zw-rect.xy),0.0,1.0);
        vec4 first=texture(CyberFrames,vec3(local,step.x));
        vec4 next=texture(CyberFrames,vec3(local,step.y));
        vec4 result=vec4(mix(first.rgb,next.rgb,step.z+fract(time)*step.w),first.a);
        return mix(nativeColor,result,clock.z);
    }
    return nativeColor;
}
