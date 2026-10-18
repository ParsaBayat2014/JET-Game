package com.toosarax.jetjo.core;

/** Forty-eight handcrafted mission configurations, independent of Android and purchases. */
public final class StageCatalog {
    private StageCatalog() {}
    public static final int SURVIVE = 0, ENERGY = 1, COMBO = 2, NEAR = 3, POWER = 4, OVERDRIVE = 5;
    public static final int WORLD_COUNT = 12, STAGES_PER_WORLD = 4, COUNT = WORLD_COUNT * STAGES_PER_WORLD;
    private static final String[] WORLD_EN = {"Aurora Garden", "Candy Comet", "Ocean of Stars", "Solar Forge", "Crystal Storm", "Midnight Galaxy",
        "Cloud Castle", "Robot Playground", "Firefly Forest", "Bubble Lagoon", "Sunflower Station", "Dream Observatory"};
    private static final String[] WORLD_FA = {"باغ شفق", "دنباله‌دار شیرین", "اقیانوس ستاره‌ها", "کوره خورشید", "طوفان کریستال", "کهکشان نیمه‌شب",
        "قلعه ابرها", "زمین بازی ربات‌ها", "جنگل شب‌تاب", "تالاب حباب‌ها", "ایستگاه آفتابگردان", "رصدخانه رویا"};
    private static final String[] MISSION_EN = {"First Flight", "Safe Passage", "Perfect Chain", "Light the Galaxy"};
    private static final String[] MISSION_FA = {"نخستین پرواز", "گذرگاه امن", "زنجیره بی‌نقص", "کهکشان را روشن کن"};
    private static final String[][] EXPEDITION_EN = {
        {"Cloud Shield", "Sky Crystals", "Castle Passage", "Wind Collector"},
        {"Robo Recharge", "Gear Parade", "Playground Patrol", "Turbo Workshop"},
        {"Firefly Lantern", "Forest Sparks", "Moonlit Canopy", "Glow Keeper"},
        {"Bubble Booster", "Pearl Hunt", "Lagoon Crossing", "Ocean Inventor"},
        {"Garden Power", "Solar Seeds", "Sunflower Flight", "Sunny Surprise"},
        {"Dream Spark", "Star Archive", "Final Orbit", "Cosmic Celebration"}};
    private static final String[][] EXPEDITION_FA = {
        {"سپر ابرها", "کریستال‌های آسمان", "گذر از قلعه", "گردآورنده باد"},
        {"شارژ رباتی", "رژه چرخ‌دنده‌ها", "گشت زمین بازی", "کارگاه توربو"},
        {"فانوس شب‌تاب", "جرقه‌های جنگل", "سایه‌بان مهتاب", "نگهبان نور"},
        {"قدرت حبابی", "شکار مروارید", "گذر از تالاب", "مخترع اقیانوس"},
        {"قدرت باغ", "دانه‌های خورشید", "پرواز آفتابگردان", "شگفتی آفتابی"},
        {"جرقه رویا", "گنجینه ستاره‌ها", "مدار آخر", "جشن کیهانی"}};
    public static final class Stage {
        public final int index, world, number, goal, target;
        public final String titleEn, titleFa;
        public final float duration, difficulty;
        public final long seed;
        private Stage(int i) {
            index = i; world = i / STAGES_PER_WORLD; number = i % STAGES_PER_WORLD + 1;
            if (i < 24) {
                // Save-compatible original campaign: keep its goals, seeds and timings.
                titleEn = MISSION_EN[number - 1]; titleFa = MISSION_FA[number - 1];
                duration = i == 0 ? 35 : 40 + world * 4 + (number - 1) * 3;
                difficulty = 1 + world * .065f;
                if (i == 0) { goal = SURVIVE; target = 35; }
                else if (number == 1) { goal = ENERGY; target = 16 + world * 4; }
                else if (number == 2) { goal = NEAR; target = 12 + world * 3; }
                else if (number == 3) { goal = COMBO; target = 10 + world * 2; }
                else { goal = OVERDRIVE; target = 2 + world / 2; }
            } else {
                int expedition = world - 6;
                titleEn = EXPEDITION_EN[expedition][number - 1];
                titleFa = EXPEDITION_FA[expedition][number - 1];
                duration = 60 + expedition * 2 + (number - 1) * 3;
                difficulty = Math.min(1.35f, 1.33f + expedition * .004f);
                if (number == 1) { goal = POWER; target = 1; }
                else if (number == 2) { goal = ENERGY; target = 28 + expedition * 3; }
                else if (number == 3) { goal = SURVIVE; target = (int) duration; }
                else { goal = COMBO; target = 32 + expedition * 3; }
            }
            seed = 0x4E454F4EL + i * 341873128712L;
        }
        public String title(boolean fa) { return fa ? titleFa : titleEn; }
        public String worldName(boolean fa) { return StageCatalog.worldName(world, fa); }
        public String objectiveName(boolean fa) { return StageCatalog.objectiveName(goal, fa); }
    }
    private static final Stage[] STAGES = createStages();
    private static Stage[] createStages() {
        Stage[] result = new Stage[COUNT];
        for (int i = 0; i < COUNT; i++) result[i] = new Stage(i);
        return result;
    }
    public static Stage get(int index) { return STAGES[Math.max(0, Math.min(COUNT - 1, index))]; }
    public static String worldName(int world, boolean fa) {
        int bounded = Math.max(0, Math.min(WORLD_COUNT - 1, world));
        return (fa ? WORLD_FA : WORLD_EN)[bounded];
    }
    public static String objectiveName(int goal, boolean fa) {
        switch (goal) {
            case ENERGY: return fa ? "انرژی جمع کن" : "Collect energy";
            case COMBO: return fa ? "کمبو بساز" : "Build a combo";
            case NEAR: return fa ? "از موانع نزدیک عبور کن" : "Dodge close obstacles";
            case POWER: return fa ? "قدرت جمع کن" : "Collect power-ups";
            case OVERDRIVE: return fa ? "فراسرعت فعال کن" : "Activate Overdrive";
            default: return fa ? "تا پایان دوام بیاور" : "Reach the finish";
        }
    }
}
