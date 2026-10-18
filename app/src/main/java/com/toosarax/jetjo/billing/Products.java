package com.toosarax.jetjo.billing;

import java.util.Arrays;
import java.util.List;

/** Permanent cosmetic packs. Never consumed and never affect simulation or rewards. */
public final class Products {
    private Products() {}
    public static final String AURORA="neon_pack_aurora", SOLAR="neon_pack_solar", SUPPORTER="neon_supporter";
    public static final List<String> IDS=Arrays.asList(AURORA,SOLAR,SUPPORTER);
    public static final String[][] NAMES={{"Aurora collection","مجموعه شفق"},{"Solar collection","مجموعه خورشیدی"},{"Founder's collection","مجموعه حامی"}};
    public static final String[][] DESCRIPTIONS={
        {"One-time purchase permanently unlocks Aurora, Borealis and Nova. Cosmetic only; no extra power or score.","خرید یک‌باره برای فعال‌کردن دائمی سه سفینهٔ شفق، قطبی و نوا؛ فقط تغییر ظاهر، بدون افزایش قدرت یا امتیاز."},
        {"One-time purchase permanently unlocks Helios, Phoenix and Flare. Cosmetic only; no extra power or score.","خرید یک‌باره برای فعال‌کردن دائمی سه سفینهٔ هلیوس، ققنوس و شراره؛ فقط تغییر ظاهر، بدون افزایش قدرت یا امتیاز."},
        {"One-time purchase permanently unlocks Sovereign, Origin and the Founding Pilot menu badge. Cosmetic only; no extra power or score.","خرید یک‌باره برای فعال‌کردن دائمی دو سفینهٔ فرمانروا و سرآغاز و نشان خلبان حامی در منو؛ فقط تغییر ظاهر، بدون افزایش قدرت یا امتیاز."}};
    public static int packForSkin(int skin){return skin<10||skin>=18?-1:skin>=16?2:skin>=13?1:0;}
}

