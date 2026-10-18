"""Original JET Worlds art and soundtrack content; CC0-1.0.
Reproducible: python tools/generate_expansion_content.py (numpy + Pillow).
No third-party media, sampled audio, downloads, or placeholder padding.
"""
from pathlib import Path
import argparse,json,math,wave
import numpy as np
from PIL import Image,ImageDraw,ImageFilter
ROOT=Path(__file__).resolve().parents[1]
OUT=ROOT/'app/src/main/assets/content'
PREVIEW=ROOT/'docs/previews'
W,H=1080,1920
RATE=44100
DURATION=112
TRACK_DURATIONS={}
THEMES=[
('aurora','Aurora Gardens','باغ شفق',(91,239,196),(97,74,197),'aurora_drift'),
('solar','Solar Frontier','مرز خورشید',(255,177,74),(225,74,143),'solar_heartbeat'),
('ocean','Ocean of Stars','اقیانوس ستاره‌ها',(62,189,251),(49,91,193),'ocean_glide'),
('candy','Candy Cosmos','کهکشان آب‌نبات',(252,127,193),(159,128,251),'candy_bounce'),
('cyber','Neon Metropolis','شهر نئونی',(89,233,219),(100,89,239),'cyber_cruise'),
('crystal','Crystal Caverns','غار بلور',(139,161,254),(63,226,216),'crystal_echo'),
('lunar','Moonlight Station','ایستگاه مهتاب',(161,181,250),(134,90,192),'ocean_glide'),
('rainbow','Rainbow Runway','مسیر رنگین‌کمان',(129,240,177),(250,155,154),'candy_bounce'),
('forest','Firefly Forest','جنگل شب‌تاب',(114,228,132),(61,131,211),'forest_fireflies'),
('coral','Coral Kingdom','سرزمین مرجان',(74,226,215),(236,123,185),'coral_waltz'),
('toy','Toy Planet','سیاره اسباب‌بازی',(253,197,97),(123,152,248),'toy_parade'),
('arcade','Arcade Circuit','مدار آرکید',(247,115,233),(97,227,248),'arcade_flux'),
('sunset','Sunset Islands','جزیره‌های غروب',(251,162,100),(167,102,197),'sunset_soar'),
('volcano','Ember Volcano','آتشفشان اخگر',(255,139,63),(205,81,110),'solar_heartbeat'),
('snow','Snowflake Peaks','قله‌های برفی',(165,225,254),(112,133,225),'crystal_echo'),
('spaceport','Starship Harbor','بندر ستاره‌پیما',(135,211,244),(174,142,239),'spaceport_launch')]

