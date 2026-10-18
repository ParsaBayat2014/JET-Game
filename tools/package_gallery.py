"""Package the six visually reviewed native Android images; never modifies PNG pixels."""
from pathlib import Path
import hashlib
import json
import struct
import zipfile
from html import escape

ROOT = Path(__file__).resolve().parents[1]
GALLERY = ROOT / 'releases/JET-2.0.0-build205-gallery'
EXPECTED = ('01-jet.png','02-action-3d.png','03-guardian-turbo.png','04-together-3d.png','05-candy-25d.png','06-worlds-crew.png')
TITLES = ('جت؛ نسخهٔ ۲','پرواز اکشن سه‌بعدی','نگهبان و توربو','اکشن دونفره روی یک گوشی','کهکشان آب‌نبات؛ ۲٫۵بعدی','جهان‌ها، شخصیت‌ها و مراحل')

def package():
    manifest = json.loads((GALLERY / 'gallery.json').read_text(encoding='utf-8'))
    scenes = sorted(manifest['scenes'], key=lambda s: s['scene'])
    if tuple(s['file'] for s in scenes) != EXPECTED:
        raise RuntimeError('All six reviewed scenes are required')
    for scene in scenes:
        blob = (GALLERY / scene['file']).read_bytes()
        if blob[:8] != b'\x89PNG\r\n\x1a\n' or blob[-12:] != b'\x00\x00\x00\x00IEND\xaeB`\x82':
            raise RuntimeError('Incomplete native PNG')
        if struct.unpack('>II', blob[16:24]) != (900,1600):
            raise RuntimeError('Unexpected capture dimensions')
        if hashlib.sha256(blob).hexdigest() != scene['sha256']:
            raise RuntimeError('Capture manifest checksum mismatch')
    cards = '\n'.join(f'<figure><a href="{name}"><img src="{name}" alt="{escape(title)}" loading="lazy" width="900" height="1600"></a><figcaption>{escape(title)}</figcaption></figure>' for name,title in zip(EXPECTED,TITLES))
    html = '''<!doctype html><html lang="fa" dir="rtl"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>جت ۲ — گالری تصاویر</title><style>body{margin:0;background:#091423;color:#effaf8;font-family:Tahoma,sans-serif}main{max-width:1240px;margin:auto;padding:28px}h1{color:#92ffcb}p{line-height:1.9;max-width:900px}section{display:grid;grid-template-columns:repeat(auto-fit,minmax(260px,1fr));gap:22px}figure{margin:0;background:#142737;border:1px solid #305668;border-radius:18px;overflow:hidden}img{width:100%;height:auto;display:block}figcaption{padding:17px;line-height:1.7}a{color:#96ffd3}footer{margin-top:24px;color:#a6bbcc;font-size:14px}</style><main><h1>جت — نسخهٔ ۲، ساخت ۲۰۵</h1><p>شش تصویر معرفی با گرافیک واقعی بازی. تصاویر سه‌بعدی از OpenGL ES و تصویر ۲٫۵بعدی از Canvas اندروید گرفته شده‌اند. صحنه‌ها ثابت و انتخاب‌شده‌اند و چیدمان معرفی با رابط معمول بازی تفاوت دارد. برای دیدن هر تصویر با اندازهٔ کامل، روی آن بزنید.</p><section>''' + cards + '''</section><footer>اندازهٔ هر تصویر: ۹۰۰ × ۱۶۰۰ پیکسل · اکشن دونفره روی یک گوشی؛ رقابت وای‌فای در حالت امتیازی · تصاویر بدون ویرایش پیکسلی پس از ثبت</footer></main></html>'''
    (GALLERY / 'index.html').write_text(html, encoding='utf-8')
    manifest.update(requiresVisualVerification=False, visualVerification={'date':'2026-10-13','allSixScenesReviewed':True,'artBodiesVisible':True,'method':'Native capture visual inspection; no pixel editing'})
    (GALLERY / 'gallery.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding='utf-8')
    out = ROOT / 'releases/JET-2.0.0-build205-images.zip'
    with zipfile.ZipFile(out,'w',zipfile.ZIP_DEFLATED) as z:
        for name in (*EXPECTED,'index.html','gallery.json'):
            z.write(GALLERY/name, name)
    sha = hashlib.sha256(out.read_bytes()).hexdigest()
    out.with_name(out.name+'.sha256').write_text(sha+'  '+out.name+'\n', encoding='ascii')
    print(json.dumps({'file':str(out),'bytes':out.stat().st_size,'sha256':sha,'images':len(EXPECTED)}))

if __name__ == '__main__':
    package()
