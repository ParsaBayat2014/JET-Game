package com.toosarax.jetjo.game;

import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import com.toosarax.jetjo.data.Catalog;

/** Original, resolution-independent ships shared by the runway and hangar. */
public final class Art {
    private Art() {}
    private static final class Geometry {
        final Matrix scale = new Matrix();
        final Path pixels = new Path();
    }
    private static final ThreadLocal<Geometry> GEOMETRY = new ThreadLocal<Geometry>() {
        @Override protected Geometry initialValue() { return new Geometry(); }
    };

    /** Some hardware Canvas drivers discard tiny local paths before the canvas scale is applied.
     * Expand geometry to pixels while keeping the same final transform and visible stroke width.
     * Geometry is reused per drawing thread, including bitmap preview rendering. */
    private static void drawPath(Canvas canvas, Paint paint, Path path, float size) {
        Geometry geometry = GEOMETRY.get();
        geometry.scale.setScale(size, size); path.transform(geometry.scale, geometry.pixels);
        float stroke = paint.getStrokeWidth();
        canvas.save();
        try {
            canvas.scale(1 / size, 1 / size); paint.setStrokeWidth(stroke * size);
            canvas.drawPath(geometry.pixels, paint);
        } finally { paint.setStrokeWidth(stroke); canvas.restore(); }
    }