def image_for_theme(index,item):
 name,_,_,accent,secondary,_=item
 rng=np.random.default_rng(7300+index)
 y,x=np.mgrid[0:H,0:W].astype(np.float32);u,v=x/W,y/H
 base=np.empty((H,W,3),dtype=np.float32);base[:]=(6,11,25)
 field=np.exp(-(((u-.77)/.57)**2+((v-.41)/.32)**2))
 lower=np.exp(-(((u-.23)/.6)**2+((v-.84)/.38)**2))
 clouds=.45+.21*np.sin(u*12+v*9+index)+.18*np.sin(v*19-u*5)
 for c in range(3):
  base[:,:,c]+=field*accent[c]*.17+lower*secondary[c]*.16
  base[:,:,c]+=np.maximum(clouds,0)*field*secondary[c]*.085
 # A tiny textured gradient removes visible banding, rather than adding bulk.
 base+=rng.normal(0,.43,(H,W,1))
 img=Image.fromarray(np.clip(base,0,255).astype(np.uint8)).convert('RGBA')
 sky=Image.new('RGBA',(W,H));d=ImageDraw.Draw(sky)
 for _ in range(190):
  sx,sy=int(rng.uniform(22,W-22)),int(rng.uniform(25,H*.80))
  r=int(rng.choice([1,1,1,2,2,3]));a=int(rng.uniform(40,150))
  d.ellipse((sx-r,sy-r,sx+r,sy+r),fill=(202,231,255,a))
  if r==3:
   d.line((sx-7,sy,sx+7,sy),fill=(*accent,45),width=1)
   d.line((sx,sy-7,sx,sy+7),fill=(*accent,45),width=1)
 img=Image.alpha_composite(img,sky)
 shapes=Image.new('RGBA',(W,H));d=ImageDraw.Draw(shapes)
 if name in ('solar','lunar','ocean','candy'):
  cx,cy=int(W*.80),int(H*.22)
  radius={'solar':203,'lunar':164,'ocean':230,'candy':180}[name]
  for ring in range(radius+100,radius,-4):d.ellipse((cx-ring,cy-ring,cx+ring,cy+ring),fill=(*accent,2))
  for r in range(radius,0,-2):
   f=1-r/radius;col=tuple(int(secondary[c]*(1-f)*.39+accent[c]*f*.58) for c in range(3))
   d.ellipse((cx-r,cy-r,cx+r,cy+r),fill=(*col,255))
  if name=='lunar':
   for _ in range(14):
    dx,dy=rng.uniform(-.65,.65,2)*radius;cr=rng.uniform(9,27)
    d.ellipse((cx+dx-cr,cy+dy-cr,cx+dx+cr,cy+dy+cr),fill=(17,27,50,54),outline=(173,189,238,32),width=2)
  else:
   d.arc((cx-radius*1.65,cy-radius*.38,cx+radius*1.65,cy+radius*.38),3,177,fill=(*accent,155),width=7)
   d.arc((cx-radius*1.75,cy-radius*.44,cx+radius*1.75,cy+radius*.44),3,177,fill=(*secondary,85),width=3)
 if name=='aurora':
  for j in range(9):
   pts=[(sx,H*(.16+.009*j)+90*math.sin(sx/270+j*.07)+45*math.sin(sx/97)) for sx in range(-20,W+20,8)]
   d.line(pts,fill=(*accent,14+j*3),width=7)
 if name in ('cyber','lunar'):
  for layer in range(3):
   horizon=H*(.80+layer*.055)
   for sx in range(-40,W+80,60):
    top=horizon-rng.uniform(80,230)*(1-layer*.18);col=(9+layer*2,17+layer*3,33+layer*4,255)
    d.rectangle((sx,top,sx+46,horizon+120),fill=col,outline=(*accent,65),width=2)
    for wy in np.arange(top+20,horizon,21):
     for wx in (sx+10,sx+28):
      if rng.random()>.42:d.rectangle((wx,wy,wx+7,wy+4),fill=(*accent,70))
    if layer==0:d.line((sx+23,top,sx+23,top-rng.uniform(15,50)),fill=(*secondary,85),width=2)
 if name in ('crystal','candy'):
  for side in (-1,1):
   for j in range(8):
    cy=H*(.24+j*.095);cx=W*(.03 if side==-1 else .97);length=rng.uniform(90,190)
    pts=[(cx-side*20,cy-length*.55),(cx+side*length*.65,cy-length),(cx+side*length,cy),(cx+side*length*.3,cy+length*.7),(cx-side*15,cy+length*.45)]
    d.polygon(pts,fill=(*secondary,35),outline=(*accent,130),width=3)
    d.line((pts[1],pts[3]),fill=(*accent,75),width=2);d.line((pts[1],pts[2]),fill=(221,242,255,125),width=3)
 if name=='rainbow':
  colors=[(247,142,179),(252,189,99),(237,239,134),(115,232,175),(100,195,246),(169,148,250)]
  for j,color in enumerate(colors):d.arc((-W*.9,H*.03-j*26,W*1.7,H*1.12+j*26),207,331,fill=(*color,65),width=9)
 if name=='forest':
  for layer in range(3):
   for side in (-1,1):
    for j in range(6):
     cx=(W*(.04 if side<0 else .96))+side*j*17;cy=H*(.17+j*.135)
     trunk=14+layer*7;d.polygon([(cx-trunk,cy),(cx+trunk,cy),(cx+side*32,cy+430),(cx+side*3,cy+430)],fill=(11+layer*3,34+layer*8,39+layer*5,255))
     for k in range(4):
      radius=115-k*16;d.ellipse((cx-radius,cy-k*55-radius*.5,cx+radius,cy-k*55+radius*.5),fill=(13+layer*5,52+layer*9,50+layer*4,255),outline=(*accent,42),width=2)
  for _ in range(44):
   cx,cy=rng.uniform(40,W-40),rng.uniform(H*.18,H*.86);r=rng.uniform(2,5)
   d.ellipse((cx-r*3,cy-r*3,cx+r*3,cy+r*3),fill=(*accent,16));d.ellipse((cx-r,cy-r,cx+r,cy+r),fill=(212,251,150,150))
  for j in range(8):
   cx=70+j*137;cy=H*.87+35*math.sin(j);d.ellipse((cx-26,cy-14,cx+26,cy+14),fill=(24,48,43,255),outline=(*secondary,50),width=2)
   d.ellipse((cx-19,cy-40,cx+19,cy-17),fill=(*secondary,90));d.line((cx,cy-19,cx,cy+15),fill=(44,82,67,255),width=7)
 if name=='coral':
  for j in range(8):
   sy=H*(.12+j*.08);pts=[(sx,sy+10*math.sin(sx/87+j)) for sx in range(-20,W+20,9)];d.line(pts,fill=(*accent,16),width=3)
  for side in (-1,1):
   for j in range(7):
    cx=W*(.04 if side<0 else .96)+side*rng.uniform(0,55);cy=H*(.42+j*.075);stem=rng.uniform(65,140)
    col=(*secondary,95+int(j*5));d.line((cx,cy,cx,cy-stem),fill=col,width=9)
    for sign in (-1,1):
     d.line((cx,cy-stem*.35,cx+sign*44,cy-stem*.65,cx+sign*51,cy-stem*.90),fill=col,width=7)
     d.line((cx,cy-stem*.60,cx+sign*30,cy-stem*.78),fill=(*accent,90),width=5)
  for j in range(22):
   cx,cy=rng.uniform(30,W-30),rng.uniform(H*.11,H*.79);r=rng.uniform(4,18)
   d.ellipse((cx-r,cy-r,cx+r,cy+r),outline=(*accent,52),width=2);d.arc((cx-r*.7,cy-r*.7,cx+r*.7,cy+r*.7),193,285,fill=(224,250,255,70),width=2)
  for j in range(9):
   cx=70+(j%3)*110 if j<5 else W-80-(j%3)*100;cy=H*(.25+(j//3)*.16)
   d.ellipse((cx-17,cy-7,cx+17,cy+7),fill=(*accent,75));d.polygon([(cx+16,cy),(cx+30,cy-10),(cx+30,cy+10)],fill=(*secondary,90))
 if name=='toy':
  for side in (-1,1):
   for j in range(7):
    cx=W*(.075 if side<0 else .925);cy=H*(.25+j*.095);a=35+j%3*13
    col=accent if j%2==0 else secondary
    d.rounded_rectangle((cx-a,cy-a,cx+a,cy+a),radius=8,fill=(*col,65),outline=(*col,145),width=3)
    for dx in (-.5,.5):
     for dy in (-.5,.5):d.ellipse((cx+dx*a-6,cy+dy*a-6,cx+dx*a+6,cy+dy*a+6),fill=(*col,100))
  cx,cy=W*.82,H*.17;d.ellipse((cx-110,cy-80,cx+110,cy+80),fill=(*secondary,80),outline=(*accent,90),width=3)
  d.line((cx,cy+80,cx-70,cy+260),fill=(*accent,75),width=3);d.rectangle((cx-102,cy+250,cx-42,cy+285),fill=(*accent,65),outline=(*secondary,100),width=2)
  for j in range(5):
   cx=70+j*233;cy=H*.88;d.polygon([(cx,cy-85),(cx+54,cy),(cx-54,cy)],fill=(*secondary,100),outline=(*accent,100),width=3)
   d.rectangle((cx-38,cy,cx+38,cy+65),fill=(*accent,100),outline=(*secondary,140),width=3)
 if name=='arcade':
  for side in (-1,1):
   for j in range(11):
    cy=H*(.18+j*.066);cx=W*(.04 if side<0 else .96);extent=45+(j%4)*28
    d.line((cx,cy,cx-side*extent,cy,cx-side*extent,cy+35,cx-side*(extent+22),cy+35),fill=(*accent,75),width=3)
    d.ellipse((cx-side*(extent+22)-5,cy+30,cx-side*(extent+22)+5,cy+40),fill=(*secondary,140))
  for j in range(6):
   cx=W*.74;cy=H*(.10+j*.06);length=74-j*7
   d.polygon([(cx-length,cy),(cx,cy-length*.47),(cx+length,cy),(cx,cy+length*.47)],outline=(*secondary,60+j*6),width=3)
  for j in range(4):
   cx=85+j*303;cy=H*.88;d.rounded_rectangle((cx-58,cy-105,cx+58,cy+75),radius=11,fill=(23,20,50,255),outline=(*accent,125),width=3)
   d.rectangle((cx-43,cy-83,cx+43,cy-12),fill=(8,22,41,255),outline=(*secondary,95),width=2)
   d.polygon([(cx-40,cy+17),(cx+40,cy+17),(cx+48,cy+43),(cx-48,cy+43)],fill=(*secondary,55));d.ellipse((cx+12,cy+24,cx+23,cy+35),fill=(*accent,140))
 if name=='sunset':
  cx,cy=W*.79,H*.21;r=140
  d.ellipse((cx-r,cy-r,cx+r,cy+r),fill=(*accent,115))
  for j in range(6):d.line((cx-r,cy+j*19,cx+r,cy+j*19),fill=(52,29,64,210),width=7+j)
  for side in (-1,1):
   for j in range(5):
    cy=H*(.33+j*.117);cx=W*(.04 if side<0 else .96);ww=130-j*8
    d.polygon([(cx-ww,cy),(cx+ww,cy),(cx+ww*.6,cy+21),(cx+10,cy+95),(cx-ww*.55,cy+31)],fill=(55,37,78,230),outline=(*secondary,105),width=2)
    d.ellipse((cx-ww,cy-19,cx+ww,cy+16),fill=(*accent,75))
    d.line((cx,cy-12,cx-12,cy-83),fill=(68,63,76,230),width=5);d.line((cx-12,cy-83,cx-55,cy-100,cx-72,cy-78),fill=(*accent,100),width=4)
 if name=='volcano':
  for cx,scale in [(W*.05,1.0),(W*.92,.86)]:
   cy=H*.58;ww=220*scale;top=cy-205*scale
   pts=[(cx-ww,cy+240),(cx-57*scale,top),(cx+49*scale,top),(cx+ww,cy+240)]
   d.polygon(pts,fill=(55,29,43,255),outline=(*secondary,105),width=3);d.ellipse((cx-57*scale,top-13,cx+49*scale,top+14),fill=(*accent,190))
   d.line((cx+13,top,cx-4,top+65,cx+28,top+120,cx-30,top+220),fill=(*accent,175),width=7)
   for j in range(4):d.ellipse((cx-70-j*15,top-120-j*60,cx+75+j*20,top-j*55),fill=(65,45,72,34-j*4))
  for j in range(18):
   cx,cy=rng.uniform(0,W),rng.uniform(H*.15,H*.8);d.line((cx,cy,cx-5,cy+18),fill=(*accent,60+int(rng.uniform(0,50))),width=2)
 if name=='snow':
  for layer in range(3):
   cy=H*(.64+layer*.09);points=[(-30,H)]
   for j in range(6):
    cx=j*230-55;peak=cy-rng.uniform(70,190);points.extend([(cx-100,cy+120),(cx,peak),(cx+100,cy+120)])
    d.polygon(points+[(W+30,H)],fill=(22+layer*5,43+layer*8,68+layer*12,255),outline=(*secondary,72),width=2)
    for j in range(4):
     cx=100+j*285;py=cy-110;d.polygon([(cx-45,py+70),(cx,py),(cx+45,py+70),(cx+10,py+52),(cx-10,py+62)],fill=(*accent,105))
  for j in range(16):
   cx,cy=rng.uniform(35,W-35),rng.uniform(H*.10,H*.71);r=rng.uniform(6,14)
   for a in range(3):
    angle=a*math.pi/3;dx,dy=math.cos(angle)*r,math.sin(angle)*r;d.line((cx-dx,cy-dy,cx+dx,cy+dy),fill=(*accent,72),width=2)
 if name=='spaceport':
  for side in (-1,1):
   cx=W*(.055 if side<0 else .945);cy=H*.57
   d.polygon([(cx-70,cy+290),(cx-48,cy-110),(cx+35,cy-110),(cx+78,cy+290)],fill=(25,38,61,255),outline=(*accent,115),width=3)
   d.ellipse((cx-70,cy-135,cx+62,cy-84),fill=(23,35,63,255),outline=(*secondary,155),width=3)
   d.line((cx-6,cy-110,cx-6,cy-218),fill=(*accent,130),width=4);d.ellipse((cx-15,cy-230,cx+3,cy-212),fill=(*accent,190))
   for j in range(6):d.line((cx-42,cy+j*35,cx+38,cy+j*35),fill=(*secondary,65),width=5)
  for j in range(3):
   cx=W*(.13+j*.36);cy=H*.16+j%2*90
   d.polygon([(cx-65,cy+15),(cx+65,cy+15),(cx+35,cy-12),(cx-32,cy-12)],fill=(47,63,83,230),outline=(*accent,115),width=2)
   d.ellipse((cx-26,cy-20,cx+26,cy+12),fill=(*secondary,100));d.line((cx-44,cy+19,cx-38,cy+34),fill=(*accent,100),width=4);d.line((cx+44,cy+19,cx+38,cy+34),fill=(*accent,100),width=4)
 if name not in ('cyber','lunar','forest','coral','toy','arcade','sunset','volcano','snow','spaceport'):
  for layer in range(3):
   baseline=H*(.80+layer*.067);pts=[(-20,H)]
   for sx in range(-20,W+60,60):pts.append((sx,baseline-60*math.sin(sx/115+layer*1.3)-rng.uniform(0,45)))
   pts.append((W+20,H));d.polygon(pts,fill=(8+layer*2,18+layer*3,31+layer*5,255));d.line(pts[1:-1],fill=(*accent,45+layer*15),width=2)
 horizon=H*.78
 for j in range(9):
  sy=horizon+(H-horizon)*(j/8)**1.7;d.line((0,sy,W,sy),fill=(*accent,25+j*5),width=2)
 for k in range(-5,6):d.line((W*.5+k*32,horizon,W*.5+k*270,H),fill=(*accent,30),width=2)
 d.line((W*.40,horizon,W*.055,H),fill=(*accent,105),width=4);d.line((W*.60,horizon,W*.945,H),fill=(*accent,105),width=4)
 img=Image.alpha_composite(img,shapes.filter(ImageFilter.GaussianBlur(9)));img=Image.alpha_composite(img,shapes)
 veil_array=np.empty((H,W,4),dtype=np.uint8);veil_array[:,:,:3]=(3,9,18)
 veil_array[:,:,3]=((15+25*np.sin(np.pi*v)**2)*np.exp(-((u-.5)/.34)**4)).astype(np.uint8)
 veil=Image.fromarray(veil_array)
 return Image.alpha_composite(img,veil).convert('RGB')

def generate_art():
 (OUT/'worlds').mkdir(parents=True,exist_ok=True);PREVIEW.mkdir(parents=True,exist_ok=True);thumbnails=[]
 for idx,item in enumerate(THEMES):
  image=image_for_theme(idx,item);path=OUT/'worlds'/(item[0]+'.jpg');image.save(path,'JPEG',quality=98,subsampling=0,optimize=True)
  small=image.copy();small.thumbnail((216,384));thumbnails.append(small);print('world',item[0],path.stat().st_size,flush=True)
 sheet=Image.new('RGB',(4*216,4*410),(8,13,26));d=ImageDraw.Draw(sheet)
 for i,small in enumerate(thumbnails):
  sx=(i%4)*216;sy=(i//4)*410;sheet.paste(small,(sx,sy));d.text((sx+8,sy+388),THEMES[i][1],fill=(220,237,250))
 sheet.save(PREVIEW/'worlds-contact-sheet.jpg',quality=95)

def note_frequency(midi):return 440*2**((midi-69)/12)

def add_voice(buffer,at,duration,midi,amplitude,pan,kind='pluck'):
 length=max(4,int(duration*RATE));t=np.arange(length,dtype=np.float32)/RATE;f=note_frequency(midi)
 if kind=='pad':
  env=np.sin(np.pi*np.minimum(t/duration,1))**1.4;tone=(np.sin(2*np.pi*f*t)+.20*np.sin(2*np.pi*f*2.003*t)+.10*np.sin(2*np.pi*f*.998*t))*.74
 elif kind=='bass':
  env=np.minimum(t/.008,1)*np.exp(-t*5/duration);tone=np.sin(2*np.pi*f*t)+.14*np.sin(4*np.pi*f*t)
 elif kind=='bell':
  env=np.minimum(t/.004,1)*np.exp(-t*6/duration);tone=np.sin(2*np.pi*f*t)+.22*np.sin(2*np.pi*f*2*t)+.06*np.sin(2*np.pi*f*3*t)
 else:
  env=np.minimum(t/.005,1)*np.exp(-t*5.5/duration);tone=np.sin(2*np.pi*f*t)+.28*np.sin(4*np.pi*f*t)+.08*np.sin(6*np.pi*f*t)
 sound=(tone*env*amplitude).astype(np.float32);gains=(math.sqrt((1-pan)/2),math.sqrt((1+pan)/2));start=int(at*RATE)%len(buffer)
 for channel,gain in enumerate(gains):
  count=min(length,len(buffer)-start);buffer[start:start+count,channel]+=sound[:count]*gain
  if count<length:buffer[:length-count,channel]+=sound[count:]*gain

def percussion(buffer,at,beat,strength,rng):
 length=int(.20*RATE);t=np.arange(length,dtype=np.float32)/RATE;phase=2*np.pi*(47*t+64*(1-np.exp(-35*t))/35)
 kick=np.sin(phase)*np.exp(-23*t)*np.minimum(t/.003,1)*strength;start=int(at*RATE)%len(buffer);count=min(length,len(buffer)-start)
 buffer[start:start+count,:]+=kick[:count,None]
 if count<length:buffer[:length-count,:]+=kick[count:,None]
 length=int(.055*RATE);t=np.arange(length,dtype=np.float32)/RATE;noise=rng.normal(0,1,length).astype(np.float32);noise=np.concatenate(([0],np.diff(noise))).astype(np.float32)
 shaker=noise*np.exp(-t*95)*.009;st=int((at+beat*.5)*RATE)%len(buffer);count=min(length,len(buffer)-st);buffer[st:st+count,:]+=shaker[:count,None]
 if count<length:buffer[:length-count,:]+=shaker[count:,None]

# Each track has its own key, pulse, motif and harmonic journey. Lengths are whole
# musical bars, rather than cutting arbitrary seconds from a repeated phrase.
TRACKS=[
('aurora_drift','Aurora Drift',120,48,[0,4,7,9,7,4,2,4],'bell',.82,56),
('solar_heartbeat','Solar Heartbeat',140,50,[0,7,12,9,7,4,2,7],'pluck',1.,66),
('ocean_glide','Ocean Glide',100,53,[7,4,2,0,4,7,9,7],'bell',.65,48),
('candy_bounce','Candy Bounce',120,55,[0,4,7,12,9,7,4,2],'pluck',.9,56),
('cyber_cruise','Cyber Cruise',140,45,[0,2,7,4,9,7,2,4],'pluck',1.,68),
('crystal_echo','Crystal Echo',90,48,[12,9,7,4,2,4,7,9],'bell',.55,42),
('forest_fireflies','Forest Fireflies',110,52,[0,7,4,11,9,4,7,2],'bell',.72,52),
('coral_waltz','Coral Waltz',108,57,[0,3,7,10,7,3,5,7],'bell',.7,52),
('toy_parade','Toy Parade',120,60,[0,4,7,4,2,5,9,5],'pluck',.8,56),
('arcade_flux','Arcade Flux',144,47,[0,3,7,12,10,7,5,3],'pluck',1.,68),
('sunset_soar','Sunset Soar',96,50,[7,9,12,9,7,4,2,4],'bell',.6,46),
('spaceport_launch','Spaceport Launch',132,48,[0,7,12,16,14,12,9,7],'pluck',.95,64)]
TRACK_DURATIONS={t[0]:round(t[7]*4*60/t[2]*RATE)/RATE for t in TRACKS}
PROGRESSIONS=[
 [(0,4,7),(7,11,14),(9,12,16),(5,9,12)],
 [(0,4,7),(5,9,12),(7,11,14),(0,4,9)],
 [(0,4,7),(2,5,9),(5,9,12),(7,11,14)],
 [(0,4,7),(9,12,16),(5,9,12),(7,11,14)],
 [(0,3,7),(5,8,12),(3,7,10),(7,10,14)],
 [(0,3,7),(8,12,15),(5,8,12),(7,10,14)],
 [(0,4,11),(5,9,12),(9,12,16),(7,11,14)],
 [(0,3,7),(5,8,12),(10,14,17),(7,10,14)],
 [(0,4,7),(5,9,12),(2,5,9),(7,11,14)],
 [(0,3,7),(8,12,15),(10,14,17),(7,10,14)],
 [(0,4,7),(9,12,16),(2,5,9),(5,9,12)],
 [(0,4,7),(7,11,14),(5,9,12),(9,12,16)]]

def music_bar(ti,track,bar):
 name,title,bpm,root,melody,kind,intensity,bars=track
 beat=60/bpm;start=round(bar*4*beat*RATE);end=round((bar+1)*4*beat*RATE)
 frames=end-start;tail_frames=math.ceil(beat*2*RATE)
 buffer=np.zeros((frames+tail_frames,2),dtype=np.float32)
 rng=np.random.default_rng(9800+ti*10000+bar)
 phrase=(bar//8)%6;chord=PROGRESSIONS[ti][(bar//2)%4]
 # Intro, main theme, answer, breakdown and finale make the two-minute track
 # develop. The ending returns to the opening chord for seamless looping.
 section=min(4,int(bar/bars*5));energy=(.65,.92,1,.68,1.0)[section]
 if section==3:chord=PROGRESSIONS[ti][(bar//4+2)%4]
 if bar>=bars-2:chord=PROGRESSIONS[ti][0]
 for j,degree in enumerate(chord):
  add_voice(buffer,0,4.1*beat,root+degree,.032*intensity,(-.65,0,.65)[j],'pad')
 steps=8
 for step in range(steps):
  # Answer phrases alter intervals, octave and rhythmic density.
  n=melody[(step+phrase*2)%len(melody)]
  if section==2:n=melody[7-step]+(12 if step in (3,7) else 0)
  if section==3 and step%2:continue
  if ti in (2,5,10) and bar%4==0 and step%2:continue
  offset=step*beat/2
  if ti==7:offset+=beat*.12*(step%2)  # Coral's gentle swung pulse.
  if ti==8:offset+=beat*.05*(step%2)
  if section==0 and bar<3 and step%2:continue
  amplitude=.071*energy*intensity
  add_voice(buffer,offset,beat*.92,root+24+n,amplitude,math.sin(step*1.2+ti)*.45,kind)
  add_voice(buffer,offset+beat*.74,beat*.85,root+24+n,.014*energy*intensity,-math.sin(step*1.2+ti)*.7,kind)
  if section==4 and bar%4>=2:
   add_voice(buffer,offset,beat*.66,root+36+chord[step%3],.013*intensity,(-1)**step*.65,'bell')
 for step in range(4):
  degree=chord[0] if step%2==0 else chord[2]
  add_voice(buffer,step*beat,beat*.84,root-12+degree,.104*intensity,0,'bass')
  if (section!=3 or step%2==0) and (ti!=5 or step%2==0):
   percussion(buffer,step*beat,beat,.084*energy*intensity,rng)
  if ti in (1,4,9,11) and section in (2,4):
   percussion(buffer,(step+.5)*beat,beat,.028*intensity,rng)
 if bar%4==3 and section!=0:
  for step in range(4):
   add_voice(buffer,(2+step*.5)*beat,beat,root+36+melody[(step+phrase)%8],.021*energy,(-1)**step*.7,'bell')
 return buffer,frames

def streamed_bars(ti,track):
 # The last bar's release is carried into the first, so a MediaPlayer loop has
 # continuous note tails. Memory never grows with the total track duration.
 last,frames=music_bar(ti,track,track[7]-1);carry=last[frames:].copy()
 for bar in range(track[7]):
  buffer,frames=music_bar(ti,track,bar);buffer[:len(carry)]+=carry
  yield buffer[:frames]
  carry=buffer[frames:].copy()

def generate_music(only_track=None):
 (OUT/'music').mkdir(parents=True,exist_ok=True)
 for ti,track in enumerate(TRACKS):
  name=track[0]
  if only_track and name!=only_track:continue
  peak=0.;squares=0.;count=0
  for chunk in streamed_bars(ti,track):
   peak=max(peak,float(np.max(np.abs(chunk))))
   squares+=float(np.sum(chunk.astype(np.float64)**2));count+=chunk.size
  rms=math.sqrt(squares/count);gain=min(.68/max(peak,.01),.085/max(rms,.001))
  path=OUT/'music'/(name+'.wav');encoded_peak=0.;encoded_squares=0.;frames_written=0
  with wave.open(str(path),'wb') as f:
   f.setnchannels(2);f.setsampwidth(2);f.setframerate(RATE)
   for chunk in streamed_bars(ti,track):
    normalized=np.tanh(chunk*gain)
    encoded_peak=max(encoded_peak,float(np.max(np.abs(normalized))))
    encoded_squares+=float(np.sum(normalized.astype(np.float64)**2));frames_written+=len(chunk)
    f.writeframesraw((normalized*32767).astype('<i2').tobytes())
  assert frames_written==round(TRACK_DURATIONS[name]*RATE)
  print('music',name,str(round(frames_written/RATE,3))+' seconds',path.stat().st_size,'peak',round(encoded_peak,3),'rms',round(math.sqrt(encoded_squares/(frames_written*2)),3),flush=True)
def write_catalog():
 entries=[]
 for i,item in enumerate(THEMES):entries.append({'id':item[0],'index':i,'name_en':item[1],'name_fa':item[2],'accent':'#%02x%02x%02x'%item[3],'background':'content/worlds/'+item[0]+'.jpg','music':'content/music/'+item[5]+'.wav','background_width':W,'background_height':H})
 data={'schema':2,'pack':'JET Worlds','license':'CC0-1.0','release_family':'expanded-local-multiplayer','music_arrangement':'Original whole-bar intro, theme, answer, breakdown and finale; seamless cyclic release tails; memory-bounded PCM rendering','origin':'Original algorithmic illustrations and original synthesized compositions; no third-party media.','worlds':entries,'tracks':[{'id':t[0],'title':t[1],'path':'content/music/'+t[0]+'.wav','duration_seconds':TRACK_DURATIONS.get(t[0],DURATION),'bpm':t[2],'sample_rate':RATE,'channels':2,'encoding':'PCM S16LE'} for t in TRACKS]}
 OUT.mkdir(parents=True,exist_ok=True);(OUT/'catalog.json').write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
 total=sum(p.stat().st_size for p in OUT.rglob('*') if p.is_file());print('Asset bytes',total,'decimal MB',round(total/1e6,2),'MiB',round(total/1048576,2),flush=True)

if __name__=='__main__':
 parser=argparse.ArgumentParser();parser.add_argument('--only',choices=['art','music']);parser.add_argument('--track',choices=[t[0] for t in TRACKS]);args=parser.parse_args()
 if args.only!='music':generate_art()
 if args.only!='art':generate_music(args.track)
 write_catalog()