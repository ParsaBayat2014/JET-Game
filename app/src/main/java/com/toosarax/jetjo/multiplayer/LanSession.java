package com.toosarax.jetjo.multiplayer;

import java.io.*;
import java.net.*;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/** A single peer TCP room on a private network. All I/O and authoritative simulation run off the UI thread. */
public final class LanSession implements AutoCloseable {
    public interface Listener {
        void room(String address, int port, String pin);
        void connected(boolean hosting);
        void snapshot(LanProtocol.Snapshot snapshot);
        void failed(String reason);
    }
    private final Executor callbacks;
    private final Listener listener;
    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicBoolean snapshotQueued = new AtomicBoolean();
    private final Object writeLock = new Object(), resourceLock = new Object();
    private final CountDownLatch finishAck = new CountDownLatch(1);
    private volatile ServerSocket server;
    private volatile Socket socket;
    private volatile Thread worker, reader;
    private volatile int lane = 1, remoteLane = 1;
    private volatile boolean paused, remotePaused;
    private volatile LanProtocol.Snapshot latest;
    private final boolean hosting, allowLoopback;
    private final InetAddress address;
    private final int port, world;
    private String pin;
    private int skin = 0, remoteSkin = 1;
    private LanSession(boolean hosting, InetAddress address, int port, String pin, int world, boolean allowLoopback, Executor callbacks, Listener listener) {
        this.hosting = hosting; this.address = address; this.port = port; this.pin = pin; this.world = Math.max(0, Math.min(63, world));
        this.allowLoopback = allowLoopback; this.callbacks = callbacks; this.listener = listener;
    }
    public static LanSession host(String address, int world, Executor callbacks, Listener listener) throws IOException {
        return host(address, world, 0, callbacks, listener);
    }
    public static LanSession host(String address, int world, int skin, Executor callbacks, Listener listener) throws IOException {
        InetAddress local = privateAddress(address, false);
        if (!LanProtocol.freeSkin(skin)) throw new IOException("invalid free character");
        LanSession session = new LanSession(true, local, 0, null, world, false, callbacks, listener); session.skin = skin; return start(session);
    }
    public static LanSession join(String address, int port, String pin, Executor callbacks, Listener listener) throws IOException {
        return join(address, port, pin, 1, callbacks, listener);
    }
    public static LanSession join(String address, int port, String pin, int skin, Executor callbacks, Listener listener) throws IOException {
        if (port < 1024 || port > 65535) throw new IOException("invalid port");
        LanProtocol.hello(pin, skin);
        LanSession session = new LanSession(false, privateAddress(address, false), port, pin, 0, false, callbacks, listener); session.skin = skin; return start(session);
    }
    static LanSession loopbackHost(Executor callbacks, Listener listener) throws IOException {
        return start(new LanSession(true, privateAddress("127.0.0.1", true), 0, "314159", 0, true, callbacks, listener));
    }
    static LanSession loopbackJoin(int port, String pin, Executor callbacks, Listener listener) throws IOException {
        return start(new LanSession(false, privateAddress("127.0.0.1", true), port, pin, 0, true, callbacks, listener));
    }
    private static LanSession start(LanSession session) {
        session.worker = new Thread(session::run, "jetjo-lan-session"); session.worker.setDaemon(true); session.worker.start(); return session;
    }
    /** Numeric IPv4 only: no DNS, public Internet destination, discovery, or relay. */
    static InetAddress privateAddress(String text, boolean loopback) throws IOException {
        String[] parts = text.trim().split("\\.", -1);
        if (parts.length != 4) throw new IOException("private IPv4 required");
        byte[] bytes = new byte[4];
        for (int i = 0; i < 4; i++) {
            if (!parts[i].matches("[0-9]{1,3}")) throw new IOException("private IPv4 required");
            int value = Integer.parseInt(parts[i]); if (value > 255) throw new IOException("private IPv4 required"); bytes[i] = (byte) value;
        }
        InetAddress result = InetAddress.getByAddress(bytes);
        if (!result.isSiteLocalAddress() && !(loopback && result.isLoopbackAddress())) throw new IOException("private IPv4 required");
        return result;
    }
    /** Only addresses of live LAN interfaces; an unavailable hotspot is never guessed. */
    public static List<String> localAddresses() {
        List<String> addresses = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces != null && interfaces.hasMoreElements()) {
                NetworkInterface network = interfaces.nextElement(); if (!network.isUp() || network.isLoopback()) continue;
                Enumeration<InetAddress> list = network.getInetAddresses();
                while (list.hasMoreElements()) {
                    InetAddress a = list.nextElement();
                    if (a instanceof Inet4Address && a.isSiteLocalAddress() && !addresses.contains(a.getHostAddress())) addresses.add(a.getHostAddress());
                }
            }
        } catch (SocketException | SecurityException ignored) { }
        return addresses;
    }
    public void input(int lane, boolean paused) { this.lane = Math.max(0, Math.min(2, lane)); this.paused = paused; }
    public LanProtocol.Snapshot latest() { return latest; }
    public boolean isClosed() { return closed.get(); }
    private void dispatch(Runnable action) { callbacks.execute(() -> { if (!closed.get()) action.run(); }); }
    private void configure(Socket peer) throws SocketException {
        peer.setTcpNoDelay(true); peer.setSoTimeout(8000); peer.setKeepAlive(true);
    }
    private boolean attach(Socket peer) throws IOException {
        synchronized (resourceLock) { if (closed.get()) { peer.close(); return false; } socket = peer; return true; }
    }
    private boolean attach(ServerSocket room) throws IOException {
        synchronized (resourceLock) { if (closed.get()) { room.close(); return false; } server = room; return true; }
    }
    private void run() {
        try {
            if (closed.get()) return;
            long seed = 700;
            if (hosting && !allowLoopback) {
                SecureRandom random = new SecureRandom(); seed = random.nextLong();
                pin = String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
            }
            if (hosting) {
                ServerSocket room = new ServerSocket(); if (!attach(room)) return; room.setReuseAddress(true); room.bind(new InetSocketAddress(address, 0), 1); room.setSoTimeout(1000);
                dispatch(() -> listener.room(address.getHostAddress(), room.getLocalPort(), pin));
                while (!closed.get()) {
                    Socket candidate;
                    try { candidate = room.accept(); } catch (SocketTimeoutException ignored) { continue; }
                    if (!attach(candidate)) return;
                    if (!candidate.getInetAddress().isSiteLocalAddress() && !(allowLoopback && candidate.getInetAddress().isLoopbackAddress())) { candidate.close(); continue; }
                    candidate.setSoTimeout(5000);
                    boolean accepted = false;
                    try {
                        LanProtocol.Frame hello = LanProtocol.read(candidate.getInputStream());
                        if (LanProtocol.validHello(hello, pin)) {
                            remoteSkin = hello.payload[6] & 255;
                            LanProtocol.write(candidate.getOutputStream(), LanProtocol.WELCOME, LanProtocol.welcome(seed, world)); accepted = true;
                        } else LanProtocol.write(candidate.getOutputStream(), LanProtocol.REJECT, new byte[0]);
                    } catch (IOException ignored) { }
                    if (accepted) break;
                    candidate.close(); socket = null;
                }
                room.close(); server = null;
            } else {
                Socket peer = new Socket(); if (!attach(peer)) return; peer.connect(new InetSocketAddress(address, port), 6000); peer.setSoTimeout(5000);
                LanProtocol.write(peer.getOutputStream(), LanProtocol.HELLO, LanProtocol.hello(pin, skin));
                LanProtocol.validateWelcome(LanProtocol.read(peer.getInputStream()));
            }
            if (closed.get() || socket == null) return;
            configure(socket);
            dispatch(() -> listener.connected(hosting));
            reader = new Thread(this::readLoop, "jetjo-lan-reader"); reader.setDaemon(true); reader.start();
            if (hosting) hostLoop(seed); else clientLoop();
        } catch (IOException | RuntimeException exception) {
            // A final snapshot may already be in the TCP receive buffer when the next input
            // write fails. Let the reader consume it (or report EOF/timeout) before closing.
            Thread receiver = reader;
            if (!closed.get() && receiver != null && receiver.isAlive()) {
                try { receiver.join(8500); } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
            }
            if (!closed.get()) fail("connection");
        } finally { close(); }
    }
    private void readLoop() {
        try {
            InputStream stream = socket.getInputStream();
            while (!closed.get()) {
                LanProtocol.Frame frame = LanProtocol.read(stream);
                if (hosting) {
                    if (frame.type == LanProtocol.FINISH_ACK) {
                        if (frame.payload.length != 0 || latest == null || !latest.finished) throw new IOException("unexpected acknowledgement");
                        finishAck.countDown();
                    } else {
                        LanProtocol.Input input = LanProtocol.decodeInput(frame); remoteLane = input.lane; remotePaused = input.paused;
                    }
                } else {
                    LanProtocol.Snapshot state = LanProtocol.decodeSnapshot(frame);
                    LanProtocol.Snapshot prior = latest;
                    if (prior != null && state.tick < prior.tick) throw new IOException("non-monotonic snapshot");
                    if (state.finished) {
                        // Confirm receipt before the hosting activity closes its result room.
                        latest = state;
                        try { synchronized (writeLock) { LanProtocol.write(socket.getOutputStream(), LanProtocol.FINISH_ACK, new byte[0]); } }
                        catch (IOException ignored) { /* Valid final results remain usable if this final ack cannot be sent. */ }
                        publish(state); return;
                    } else publish(state);
                }
            }
        } catch (IOException | RuntimeException exception) {
            if (!closed.get()) {
                if (latest != null && latest.finished) close(); else fail("disconnected");
            }
        }
    }
    private void publish(LanProtocol.Snapshot state) {
        latest = state;
        // Terminal data must survive the peer closing immediately after its result screen appears.
        if (state.finished) callbacks.execute(() -> listener.snapshot(state));
        else if (snapshotQueued.compareAndSet(false, true)) callbacks.execute(() -> {
            snapshotQueued.set(false);
            LanProtocol.Snapshot current = latest;
            if (!closed.get() && current != null && !current.finished) listener.snapshot(current);
        });
    }
    private void hostLoop(long seed) throws IOException {
        DuelMatch match = new DuelMatch(seed, world, skin, remoteSkin); OutputStream out = socket.getOutputStream();
        long deadline = System.nanoTime(), start = deadline + 1_000_000_000L; int frame = 0;
        while (!closed.get()) {
            long now = System.nanoTime();
            if (now >= start) match.step(lane, remoteLane, paused || remotePaused);
            if (frame++ % 3 == 0) {
                LanProtocol.Snapshot state = match.snapshot();
                if (state.finished) {
                    latest = state; LanProtocol.write(out, LanProtocol.SNAPSHOT, LanProtocol.snapshot(state));
                    try { finishAck.await(2, TimeUnit.SECONDS); } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
                    publish(state); return;
                }
                LanProtocol.write(out, LanProtocol.SNAPSHOT, LanProtocol.snapshot(state)); publish(state);
            }
            deadline += 16_666_667L;
            // A stalled host never fast-forwards the race after Android resumes it.
            if (deadline < now - 100_000_000L) deadline = now;
            sleepUntil(deadline);
        }
    }
    private void clientLoop() throws IOException {
        OutputStream out = socket.getOutputStream();
        while (!closed.get()) {
            synchronized (writeLock) { if (latest == null || !latest.finished) LanProtocol.write(out, LanProtocol.INPUT, LanProtocol.input(lane, paused)); }
            sleepUntil(System.nanoTime() + 50_000_000L);
        }
    }
    private static void sleepUntil(long deadline) throws IOException {
        long nanos = Math.max(0, deadline - System.nanoTime());
        try { if (nanos > 0) Thread.sleep(nanos / 1_000_000L, (int) (nanos % 1_000_000L)); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IOException("closed", e); }
    }
    private void fail(String reason) {
        if (!closed.compareAndSet(false, true)) return;
        callbacks.execute(() -> listener.failed(reason)); release();
    }
    @Override public void close() { closed.set(true); release(); }
    private void release() {
        Socket peer; ServerSocket room;
        synchronized (resourceLock) { peer = socket; room = server; }
        if (peer != null) try { peer.close(); } catch (IOException ignored) { }
        if (room != null) try { room.close(); } catch (IOException ignored) { }
        Thread a = worker, b = reader;
        if (a != null && a != Thread.currentThread()) a.interrupt(); if (b != null && b != Thread.currentThread()) b.interrupt();
    }
    boolean workersStopped() { return (worker == null || !worker.isAlive()) && (reader == null || !reader.isAlive()); }
}