    /** Reuses the caller's paint and path; drawing never allocates geometry objects. */
    public static void ship(Canvas c, Paint p, Path path, float x, float y,
                            float size, int skin, float time, boolean motion) {
        if (!Float.isFinite(size) || size <= 0 || Catalog.COLORS.length == 0) return;
        int id = Math.max(0, Math.min(Catalog.COLORS.length - 1, skin));
        int color = Catalog.COLORS[id], shape = id;
        float pulse = motion ? (float) Math.sin(time * 8 + id) * .12f : 0;
        c.save(); c.translate(x, y); c.scale(size, size);
        p.setShader(null); p.setStyle(Paint.Style.FILL);
        for (int i = 3; i > 0; i--) {
            p.setColor((color & 0xffffff) | ((9 - i) << 24));
            c.drawCircle(0, .06f, 1.05f + i * .19f, p);
        }
        // Engines remain decorative; cosmetic selection cannot affect collisions.
        flame(c, p, path, color, -.22f, .68f, .80f + pulse, size);
        flame(c, p, path, color, .22f, .68f, .80f - pulse * .6f, size);
        if (shape == 4 || shape == 15) {
            flame(c, p, path, color, -.70f, .42f, .58f + pulse, size);
            flame(c, p, path, color, .70f, .42f, .58f - pulse, size);
        }
        path.reset();
        switch (shape) {
            case 0: // Ion: sharp trainer.
                path.moveTo(0, -1.03f); path.lineTo(.81f, .61f);
                path.lineTo(.28f, .40f); path.lineTo(0, .73f);
                path.lineTo(-.28f, .40f); path.lineTo(-.81f, .61f); break;
            case 1: // Glacier: crystal glider.
                path.moveTo(0, -1.06f); path.lineTo(.64f, -.32f);
                path.lineTo(.86f, .43f); path.lineTo(.38f, .70f);
                path.lineTo(-.38f, .70f); path.lineTo(-.86f, .43f);
                path.lineTo(-.64f, -.32f); break;
            case 2: // Solar: round exploration pod.
                path.moveTo(0, -.82f);
                path.cubicTo(.52f, -.85f, .91f, -.42f, .93f, .08f);
                path.cubicTo(1.01f, .65f, .51f, .70f, 0, .66f);
                path.cubicTo(-.51f, .70f, -1.01f, .65f, -.93f, .08f);
                path.cubicTo(-.91f, -.42f, -.52f, -.85f, 0, -.82f); break;
            case 3: // Orchid: organic butterfly wings.
                path.moveTo(0, -.74f); path.quadTo(.20f, -.95f, .26f, -.54f);
                path.cubicTo(1.18f, -1.02f, 1.12f, .47f, .32f, .53f);
                path.lineTo(0, .78f); path.lineTo(-.32f, .53f);
                path.cubicTo(-1.12f, .47f, -1.18f, -1.02f, -.26f, -.54f);
                path.quadTo(-.20f, -.95f, 0, -.74f); break;
            case 4: // Ember: rounded three-engine rocket.
                path.moveTo(0, -1.10f); path.quadTo(.42f, -.82f, .40f, .04f);
                path.lineTo(.80f, .48f); path.lineTo(.80f, .70f);
                path.lineTo(.28f, .52f); path.quadTo(0, .88f, -.28f, .52f);
                path.lineTo(-.80f, .70f); path.lineTo(-.80f, .48f);
                path.lineTo(-.40f, .04f); path.quadTo(-.42f, -.82f, 0, -1.10f); break;
            case 5: // Spectre: broad manta glider.
                path.moveTo(0, -.83f); path.quadTo(.57f, -.25f, 1.09f, -.02f);
                path.quadTo(.94f, .68f, .37f, .36f); path.lineTo(0, .81f);
                path.lineTo(-.37f, .36f); path.quadTo(-.94f, .68f, -1.09f, -.02f);
                path.quadTo(-.57f, -.25f, 0, -.83f); break;
            case 6: // Mint: little cat-shaped scout.
                path.moveTo(-.64f, -.21f); path.lineTo(-.66f, -.89f);
                path.lineTo(-.26f, -.56f); path.quadTo(0, -.70f, .26f, -.56f);
                path.lineTo(.66f, -.89f); path.lineTo(.64f, -.21f);
                path.quadTo(1.03f, .62f, .27f, .69f);
                path.lineTo(0, .83f); path.lineTo(-.27f, .69f);
                path.quadTo(-1.03f, .62f, -.64f, -.21f); break;
            case 7: // Quasar: asymmetric comet.
                path.moveTo(.06f, -1.03f); path.lineTo(.59f, -.23f);
                path.lineTo(.91f, .63f); path.lineTo(.23f, .40f);
                path.lineTo(-.32f, .81f); path.lineTo(-.30f, .22f);
                path.lineTo(-.83f, .44f); path.lineTo(-.45f, -.46f); break;
            case 8: // Prism: stepped diamond.
                path.moveTo(0, -1.04f); path.lineTo(.43f, -.38f);
                path.lineTo(.90f, -.18f); path.lineTo(.57f, .18f);
                path.lineTo(.77f, .63f); path.lineTo(.20f, .46f);
                path.lineTo(0, .89f); path.lineTo(-.20f, .46f);
                path.lineTo(-.77f, .63f); path.lineTo(-.57f, .18f);
                path.lineTo(-.90f, -.18f); path.lineTo(-.43f, -.38f); break;
            case 9: // Eclipse: soft batwing.
                path.moveTo(0, -.75f); path.lineTo(.32f, -.33f);
                path.quadTo(.87f, -.65f, 1.07f, .25f);
                path.quadTo(.68f, .13f, .53f, .67f);
                path.quadTo(.21f, .37f, 0, .84f);
                path.quadTo(-.21f, .37f, -.53f, .67f);
                path.quadTo(-.68f, .13f, -1.07f, .25f);
                path.quadTo(-.87f, -.65f, -.32f, -.33f); break;
            case 10: // Aurora: sweeping paper glider.
                path.moveTo(0, -.96f); path.lineTo(1.03f, .61f);
                path.lineTo(.45f, .45f); path.lineTo(.15f, .75f);
                path.lineTo(0, .30f); path.lineTo(-.15f, .75f);
                path.lineTo(-.45f, .45f); path.lineTo(-1.03f, .61f); break;
            case 11: // Borealis: armored hexagonal pod.
                path.moveTo(0, -.98f); path.lineTo(.69f, -.56f);
                path.lineTo(.80f, .43f); path.lineTo(.41f, .77f);
                path.lineTo(-.41f, .77f); path.lineTo(-.80f, .43f);
                path.lineTo(-.69f, -.56f); break;
            case 12: // Nova: five-point star cruiser.
                star(path, 0, -.02f, 1.02f, .51f, 5, -.5f * (float) Math.PI); break;
            case 13: // Helios: curved sunwing.
                path.moveTo(0, -.86f); path.quadTo(.42f, -.76f, .35f, -.05f);
                path.lineTo(.94f, -.38f); path.lineTo(.78f, .54f);
                path.lineTo(.30f, .43f); path.lineTo(0, .82f);
                path.lineTo(-.30f, .43f); path.lineTo(-.78f, .54f);
                path.lineTo(-.94f, -.38f); path.lineTo(-.35f, -.05f);
                path.quadTo(-.42f, -.76f, 0, -.86f); break;
            case 14: // Phoenix: playful bird with feather tips.
                path.moveTo(0, -1.05f); path.lineTo(.23f, -.40f);
                path.lineTo(.63f, -.61f); path.lineTo(.48f, -.11f);
                path.lineTo(1.05f, -.21f); path.lineTo(.77f, .34f);
                path.lineTo(.95f, .60f); path.lineTo(.24f, .48f);
                path.lineTo(0, .86f); path.lineTo(-.24f, .48f);
                path.lineTo(-.95f, .60f); path.lineTo(-.77f, .34f);
                path.lineTo(-1.05f, -.21f); path.lineTo(-.48f, -.11f);
                path.lineTo(-.63f, -.61f); path.lineTo(-.23f, -.40f); break;
            case 15: // Flare: twin pontoons and inset cockpit.
                path.moveTo(0, -.86f); path.lineTo(.26f, -.24f);
                path.lineTo(.43f, -.84f); path.lineTo(.80f, -.63f);
                path.lineTo(.84f, .68f); path.lineTo(.45f, .68f);
                path.lineTo(.31f, .35f); path.lineTo(0, .70f);
                path.lineTo(-.31f, .35f); path.lineTo(-.45f, .68f);
                path.lineTo(-.84f, .68f); path.lineTo(-.80f, -.63f);
                path.lineTo(-.43f, -.84f); path.lineTo(-.26f, -.24f); break;
            case 16: // Sovereign: crown-shaped flagship.
                path.moveTo(0, -1.02f); path.lineTo(.34f, -.30f);
                path.lineTo(.80f, -.73f); path.lineTo(.79f, .39f);
                path.lineTo(.49f, .76f); path.lineTo(0, .53f);
                path.lineTo(-.49f, .76f); path.lineTo(-.79f, .39f);
                path.lineTo(-.80f, -.73f); path.lineTo(-.34f, -.30f); break;
            case 17: // Origin: happy round robot companion.
                path.moveTo(0, -.85f);
                path.cubicTo(.74f, -.87f, .93f, -.22f, .76f, .36f);
                path.lineTo(1.03f, .59f); path.lineTo(.35f, .65f);
                path.quadTo(0, .85f, -.35f, .65f); path.lineTo(-1.03f, .59f);
                path.lineTo(-.76f, .36f);
                path.cubicTo(-.93f, -.22f, -.74f, -.87f, 0, -.85f); break;
            case 18: // Lumo: long rabbit ears and a soft little landing pod.
                path.moveTo(-.55f, -.31f); path.quadTo(-.76f, -1.35f, -.36f, -1.20f);
                path.lineTo(-.12f, -.64f); path.lineTo(.12f, -.64f);
                path.quadTo(.30f, -1.48f, .53f, -1.19f); path.lineTo(.53f, -.32f);
                path.quadTo(.96f, .09f, .65f, .55f); path.lineTo(.41f, .74f);
                path.lineTo(-.41f, .74f); path.quadTo(-.98f, .43f, -.55f, -.31f); break;
            case 19: // Fino: angular fox ears and a tapered diamond cheek.
                path.moveTo(-.84f, -.88f); path.lineTo(-.31f, -.57f); path.lineTo(0, -.75f);
                path.lineTo(.31f, -.57f); path.lineTo(.84f, -.88f); path.lineTo(.73f, -.02f);
                path.lineTo(1.06f, .28f); path.lineTo(.49f, .57f); path.lineTo(0, .89f);
                path.lineTo(-.49f, .57f); path.lineTo(-1.06f, .28f); path.lineTo(-.73f, -.02f); break;
            case 20: // Pingo: oval penguin, sweeping flippers and orange landing feet.
                path.moveTo(0, -.94f); path.cubicTo(.55f, -.96f, .62f, -.45f, .64f, -.05f);
                path.lineTo(1.02f, .48f); path.lineTo(.61f, .37f); path.lineTo(.65f, .81f);
                path.lineTo(.13f, .69f); path.lineTo(-.13f, .69f); path.lineTo(-.65f, .81f);
                path.lineTo(-.61f, .37f); path.lineTo(-1.02f, .48f); path.lineTo(-.64f, -.05f);
                path.cubicTo(-.62f, -.45f, -.55f, -.96f, 0, -.94f); break;
            case 21: // Tuga: circular shell with four small fins and a raised head.
                path.moveTo(-.23f, -.55f); path.quadTo(-.34f, -1.07f, 0, -1.06f);
                path.quadTo(.34f, -1.07f, .23f, -.55f); path.lineTo(.58f, -.36f);
                path.quadTo(1.22f, -.65f, .92f, -.03f); path.lineTo(.75f, .23f);
                path.quadTo(1.10f, .77f, .53f, .64f); path.lineTo(.26f, .69f);
                path.lineTo(0, .87f); path.lineTo(-.26f, .69f); path.lineTo(-.53f, .64f);
                path.quadTo(-1.10f, .77f, -.75f, .23f); path.lineTo(-.92f, -.03f);
                path.quadTo(-1.22f, -.65f, -.58f, -.36f); break;
            case 22: // Pando: circular bear ears, broad cheeks and two rounded feet.
                path.moveTo(-.69f, -.37f); path.cubicTo(-1.10f, -.62f, -.94f, -1.06f, -.48f, -.77f);
                path.quadTo(0, -.96f, .48f, -.77f); path.cubicTo(.94f, -1.06f, 1.10f, -.62f, .69f, -.37f);
                path.quadTo(1.02f, .26f, .63f, .61f); path.quadTo(.58f, 1.0f, .14f, .71f);
                path.lineTo(-.14f, .71f); path.quadTo(-.58f, 1.0f, -.63f, .61f);
                path.quadTo(-1.02f, .26f, -.69f, -.37f); break;
            case 23: // Hoot: tufted owl, scalloped wings and a three-feather tail.
                path.moveTo(-.73f, -.90f); path.lineTo(-.15f, -.58f); path.lineTo(.15f, -.58f);
                path.lineTo(.73f, -.90f); path.lineTo(.64f, -.10f);
                path.quadTo(1.15f, .25f, .73f, .57f); path.lineTo(.36f, .48f);
                path.lineTo(.29f, .91f); path.lineTo(0, .72f); path.lineTo(-.29f, .91f);
                path.lineTo(-.36f, .48f); path.lineTo(-.73f, .57f);
                path.quadTo(-1.15f, .25f, -.64f, -.10f); break;
            case 24: // Ribo: elevated round eyes and two wide frog landing feet.
                path.moveTo(-.83f, -.30f); path.quadTo(-1.01f, -.94f, -.45f, -.92f);
                path.quadTo(-.20f, -.94f, -.16f, -.56f); path.lineTo(.16f, -.56f);
                path.quadTo(.20f, -.94f, .45f, -.92f); path.quadTo(1.01f, -.94f, .83f, -.30f);
                path.quadTo(.95f, .19f, .54f, .42f); path.lineTo(.96f, .69f);
                path.lineTo(.27f, .79f); path.lineTo(0, .56f); path.lineTo(-.27f, .79f);
                path.lineTo(-.96f, .69f); path.lineTo(-.54f, .42f);
                path.quadTo(-.95f, .19f, -.83f, -.30f); break;
            case 25: // Wavo: a playful whale with a curled tail and pectoral fins.
                path.moveTo(-.82f, -.55f); path.cubicTo(-.38f, -1.12f, .75f, -.94f, .86f, -.16f);
                path.lineTo(1.18f, -.49f); path.lineTo(1.06f, .20f); path.lineTo(.82f, .34f);
                path.lineTo(.97f, .66f); path.lineTo(.42f, .55f);
                path.quadTo(.08f, .85f, -.25f, .51f); path.lineTo(-.75f, .71f);
                path.lineTo(-.60f, .34f); path.quadTo(-1.05f, .04f, -.82f, -.55f); break;
            case 26: // Buzzy: separate upper wing lobes surrounding a striped bee body.
                path.moveTo(-.21f, -.63f); path.cubicTo(-1.16f, -1.03f, -1.26f, -.07f, -.64f, .09f);
                path.quadTo(-.80f, .39f, -.42f, .69f); path.lineTo(0, .98f);
                path.lineTo(.42f, .69f); path.quadTo(.80f, .39f, .64f, .09f);
                path.cubicTo(1.26f, -.07f, 1.16f, -1.03f, .21f, -.63f);
                path.quadTo(0, -1.06f, -.21f, -.63f); break;
            case 27: // Jelli: umbrella cap with four rounded trailing tentacles.
                path.moveTo(-.94f, .0f); path.cubicTo(-1.02f, -1.13f, 1.02f, -1.13f, .94f, .0f);
                path.lineTo(.60f, .08f); path.quadTo(.92f, .93f, .52f, .89f);
                path.lineTo(.31f, .19f); path.quadTo(.42f, 1.17f, .12f, .93f);
                path.lineTo(0, .29f); path.lineTo(-.12f, .93f);
                path.quadTo(-.42f, 1.17f, -.31f, .19f); path.lineTo(-.52f, .89f);
                path.quadTo(-.92f, .93f, -.60f, .08f); break;
            case 28: // Axo: cheerful explorer with three gill branches on each cheek.
                path.moveTo(0, -.75f); path.quadTo(.51f, -.81f, .55f, -.38f);
                path.lineTo(1.00f, -.90f); path.lineTo(.93f, -.22f); path.lineTo(1.18f, -.23f);
                path.lineTo(.89f, .18f); path.lineTo(1.11f, .46f); path.lineTo(.63f, .46f);
                path.quadTo(.60f, .87f, 0, .78f); path.quadTo(-.60f, .87f, -.63f, .46f);
                path.lineTo(-1.11f, .46f); path.lineTo(-.89f, .18f); path.lineTo(-1.18f, -.23f);
                path.lineTo(-.93f, -.22f); path.lineTo(-1.00f, -.90f); path.lineTo(-.55f, -.38f);
                path.quadTo(-.51f, -.81f, 0, -.75f); break;
            case 29: // Drako: rounded dragon head, small horns and scalloped flight wings.
                path.moveTo(-.33f, -.64f); path.lineTo(-.42f, -1.11f); path.lineTo(-.02f, -.81f);
                path.lineTo(.35f, -1.01f); path.lineTo(.40f, -.55f); path.lineTo(.73f, -.27f);
                path.lineTo(1.12f, -.47f); path.lineTo(.92f, .23f); path.lineTo(.73f, .05f);
                path.lineTo(.56f, .55f); path.lineTo(.30f, .36f); path.lineTo(.22f, .86f);
                path.lineTo(0, .66f); path.lineTo(-.22f, .86f); path.lineTo(-.30f, .36f);
                path.lineTo(-.56f, .55f); path.lineTo(-.73f, .05f); path.lineTo(-.92f, .23f);
                path.lineTo(-1.12f, -.47f); path.lineTo(-.73f, -.27f); path.lineTo(-.40f, -.55f); break;
            case 30: // Mira: astronaut helmet, side backpack and boots.
                path.moveTo(-.64f, -.58f); path.quadTo(-.52f, -1.02f, 0, -.99f);
                path.quadTo(.60f, -1.02f, .69f, -.50f); path.lineTo(.88f, -.42f);
                path.lineTo(.89f, .29f); path.lineTo(.57f, .39f); path.lineTo(.51f, .88f);
                path.lineTo(.13f, .88f); path.lineTo(0, .57f); path.lineTo(-.13f, .88f);
                path.lineTo(-.51f, .88f); path.lineTo(-.57f, .39f); path.lineTo(-.89f, .29f);
                path.lineTo(-.88f, -.42f); path.lineTo(-.69f, -.50f); break;
            case 31: // Rolo: six-wheel rover with a raised panoramic camera.
                path.moveTo(-.46f, -.64f); path.lineTo(-.46f, -1.03f); path.lineTo(.46f, -1.03f);
                path.lineTo(.46f, -.64f); path.lineTo(.83f, -.48f); path.lineTo(.83f, .05f);
                path.quadTo(1.18f, -.10f, 1.11f, .24f); path.quadTo(1.12f, .43f, .81f, .39f);
                path.quadTo(1.09f, .73f, .71f, .76f); path.lineTo(.34f, .65f);
                path.quadTo(0, 1.10f, -.34f, .65f); path.lineTo(-.71f, .76f);
                path.quadTo(-1.09f, .73f, -.81f, .39f); path.quadTo(-1.12f, .43f, -1.11f, .24f);
                path.quadTo(-1.18f, -.10f, -.83f, .05f); path.lineTo(-.83f, -.48f); break;
            case 32: // Spot: soft puppy with long drooping ears and two little paws.
                path.moveTo(-.51f, -.69f); path.quadTo(0, -1.02f, .51f, -.69f);
                path.cubicTo(1.07f, -.77f, 1.24f, .74f, .75f, .67f); path.lineTo(.55f, .40f);
                path.quadTo(.71f, .89f, .22f, .76f); path.lineTo(0, .54f);
                path.lineTo(-.22f, .76f); path.quadTo(-.71f, .89f, -.55f, .40f);
                path.lineTo(-.75f, .67f); path.cubicTo(-1.24f, .74f, -1.07f, -.77f, -.51f, -.69f); break;
            case 33: // Bobo: antenna, square robot head and articulated arms.
                path.moveTo(-.14f, -.81f); path.lineTo(-.14f, -1.13f); path.lineTo(.14f, -1.13f);
                path.lineTo(.14f, -.81f); path.lineTo(.68f, -.81f); path.lineTo(.68f, -.25f);
                path.lineTo(1.03f, -.06f); path.lineTo(1.03f, .40f); path.lineTo(.63f, .40f);
                path.lineTo(.48f, .77f); path.lineTo(.18f, .77f); path.lineTo(0, .50f);
                path.lineTo(-.18f, .77f); path.lineTo(-.48f, .77f); path.lineTo(-.63f, .40f);
                path.lineTo(-1.03f, .40f); path.lineTo(-1.03f, -.06f);
                path.lineTo(-.68f, -.25f); path.lineTo(-.68f, -.81f); break;
            case 34: // Bloom: six gently rounded petals around a smiling centre.
                path.moveTo(0, -.69f); path.cubicTo(.12f, -1.18f, .72f, -1.12f, .65f, -.50f);
                path.cubicTo(1.25f, -.67f, 1.29f, -.04f, .72f, .18f);
                path.cubicTo(1.11f, .58f, .62f, 1.04f, .23f, .62f);
                path.cubicTo(.03f, 1.28f, -.63f, 1.14f, -.49f, .50f);
                path.cubicTo(-1.10f, .80f, -1.39f, .21f, -.75f, -.04f);
                path.cubicTo(-1.20f, -.54f, -.62f, -1.02f, -.25f, -.57f); break;
            default: // Orbit: compact raccoon pilot with ringed wing tips.
                path.moveTo(-.73f, -.50f); path.quadTo(-1.12f, -1.10f, -.43f, -.81f);
                path.quadTo(0, -.99f, .43f, -.81f); path.quadTo(1.12f, -1.10f, .73f, -.50f);
                path.lineTo(1.15f, -.13f); path.lineTo(.79f, .05f); path.lineTo(1.05f, .40f);
                path.lineTo(.63f, .44f); path.lineTo(.47f, .83f); path.lineTo(0, .68f);
                path.lineTo(-.47f, .83f); path.lineTo(-.63f, .44f); path.lineTo(-1.05f, .40f);
                path.lineTo(-.79f, .05f); path.lineTo(-1.15f, -.13f); break;
        }
        path.close(); p.setColor(0xff142938); drawPath(c, p, path, size);
        p.setColor(color); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(.068f);
        p.setStrokeJoin(Paint.Join.ROUND); drawPath(c, p, path, size); p.setStyle(Paint.Style.FILL);
        if (shape >= 18) {
            companionFace(c, p, path, shape, color, size);
        } else if (shape == 6 || shape == 17) {
            p.setColor(0xffeafff4); c.drawOval(-.40f, -.28f, .40f, .28f, p);
            p.setColor(0xff12303c);
            c.drawCircle(-.19f, -.03f, .068f, p); c.drawCircle(.19f, -.03f, .068f, p);
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(.047f);
            c.drawArc(-.17f, .03f, .17f, .22f, 20, 140, false, p); p.setStyle(Paint.Style.FILL);
        } else if (shape == 2 || shape == 11) {
            p.setColor(color); c.drawOval(-.54f, -.42f, .54f, .24f, p);
            p.setColor(0xffeafffa); c.drawOval(-.39f, -.32f, .39f, .13f, p);
            p.setColor(0xff1c5362); c.drawOval(-.30f, -.25f, .30f, .07f, p);
            p.setColor(0xffdbffff); c.drawOval(-.20f, -.21f, -.03f, -.14f, p);
        } else {
            path.reset(); path.moveTo(0, -.57f); path.lineTo(.22f, .25f);
            path.quadTo(0, .12f, -.22f, .25f); path.close();
            p.setColor(0xffedfff9); drawPath(c, p, path, size);
            p.setColor(color); c.drawCircle(0, -.13f, .078f, p);
        }
        p.setColor(color);
        c.drawCircle(-.53f, .30f, .073f, p); c.drawCircle(.53f, .30f, .073f, p);
        if (shape == 1 || shape == 8 || shape == 11) {
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(.030f);
            c.drawLine(-.52f, -.15f, -.25f, .40f, p); c.drawLine(.52f, -.15f, .25f, .40f, p);
            p.setStyle(Paint.Style.FILL);
        }
        if (shape == 16) {
            p.setColor(0xffffed9c); c.drawCircle(0, -.77f, .10f, p);
            c.drawCircle(-.65f, -.48f, .07f, p); c.drawCircle(.65f, -.48f, .07f, p);
        }
        p.setAlpha(255); p.setStyle(Paint.Style.FILL); p.setStrokeJoin(Paint.Join.MITER); c.restore();
    }

