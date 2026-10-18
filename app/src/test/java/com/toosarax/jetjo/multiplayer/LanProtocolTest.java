package com.toosarax.jetjo.multiplayer;

import java.io.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class LanProtocolTest {
    private byte[] frame(int type, byte[] payload) throws IOException {
        ByteArrayOutputStream stream = new ByteArrayOutputStream(); LanProtocol.write(stream, type, payload); return stream.toByteArray();
    }
    private LanProtocol.Frame read(byte[] bytes) throws IOException { return LanProtocol.read(new ByteArrayInputStream(bytes)); }
    @Test public void fragmentedTcpInputIsReassembled() throws Exception {
        byte[] bytes = frame(LanProtocol.INPUT, LanProtocol.input(2, true));
        InputStream fragmented = new FilterInputStream(new ByteArrayInputStream(bytes)) {
            @Override public int read(byte[] b, int off, int len) throws IOException { return super.read(b, off, Math.min(len, 1)); }
        };
        LanProtocol.Input decoded = LanProtocol.decodeInput(LanProtocol.read(fragmented)); assertEquals(2, decoded.lane); assertTrue(decoded.paused);
    }
    @Test public void twoFramesAreNotMerged() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(); bytes.write(frame(LanProtocol.INPUT, LanProtocol.input(0, false))); bytes.write(frame(LanProtocol.INPUT, LanProtocol.input(2, true)));
        InputStream in = new ByteArrayInputStream(bytes.toByteArray()); assertEquals(0, LanProtocol.decodeInput(LanProtocol.read(in)).lane); assertEquals(2, LanProtocol.decodeInput(LanProtocol.read(in)).lane); assertEquals(-1, in.read());
    }
    @Test public void roomPinRejectsAccidentalPeers() throws Exception {
        LanProtocol.Frame hello = read(frame(LanProtocol.HELLO, LanProtocol.hello("001122")));
        assertTrue(LanProtocol.validHello(hello, "001122")); assertFalse(LanProtocol.validHello(hello, "000000"));
        assertFalse(LanProtocol.validHello(read(frame(LanProtocol.INPUT, LanProtocol.input(1, false))), "001122"));
    }
    @Test public void onlyFreeCharacterIdsAreAccepted() throws Exception {
        assertThrows(IOException.class, () -> LanProtocol.hello("001122", 10));
        assertThrows(IOException.class, () -> LanProtocol.hello("001122", 17));
        assertThrows(IOException.class, () -> LanProtocol.hello("001122", 36));
        LanProtocol.Frame hello = read(frame(LanProtocol.HELLO, LanProtocol.hello("001122", 35))); assertTrue(LanProtocol.validHello(hello, "001122"));
        LanProtocol.Snapshot state = new DuelMatch(5, 0, 18, 35).snapshot();
        LanProtocol.Snapshot received = LanProtocol.decodeSnapshot(read(frame(LanProtocol.SNAPSHOT, LanProtocol.snapshot(state)))); assertEquals(18, received.pilots[0].skin); assertEquals(35, received.pilots[1].skin);
    }
    @Test public void unknownSchemaAndWrongMagicAreRejected() throws Exception {
        byte[] version = frame(LanProtocol.INPUT, LanProtocol.input(1, false)); version[11] = 7; assertThrows(IOException.class, () -> read(version));
        byte[] magic = frame(LanProtocol.INPUT, LanProtocol.input(1, false)); magic[4] = 0; assertThrows(IOException.class, () -> read(magic));
    }
    @Test public void oversizedNegativeAndTruncatedFramesFail() throws Exception {
        byte[] large = new byte[]{0, 1, 0, 0}; assertThrows(IOException.class, () -> read(large));
        assertThrows(IOException.class, () -> read(new byte[]{-1, -1, -1, -1}));
        byte[] full = frame(LanProtocol.INPUT, LanProtocol.input(1, false)); assertThrows(EOFException.class, () -> read(java.util.Arrays.copyOf(full, full.length - 1)));
    }
    @Test public void inputsRejectOutOfRangeBooleanAndExtraPayload() throws Exception {
        assertThrows(IOException.class, () -> LanProtocol.decodeInput(read(frame(LanProtocol.INPUT, new byte[]{3, 0}))));
        assertThrows(IOException.class, () -> LanProtocol.decodeInput(read(frame(LanProtocol.INPUT, new byte[]{1, 2}))));
        assertThrows(IOException.class, () -> LanProtocol.decodeInput(read(frame(LanProtocol.INPUT, new byte[]{1, 0, 4}))));
    }
    @Test public void snapshotRoundTripsBothTracksAndPowers() throws Exception {
        DuelMatch match = new DuelMatch(43, 2); for (int i = 0; i < 360; i++) match.step(i % 3, 2, false);
        LanProtocol.Snapshot original = match.snapshot(), decoded = LanProtocol.decodeSnapshot(read(frame(LanProtocol.SNAPSHOT, LanProtocol.snapshot(original))));
        assertEquals(original.tick, decoded.tick); assertEquals(original.seconds, decoded.seconds, 0); assertEquals(2, decoded.world);
        for (int p = 0; p < 2; p++) { assertEquals(original.pilots[p].score, decoded.pilots[p].score); assertEquals(original.pilots[p].lane, decoded.pilots[p].lane, 0); assertEquals(original.pilots[p].items.length, decoded.pilots[p].items.length); }
    }
    @Test public void snapshotRejectsNonFiniteCoordinatesAndEntityOverflow() throws Exception {
        byte[] payload = LanProtocol.snapshot(new DuelMatch(3, 0).snapshot());
        // snapshot header: tick4, world4, seconds4, paused1, finished1; first lane follows.
        byte[] nan = payload.clone(); nan[14] = 0x7f; nan[15] = (byte)0xc0; nan[16] = 0; nan[17] = 0;
        assertThrows(IOException.class, () -> LanProtocol.decodeSnapshot(read(frame(LanProtocol.SNAPSHOT, nan))));
        // First pilot entity count follows lane4, score8, hits4, combo4, skin1, overdrive4, charge4, powers16.
        byte[] count = payload.clone(); count[59] = 1; count[60] = 0;
        assertThrows(IOException.class, () -> LanProtocol.decodeSnapshot(read(frame(LanProtocol.SNAPSHOT, count))));
    }
    @Test public void welcomeRequiresIdenticalRules() throws Exception {
        LanProtocol.validateWelcome(read(frame(LanProtocol.WELCOME, LanProtocol.welcome(11, 2))));
        byte[] rules = LanProtocol.welcome(11, 2); rules[15] = 1;
        assertThrows(IOException.class, () -> LanProtocol.validateWelcome(read(frame(LanProtocol.WELCOME, rules))));
    }
}
