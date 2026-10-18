package com.toosarax.jetjo.data;

/** Parallel language entries keep content extensible without coupling it to views. */
public final class Catalog {
    private Catalog() {}
    public static final int[] TRAIL_LEVELS={1,2,3,5,7,10,12,15,18,22,27,32};
    public static final int[] BADGE_STARS={0,3,12,36,60,84,108,144};
    public static final int[] SCENE_LEVELS={1,3,4,1,5,6,8,10,12,14,16,18,20,23,26,30};
    public static final String[][] SKINS={
        {"Ion","یون"},{"Glacier","یخچال"},{"Solar","خورشید"},{"Orchid","ارکیده"},{"Ember","اخگر"},
        {"Spectre","شبح"},{"Mint","نعناع"},{"Quasar","اختروش"},{"Prism","منشور"},{"Eclipse","کسوف"},{"Aurora","شفق"},{"Borealis","قطبی"},{"Nova","نوا"},{"Helios","هلیوس"},{"Phoenix","ققنوس"},{"Flare","شراره"},{"Sovereign","فرمانروا"},{"Origin","سرآغاز"},
        {"Lumo bunny","لومو خرگوش"},{"Fino fox","فینو روباه"},{"Pingo penguin","پینگو پنگوئن"},{"Tuga turtle","توگا لاک‌پشت"},
        {"Pando panda","پاندو پاندا"},{"Hoot owl","هوت جغد"},{"Ribo frog","ریبو قورباغه"},{"Wavo whale","واوو نهنگ"},
        {"Buzzy bee","بوزی زنبور"},{"Jelli jellyfish","ژلی عروس دریایی"},{"Axo explorer","آکسو کاوشگر"},{"Drako dragon","دراکو اژدها"},
        {"Mira astronaut","میرا فضانورد"},{"Rolo rover","رولو مریخ‌نورد"},{"Spot puppy","اسپات توله‌سگ"},{"Bobo robot","بوبو ربات"},
        {"Bloom flower","بلوم گل"},{"Orbit raccoon","اوربیت راکون"}};
    public static final int[] COLORS={0xff8cffb9,0xff6ae4ff,0xffffd779,0xffd2a0ff,0xffff926f,0xffecefff,0xff56e9d4,0xffff80b5,0xffadb0ff,0xffffcc6a,0xff71ffe2,0xffac8eff,0xffc4efff,0xffffb06b,0xffff648e,0xffffee88,0xffffd56a,0xfff4f7ff,
        0xffffb4d8,0xffffa35c,0xff85d6ff,0xff95e99b,0xfff0f1ff,0xffccaa83,0xff89f3bb,0xff86abff,
        0xffffdf66,0xffd9a0ff,0xffff8bc8,0xffb4ef76,0xffdde8ff,0xffffbd8a,0xffdec292,0xff8be9e0,
        0xffffb28b,0xffbcbbdf};
    public static final String[][] ZONES={{"Midnight circuit","مدار نیمه‌شب"},{"Violet frontier","مرز بنفش"},{"Solar drift","گذر خورشیدی"}};
    public static final String[][] POWERS={{"Shield","سپر"},{"Magnet","آهن‌ربا"},{"Slow motion","حرکت آهسته"},{"Double score","امتیاز دوبرابر"}};
    public static final String[] POWER_ICONS={"◇","∩","◷","×2"};
    public static final String[][] ACHIEVEMENTS={
        {"First contact","اولین تماس"},{"In the flow","در جریان"},{"Night rider","شب‌رو"},{"Centurion","صدتایی"},
        {"Spark collector","گردآورنده جرقه"},{"Energy bank","بانک انرژی"},{"Unbroken","بی‌وقفه"},{"Chain reaction","واکنش زنجیره‌ای"},
        {"Overdrive","فراسرعت"},{"Survivor","بازمانده"},{"Untouchable","دست‌نیافتنی"},{"Power player","بازیکن قدرتمند"},
        {"New horizons","افق‌های نو"},{"Full spectrum","طیف کامل"},{"Rising star","ستاره نوظهور"},{"Beyond the neon","آن‌سوی نئون"},{"First destination","اولین مقصد"},{"World explorer","جهانگرد"},{"Campaign captain","فرمانده سفر"},{"Star chart","نقشه ستاره‌ها"},{"Close call ace","استاد عبور نزدیک"},{"Overdrive master","استاد فراسرعت"},{"Energy sprinter","دونده انرژی"},{"Space traveller","مسافر فضا"},
        {"New friends","دوستان تازه"},{"Animal crew","گروه حیوانات"},{"Grand collection","مجموعه بزرگ"},{"Second horizon","افق دوم"},
        {"Cosmic navigator","راهنمای کیهانی"},{"Perfect universe","جهان بی‌نقص"},{"Veteran pilot","خلبان باتجربه"},{"Power specialist","استاد قدرت"}};
    public static final String[][] ACH_DESC={
        {"Finish your first run","اولین دور را تمام کن"},{"Play 10 runs","۱۰ دور بازی کن"},{"Play 50 runs","۵۰ دور بازی کن"},{"Pass 100 obstacles","از ۱۰۰ مانع عبور کن"},
        {"Collect 100 energy","۱۰۰ انرژی جمع کن"},{"Collect 1,000 energy","۱۰۰۰ انرژی جمع کن"},{"Reach a 10 combo","به کمبوی ۱۰ برس"},{"Reach a 25 combo","به کمبوی ۲۵ برس"},
        {"Score 5,000 in a run","در یک دور ۵۰۰۰ امتیاز بگیر"},{"Survive 60 seconds","۶۰ ثانیه دوام بیاور"},{"Survive 120 seconds","۱۲۰ ثانیه دوام بیاور"},{"Collect 20 power-ups","۲۰ قدرت جمع کن"},
        {"Own 4 ships","۴ سفینه داشته باش"},{"Own all 10 original earned ships","هر ۱۰ سفینه رایگان اولیه را داشته باش"},{"Reach pilot level 5","به سطح ۵ برس"},{"Reach pilot level 10","به سطح ۱۰ برس"},
        {"Clear your first stage","اولین مرحله را کامل کن"},{"Clear 8 stages","۸ مرحله را کامل کن"},{"Clear the first 24 stages","۲۴ مرحله اول را کامل کن"},{"Earn 36 campaign stars","۳۶ ستاره در سفر بگیر"},
        {"Make 50 close calls","۵۰ عبور نزدیک انجام بده"},{"Trigger 20 Overdrives","۲۰ فراسرعت فعال کن"},{"Reach 3,000 in Energy Rush","در هجوم انرژی به ۳۰۰۰ برس"},{"Fly for 30 minutes in total","در مجموع ۳۰ دقیقه پرواز کن"},
        {"Own your first new character","اولین شخصیت تازه را آزاد کن"},{"Own 8 new characters","۸ شخصیت تازه را آزاد کن"},
        {"Own all 28 earned characters","هر ۲۸ شخصیت رایگان را آزاد کن"},{"Clear 32 stages","۳۲ مرحله را کامل کن"},
        {"Clear all 48 stages","هر ۴۸ مرحله را کامل کن"},{"Earn all 144 campaign stars","هر ۱۴۴ ستاره سفر را بگیر"},
        {"Reach pilot level 25","به سطح ۲۵ برس"},{"Collect 100 power-ups","۱۰۰ قدرت جمع کن"}};
    public static final String[][] MISSIONS={{"Set a run score","امتیاز یک دور"},{"Stay in flight (seconds)","زمان پرواز (ثانیه)"},{"Collect energy","جمع‌آوری انرژی"},{"Collect power-ups","جمع‌آوری قدرت"},{"Dodge obstacles","عبور از موانع"}};
    public static final int[] MISSION_BASE={1200,75,45,3,30};
}