    /** Character-specific face details, using only the runway's existing geometry objects. */
    private static void companionFace(Canvas c, Paint p, Path path, int id, int color, float size) {
        float cy = id == 27 ? -.32f : id == 31 ? -.65f : id == 21 ? -.83f : -.08f;
        p.setStyle(Paint.Style.FILL); p.setColor(0xffeafaf7);
        if (id == 19) {
            path.reset(); path.moveTo(-.51f, -.24f); path.lineTo(0, .57f);
            path.lineTo(.51f, -.24f); path.quadTo(0, .10f, -.51f, -.24f); path.close();
            drawPath(c, p, path, size);
        } else if (id == 20) c.drawOval(-.37f, -.51f, .37f, .57f, p);
        else if (id == 30 || id == 33 || id == 31) {
            c.drawRoundRect(-.52f, cy - .28f, .52f, cy + .26f, .12f, .12f, p);
            p.setColor(0xff154552); c.drawRoundRect(-.45f, cy - .21f, .45f, cy + .19f, .09f, .09f, p);
        } else if (id == 21) {
            p.setColor(0xffbef6d3); c.drawOval(-.20f, -1.02f, .20f, -.64f, p);
            p.setColor(color); c.drawOval(-.50f, -.42f, .50f, .57f, p);
            p.setColor(0xff315e57); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(.04f);
            c.drawOval(-.48f, -.40f, .48f, .55f, p);
            c.drawLine(0, -.37f, 0, .53f, p); c.drawLine(-.45f, -.02f, .45f, .23f, p);
            c.drawLine(.45f, -.02f, -.45f, .23f, p); p.setStyle(Paint.Style.FILL);
        } else if (id == 26) {
            p.setColor(color); c.drawOval(-.49f, -.55f, .49f, .75f, p);
            p.setColor(0xff3a3930); c.drawRoundRect(-.48f, .20f, .48f, .35f, .05f, .05f, p);
            c.drawRoundRect(-.34f, .52f, .34f, .63f, .04f, .04f, p);
            p.setColor(0xffeafaf7); c.drawOval(-.34f, -.42f, .34f, .08f, p);
            cy = -.18f;
        } else if (id == 24) {
            p.setColor(0xffeafaf7); c.drawCircle(-.48f, -.61f, .18f, p);
            c.drawCircle(.48f, -.61f, .18f, p);
            p.setColor(0xff193c41); c.drawCircle(-.48f, -.61f, .080f, p);
            c.drawCircle(.48f, -.61f, .080f, p);
        } else if (id == 35) {
            p.setColor(0xffdae2f5); c.drawOval(-.64f, -.39f, .64f, .38f, p);
            p.setColor(0xff273f57); c.drawOval(-.58f, -.23f, .58f, .12f, p);
        } else if (id == 23) {
            c.drawOval(-.53f, -.46f, .02f, .17f, p); c.drawOval(-.02f, -.46f, .53f, .17f, p);
        } else c.drawOval(-.44f, cy - .32f, .44f, cy + .33f, p);

        if (id == 22) {
            p.setColor(0xff283b51); c.drawOval(-.40f, -.31f, -.10f, .10f, p);
            c.drawOval(.10f, -.31f, .40f, .10f, p);
        }
        if (id == 24) {
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(.052f); p.setColor(0xffc2ffe0);
            c.drawArc(-.35f, -.03f, .35f, .35f, 12, 156, false, p); p.setStyle(Paint.Style.FILL);
        } else {
            boolean visor = id == 30 || id == 31 || id == 33 || id == 22 || id == 35;
            p.setColor(visor ? 0xffd6fff5 : 0xff173a45);
            float eyeY = cy - .05f, eyeSize = id == 21 ? .040f : .066f;
            float eyeX = id == 21 ? .12f : id == 23 ? .24f : .19f;
            c.drawCircle(-eyeX, eyeY, eyeSize, p); c.drawCircle(eyeX, eyeY, eyeSize, p);
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(.043f);
            c.drawArc(-.14f, cy + .03f, .14f, cy + .20f, 12, 156, false, p); p.setStyle(Paint.Style.FILL);
        }
        if (id == 18) {
            p.setColor(0xffff84b6); c.drawOval(-.52f, -1.02f, -.35f, -.46f, p);
            c.drawOval(.29f, -1.06f, .46f, -.46f, p);
        }
        if (id == 19 || id == 32 || id == 35) {
            p.setColor(0xff173a45); c.drawOval(-.10f, .11f, .10f, .22f, p);
        }
        if (id == 20 || id == 23) {
            p.setColor(0xffffc85f); path.reset(); path.moveTo(0, .03f);
            path.lineTo(.13f, .13f); path.lineTo(0, .26f); path.lineTo(-.13f, .13f);
            path.close(); drawPath(c, p, path, size);
        }
        if (id == 30) {
            p.setColor(0xffffbda0); c.drawRoundRect(-.24f, .38f, .24f, .52f, .04f, .04f, p);
            p.setColor(0xffedfaff); c.drawLine(.59f, -.36f, .75f, -.05f, p);
        }
        if (id == 31) {
            p.setColor(0xff778bab); c.drawCircle(-.82f, .28f, .15f, p);
            c.drawCircle(.82f, .28f, .15f, p); c.drawCircle(-.66f, .56f, .15f, p);
            c.drawCircle(.66f, .56f, .15f, p); c.drawCircle(0, .68f, .14f, p);
        }
        if (id == 33) {
            p.setColor(0xffffdf7d); c.drawCircle(0, -1.00f, .075f, p);
            c.drawRoundRect(-.22f, .35f, .22f, .45f, .025f, .025f, p);
        }
        if (id == 29) {
            p.setColor(0xff375149); c.drawCircle(-.12f, .27f, .045f, p);
            c.drawCircle(.12f, .27f, .045f, p);
        }
        if (id == 25 || id == 27 || id == 28 || id == 34) {
            p.setColor(0xffffa6c5); c.drawOval(-.37f, cy + .07f, -.24f, cy + .14f, p);
            c.drawOval(.24f, cy + .07f, .37f, cy + .14f, p);
        }
    }

    private static void flame(Canvas c, Paint p, Path path, int color, float x, float y, float length, float size) {
        path.reset(); path.moveTo(x - .115f, y);
        path.quadTo(x - .19f, y + length * .45f, x, y + length);
        path.quadTo(x + .19f, y + length * .45f, x + .115f, y); path.close();
        p.setColor((color & 0xffffff) | 0x85000000); drawPath(c, p, path, size);
        p.setColor(0xffd9fff7); c.drawOval(x - .07f, y - .03f, x + .07f, y + .20f, p);
    }

    private static void star(Path path, float x, float y, float radius, float inside, int tips, float angle) {
        for (int i = 0; i < tips * 2; i++) {
            float r = (i & 1) == 0 ? radius : inside;
            float a = angle + i * (float) Math.PI / tips;
            float px = x + (float) Math.cos(a) * r, py = y + (float) Math.sin(a) * r;
            if (i == 0) path.moveTo(px, py); else path.lineTo(px, py);
        }
    }
}
