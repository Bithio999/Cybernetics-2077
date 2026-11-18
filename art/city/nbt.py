"""Small dependency-free NBT codec for reproducible structure assets."""
import gzip,struct,io
class Tag:
 def __init__(self,t,v):self.t,self.v=t,v
def S(v):return Tag(8,v)
def I(v):return Tag(3,v)
def B(v):return Tag(1,v)
def F(v):return Tag(5,v)
def D(v):return Tag(6,v)
def L(t,v):return Tag(9,(t,v))
def C(v):return Tag(10,v)
def string(v):
 b=v.encode();return struct.pack('>H',len(b))+b
def payload(t,v):
 if isinstance(v,Tag):v=v.v
 if t in [1,2,3,4,5,6]:return struct.pack({1:'>b',2:'>h',3:'>i',4:'>q',5:'>f',6:'>d'}[t],v)
 if t==8:return string(v)
 if t==9:
  a,b=v;return bytes([a])+struct.pack('>i',len(b))+b''.join(payload(a,x) for x in b)
 if t==10:return b''.join(bytes([x.t])+string(k)+payload(x.t,x.v) for k,x in v.items())+b'\0'
 raise ValueError(t)
def write(path,v):
 path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(gzip.compress(b'\x0a\0\0'+payload(10,v),mtime=0))
def read(b):
 f=io.BytesIO(gzip.decompress(b) if b[:2]==b'\x1f\x8b' else b)
 def num(fmt):return struct.unpack(fmt,f.read(struct.calcsize(fmt)))[0]
 def s():return f.read(num('>H')).decode()
 def val(t):
  if t in [1,2,3,4,5,6]:return num({1:'>b',2:'>h',3:'>i',4:'>q',5:'>f',6:'>d'}[t])
  if t==8:return s()
  if t==9:
   a,n=num('>b'),num('>i');return [val(a) for _ in range(n)]
  if t==10:
   d={}
   while (a:=num('>b'))!=0:
    k=s();d[k]=val(a)
   return d
  if t in [7,11,12]:return [num({7:'>b',11:'>i',12:'>q'}[t]) for _ in range(num('>i'))]
  raise ValueError(t)
 t=num('>b');s();return val(t)
