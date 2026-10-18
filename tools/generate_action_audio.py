"""Original short Action Flight effects, synthesized locally; CC0, no recorded samples."""
from pathlib import Path
import math,random,struct,wave
ROOT=Path(__file__).resolve().parents[1]/'app/src/main/res/raw'
RATE=22050

def effect(name,duration,render):
    rng=random.Random(412+sum(map(ord,name)))
    frames=[]
    for i in range(round(duration*RATE)):
        t=i/RATE;u=t/duration
        fade=min(1,t/.006,(duration-t)/.018)
        value=max(-.85,min(.85,render(t,u,rng)))*max(0,fade)
        frames.append(struct.pack('<h',round(value*32767)))
    with wave.open(str(ROOT/(name+'.wav')),'wb') as audio:
        audio.setnchannels(1);audio.setsampwidth(2);audio.setframerate(RATE);audio.writeframes(b''.join(frames))

def tone(freq,t):return math.sin(2*math.pi*freq*t)

def jump(t,u,rng):return (.24*tone(280+1100*u,t)+.10*tone(560+1800*u,t))*(1-u)**.65

def duck(t,u,rng):return (.22*tone(620-470*u,t)+.10*(rng.random()*2-1))*(1-u)**.7

def turbo(t,u,rng):return (.15*tone(95+120*u,t)+.15*tone(330+1000*u,t)+.06*(rng.random()*2-1))*(math.sin(math.pi*u)**.6)

def guardian(t,u,rng):
    pulse=(t% .19)/.19
    return (.22*tone(220 if int(t/.19)%2==0 else 330,t)+.08*tone(660,t))*(1-pulse)*(.4+.6*(1-u))

def victory(t,u,rng):
    value=0
    for delay,freq in ((0,523.25),(.09,659.25),(.18,783.99),(.28,1046.5)):
        a=t-delay
        if a>=0:value+=.13*tone(freq,a)*math.exp(-a*5)
    value+=.09*(rng.random()*2-1)*math.exp(-t*18)
    return value

def stunt(t,u,rng):
    return (.19*tone(880,t)+.12*tone(1320,t)+.05*tone(1760,t))*math.exp(-t*11)

if __name__=='__main__':
    ROOT.mkdir(parents=True,exist_ok=True)
    for name,duration,render in (('jump',.24,jump),('duck',.20,duck),('turbo',.70,turbo),('guardian',.60,guardian),('guardian_break',.85,victory),('stunt',.24,stunt)):
        effect(name,duration,render)
    print('PASS: six original short action effects')
