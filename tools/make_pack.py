#!/usr/bin/env python3
"""Generate unique item icons and wearable armor skins, no third-party packages."""
import json, struct, zlib, zipfile, pathlib
ROOT=pathlib.Path("dist")
ROOT.mkdir(exist_ok=True)
DEST=ROOT/"ESN-SpeedForce-ResourcePack-1.1.0.zip"
SUITS={
    "flash":((218,34,37),(255,209,66)),
    "reverse":((238,210,41),(183,31,30)),
    "zoom":((31,31,52),(40,85,194)),
    "godspeed":((235,237,235),(233,190,52)),
    "savitar":((83,93,149),(157,208,242)),
    "esn":((127,36,213),(25,233,247)),
}
PARTS=["helmet","chestplate","leggings","boots"]
UTILITY={
    "core":((51,192,225),(239,251,255)),
    "tachyon":((169,46,220),(231,172,255)),
    "serum":((228,186,33),(255,246,160)),
    "lightning_shard":((69,196,245),(254,246,116)),
    "dampener":((76,35,97),(212,89,235)),
    "meta_cuffs":((112,115,122),(222,227,230)),
    "rift_compass":((111,46,219),(181,255,255)),
    "chrono_shard":((31,149,210),(248,233,119)),
    "trial_medal":((235,165,40),(255,251,216)),
}
def png(pixels):
    h=len(pixels); w=len(pixels[0])
    raw=b"".join(b"\x00"+b"".join(bytes(color) for color in row) for row in pixels)
    def chunk(tag,data):return struct.pack(">I",len(data))+tag+data+struct.pack(">I",zlib.crc32(tag+data)&0xffffffff)
    return b"\x89PNG\r\n\x1a\n"+chunk(b"IHDR",struct.pack(">2I5B",w,h,8,6,0,0,0))+chunk(b"IDAT",zlib.compress(raw,9))+chunk(b"IEND",b"")
def shade(base,scale):
    return tuple(min(255,max(0,int(c*scale))) for c in base)+(255,)
def blank(w,h):return [[(0,0,0,0) for _ in range(w)] for _ in range(h)]
def icon(kind,base,accent):
    out=blank(16,16)
    for y in range(2,14):
        for x in range(2,14):
            present=(kind=="ring" and (x-8)**2+(y-8)**2 in range(10,30)) or (
                kind=="helmet" and ((4<=x<=11 and 3<=y<=9) or (3<=x<=12 and y==10))) or (
                kind=="chestplate" and ((4<=x<=11 and 3<=y<=13) or (2<=x<=3 and 4<=y<=8) or (12<=x<=13 and 4<=y<=8))) or (
                kind=="leggings" and ((3<=x<=12 and 3<=y<=7) or (3<=x<=6 and 8<=y<=13) or (9<=x<=12 and 8<=y<=13))) or (
                kind=="boots" and ((3<=x<=6 and 5<=y<=12) or (9<=x<=12 and 5<=y<=12) or (2<=x<=7 and y==13) or (8<=x<=13 and y==13))) or (
                kind=="utility" and abs(x-8)+abs(y-7)<=7)
            if present:
                s=(0.7 if x in (2,3,12,13) else 1.05 if y<6 else 0.9)
                out[y][x]=shade(base,s)
    # signature forked lightning mark on each item
    for x,y in [(9,3),(8,4),(7,5),(8,5),(7,6),(6,7),(9,7),(8,8),(7,9),(6,10),(5,11)]:
        if out[y][x][3]>0:out[y][x]=(*accent,255)
    if kind=="ring":out[7][8]=(*accent,255);out[8][8]=(*accent,255)
    return png(out)
def armor(base,accent,leggings=False):
    out=blank(64,32)
    # Pixel-art speedster armor; geometry follows humanoid UV faces.
    for y in range(32):
        for x in range(64):
            diagonal=(x+y//2)%16
            mult=(0.72 if diagonal in (0,1) else 1.04 if diagonal in (3,4,5) else 0.88)
            out[y][x]=shade(base,mult)
            if x%16 in (7,8) or (x+y)%23 in (2,3):
                out[y][x]=(*accent,255)
    # Face emblem on torso/legging strip
    for x,y in [(25,20),(24,21),(23,22),(26,22),(25,23),(24,24),(23,25)]:
        if y<32:out[y][x]=(*accent,255)
    return png(out)
def dumps(obj):return json.dumps(obj,separators=(",",":"),ensure_ascii=False).encode()
with zipfile.ZipFile(DEST,"w",compression=zipfile.ZIP_DEFLATED,compresslevel=9) as pack:
    def add(path,payload):pack.writestr(path,payload)
    add("pack.mcmeta",dumps({"pack":{"pack_format":46,"description":"ESN SpeedForce 1.1 - super suits, rings, and custom items (1.21.4)"}}))
    add("PACK-INSTRUCTIONS.txt",b"Enable this resource pack on Java Edition 1.21.4. Upload it as the server resource pack or use Options > Resource Packs. Bedrock clients require a separate Geyser-converted Bedrock pack.\n")
    def item(id,kind,colors):
        ns="assets/esn_speedforce/"
        add(ns+"items/"+id+".json",dumps({"model":{"type":"minecraft:model","model":"esn_speedforce:item/"+id}}))
        add(ns+"models/item/"+id+".json",dumps({"parent":"minecraft:item/generated","textures":{"layer0":"esn_speedforce:item/"+id}}))
        add(ns+"textures/item/"+id+".png",icon(kind,*colors))
    for name,colors in SUITS.items():
        item("ring_"+name,"ring",colors)
        for part in PARTS:item(name+"_"+part,part,colors)
        model={"layers":{"humanoid":[{"texture":"esn_speedforce:"+name}],
                         "humanoid_leggings":[{"texture":"esn_speedforce:"+name}]}}
        add("assets/esn_speedforce/equipment/"+name+".json",dumps(model))
        add("assets/esn_speedforce/textures/entity/equipment/humanoid/"+name+".png",armor(*colors))
        add("assets/esn_speedforce/textures/entity/equipment/humanoid_leggings/"+name+".png",armor(*colors,leggings=True))
    for name,colors in UTILITY.items():item(name,"utility",colors)
print("Created",DEST)
