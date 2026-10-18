package com.toosarax.jetjo.multiplayer;

import com.toosarax.jetjo.core.GameConfig;
import com.toosarax.jetjo.core.GameEngine;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Explicit, bounded wire schema shared by both phones. No Java object deserialization. */
public final class LanProtocol {
    private LanProtocol() {}
    public static final int VERSION = 1, MAGIC = 0x4a45544a, MAX_FRAME = 8192;
    public static final int HELLO = 1, WELCOME = 2, INPUT = 3, SNAPSHOT = 4, REJECT = 5, FINISH_ACK = 6;
    public static final int MATCH_SECONDS = 75;
    public static final class Frame {
        public final int type;
        public final byte[] payload;
        Frame(int type, byte[] payload) { this.type = type; this.payload = payload; }
    }
    public static final class Input {
        public final int lane;
        public final boolean paused;
        Input(int lane, boolean paused) { this.lane = lane; this.paused = paused; }
    }
    public static final class Item {
        public final int type, lane, style;
        public final float y;
        Item(int type, int lane, int style, float y) { this.type = type; this.lane = lane; this.style = style; this.y = y; }
    }
    public static final class Pilot {
        public final float lane, overdrive, charge;
        public final long score;
        public final int hits, combo, skin;
        public final float[] powers;
        public final Item[] items;
        Pilot(float lane, long score, int hits, int combo, int skin, float overdrive, float charge, float[] powers, Item[] items) {
            this.lane = lane; this.score = score; this.hits = hits; this.combo = combo; this.skin = skin;
            this.overdrive = overdrive; this.charge = charge; this.powers = powers; this.items = items;
        }
    }
    public static final class Snapshot {
        public final int tick, world;
        public final float seconds;
        public final boolean paused, finished;
        public final Pilot[] pilots;
        Snapshot(int tick, int world, float seconds, boolean paused, boolean finished, Pilot[] pilots) {
            this.tick = tick; this.world = world; this.seconds = seconds; this.paused = paused; this.finished = finished; this.pilots = pilots;
        }
    }
    private interface Encoder { void encode(DataOutputStream out) throws IOException; }
    private static byte[] payload(Encoder encoder) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) { encoder.encode(out); }
        if (bytes.size() > MAX_FRAME - 10) throw new IOException("frame too large");
        return bytes.toByteArray();
    }
    public static void write(OutputStream stream, int type, byte[] payload) throws IOException {
        if (type < HELLO || type > FINISH_ACK || payload.length > MAX_FRAME - 10) throw new IOException("invalid frame");
        DataOutputStream out = new DataOutputStream(stream);
        out.writeInt(payload.length + 10); out.writeInt(MAGIC); out.writeInt(VERSION); out.writeShort(type);
        out.write(payload); out.flush();
    }
    public static Frame read(InputStream stream) throws IOException {
        DataInputStream in = new DataInputStream(stream);
        int size = in.readInt();
        if (size < 10 || size > MAX_FRAME) throw new IOException("invalid frame length");
        int magic = in.readInt(), version = in.readInt(), type = in.readUnsignedShort();
        if (magic != MAGIC || version != VERSION) throw new IOException("incompatible game version");
        if (type < HELLO || type > FINISH_ACK) throw new IOException("unknown frame");
        byte[] payload = new byte[size - 10]; in.readFully(payload); return new Frame(type, payload);
    }
    private static void end(DataInputStream in) throws IOException { if (in.available() != 0) throw new IOException("trailing payload"); }
    private static int range(int value, int min, int max) throws IOException {
        if (value < min || value > max) throw new IOException("invalid field"); return value;
    }
    private static float finite(float value, float min, float max) throws IOException {
        if (!Float.isFinite(value) || value < min || value > max) throw new IOException("invalid float"); return value;
    }
    private static boolean bool(DataInputStream in) throws IOException { return range(in.readUnsignedByte(), 0, 1) != 0; }
    public static byte[] hello(String pin) throws IOException {
        return hello(pin, 1);
    }
    public static boolean freeSkin(int skin) { return skin >= 0 && (skin < 10 || (skin >= 18 && skin <= 35)); }
    public static byte[] hello(String pin, int skin) throws IOException {
        if (!pin.matches("[0-9]{6}")) throw new IOException("invalid room code");
        if (!freeSkin(skin)) throw new IOException("invalid free character");
        byte[] result = new byte[7]; System.arraycopy(pin.getBytes(StandardCharsets.US_ASCII), 0, result, 0, 6); result[6] = (byte)skin; return result;
    }
    public static boolean validHello(Frame frame, String pin) {
        return frame.type == HELLO && frame.payload.length == 7 && freeSkin(frame.payload[6] & 255) && pin.equals(new String(frame.payload, 0, 6, StandardCharsets.US_ASCII));
    }
    public static byte[] welcome(long seed, int world) throws IOException { return payload(o -> { o.writeLong(seed); o.writeInt(world); o.writeInt(MATCH_SECONDS); }); }
    public static void validateWelcome(Frame frame) throws IOException {
        if (frame.type != WELCOME) throw new IOException("room rejected");
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(frame.payload));
        in.readLong(); range(in.readInt(), 0, 63); if (in.readInt() != MATCH_SECONDS) throw new IOException("match rules mismatch"); end(in);
    }
    public static byte[] input(int lane, boolean paused) throws IOException {
        range(lane, 0, 2); return payload(o -> { o.writeByte(lane); o.writeBoolean(paused); });
    }
    public static Input decodeInput(Frame frame) throws IOException {
        if (frame.type != INPUT) throw new IOException("expected input");
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(frame.payload));
        Input input = new Input(range(in.readUnsignedByte(), 0, 2), bool(in)); end(in); return input;
    }
    public static Snapshot capture(int tick, int world, float seconds, boolean paused, boolean finished, GameEngine[] engines) {
        return capture(tick, world, seconds, paused, finished, engines, new int[]{0, 1});
    }
    public static Snapshot capture(int tick, int world, float seconds, boolean paused, boolean finished, GameEngine[] engines, int[] skins) {
        Pilot[] pilots = new Pilot[2];
        for (int i = 0; i < 2; i++) {
            GameEngine e = engines[i]; int count = 0;
            for (GameEngine.Entity item : e.entities) if (item.active) count++;
            Item[] items = new Item[count]; int at = 0;
            for (GameEngine.Entity item : e.entities) if (item.active) items[at++] = new Item(item.type, item.lane, item.style, item.y);
            pilots[i] = new Pilot(e.playerLane, e.score(), e.hits, e.combo, skins[i], e.overdrive, e.charge, e.power.clone(), items);
        }
        return new Snapshot(tick, world, seconds, paused, finished, pilots);
    }
    public static byte[] snapshot(Snapshot state) throws IOException {
        return payload(o -> {
            o.writeInt(state.tick); o.writeInt(state.world); o.writeFloat(state.seconds); o.writeBoolean(state.paused); o.writeBoolean(state.finished);
            for (Pilot pilot : state.pilots) {
                o.writeFloat(pilot.lane); o.writeLong(pilot.score); o.writeInt(pilot.hits); o.writeInt(pilot.combo); o.writeByte(pilot.skin);
                o.writeFloat(pilot.overdrive); o.writeFloat(pilot.charge);
                for (float power : pilot.powers) o.writeFloat(power);
                o.writeShort(pilot.items.length);
                for (Item item : pilot.items) { o.writeByte(item.type); o.writeByte(item.lane); o.writeByte(item.style); o.writeFloat(item.y); }
            }
        });
    }
    public static Snapshot decodeSnapshot(Frame frame) throws IOException {
        if (frame.type != SNAPSHOT) throw new IOException("expected snapshot");
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(frame.payload));
        int tick = range(in.readInt(), 0, 1_000_000), world = range(in.readInt(), 0, 63);
        float seconds = finite(in.readFloat(), 0, MATCH_SECONDS); boolean paused = bool(in), finished = bool(in);
        Pilot[] pilots = new Pilot[2];
        for (int p = 0; p < 2; p++) {
            float lane = finite(in.readFloat(), 0, 2); long score = in.readLong();
            if (score < 0 || score > 1_000_000_000L) throw new IOException("invalid score");
            int hits = range(in.readInt(), 0, 100_000), combo = range(in.readInt(), 0, 100_000), skin = range(in.readUnsignedByte(), 0, 35);
            if (!freeSkin(skin)) throw new IOException("invalid free character");
            float overdrive = finite(in.readFloat(), 0, 60), charge = finite(in.readFloat(), 0, 1);
            float[] powers = new float[4]; for (int i = 0; i < 4; i++) powers[i] = finite(in.readFloat(), 0, 60);
            int count = range(in.readUnsignedShort(), 0, GameConfig.MAX_ENTITIES); Item[] items = new Item[count];
            for (int i = 0; i < count; i++) items[i] = new Item(range(in.readUnsignedByte(), 0, GameEngine.DOUBLE), range(in.readUnsignedByte(), 0, 2), range(in.readUnsignedByte(), 0, 32), finite(in.readFloat(), -2, 2));
            pilots[p] = new Pilot(lane, score, hits, combo, skin, overdrive, charge, powers, items);
        }
        end(in); return new Snapshot(tick, world, seconds, paused, finished, pilots);
    }
}
