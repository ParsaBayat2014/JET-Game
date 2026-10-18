"""Original synthesized JET sounds. Python standard library; CC0."""
import math, wave, struct
from pathlib import Path
OUT = Path(__file__).resolve().parents[1] / 'app/src/main/res/raw'
OUT.mkdir(parents=True, exist_ok=True)
RATE = 22050
def write(name, duration, fn):
    with wave.open(str(OUT / (name + '.wav')), 'wb') as f:
        f.setnchannels(1); f.setsampwidth(2); f.setframerate(RATE)
        f.writeframes(b''.join(struct.pack('<h', int(max(-.9, min(.9, fn(i/RATE))) * 32767)) for i in range(int(duration*RATE))))
def tone(freq, t): return math.sin(2*math.pi*freq*t) + .18*math.sin(4*math.pi*freq*t)
for name, freq, length in [('click',620,.07),('collect',960,.13),('combo',1320,.25),('shield',400,.35),('magnet',520,.35),('slow',260,.45),('double_score',780,.4),('level',660,.65),('achievement',880,.75)]:
    write(name,length,lambda t,f=freq,d=length:.22*tone(f*(1+t*.7),t)*min(1,t/.006)*max(0,1-t/d)**2)
write('collision',.3,lambda t:.35*math.sin(2*math.pi*(110-90*t)*t)*math.exp(-14*t))
write('game_over',.7,lambda t:.28*tone(280*math.exp(-1.7*t),t)*max(0,1-t/.7)**2)
notes=[110,164.81,220,164.81,130.81,196,261.63,196,98,146.83,196,146.83,123.47,185,246.94,185]
def music(t):
    beat=t%.4; idx=int(t/.4)%16
    pulse=.09*tone(notes[idx],t)*math.exp(-beat*9)
    pad=.025*tone(notes[(idx//4)*4]*2,t)
    kick=.10*math.sin(2*math.pi*(52+65*math.exp(-beat*30))*beat)*math.exp(-beat*20)
    return (pulse+pad+kick)*min(1,t/.04)*min(1,(6.4-t)/.04)
write('music',6.4,music)
print('Generated 12 original PCM audio assets')
