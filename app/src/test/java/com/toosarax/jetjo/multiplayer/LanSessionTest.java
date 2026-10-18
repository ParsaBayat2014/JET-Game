package com.toosarax.jetjo.multiplayer;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;
import org.junit.Test;
import static org.junit.Assert.*;

public class LanSessionTest {
    static class Events implements LanSession.Listener {
        final CountDownLatch room = new CountDownLatch(1), connected = new CountDownLatch(1), snapshots = new CountDownLatch(1), failed = new CountDownLatch(1);
        volatile int port;
        volatile LanProtocol.Snapshot state;
        @Override public void room(String address, int port, String pin) { this.port = port; room.countDown(); }
        @Override public void connected(boolean hosting) { connected.countDown(); }
        @Override public void snapshot(LanProtocol.Snapshot state) { this.state = state; snapshots.countDown(); }
        @Override public void failed(String reason) { failed.countDown(); }
    }
    private static void until(BooleanSupplier condition) throws Exception {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(4);
        while (!condition.getAsBoolean() && System.nanoTime() < end) Thread.sleep(10);
        assertTrue(condition.getAsBoolean());
    }
    @Test public void realSocketsHandshakeSendInputPauseAndDisconnectWithoutLeakingThreads() throws Exception {
        Events hostEvents = new Events(), guestEvents = new Events();
        LanSession host = LanSession.loopbackHost(Runnable::run, hostEvents), guest = null;
        try {
            assertTrue(hostEvents.room.await(3, TimeUnit.SECONDS)); guest = LanSession.loopbackJoin(hostEvents.port, "314159", Runnable::run, guestEvents);
            assertTrue(hostEvents.connected.await(3, TimeUnit.SECONDS)); assertTrue(guestEvents.connected.await(3, TimeUnit.SECONDS)); assertTrue(guestEvents.snapshots.await(3, TimeUnit.SECONDS));
            host.input(0, false); guest.input(2, false);
            until(() -> guestEvents.state != null && guestEvents.state.seconds > 1 && guestEvents.state.pilots[0].lane < .1f && guestEvents.state.pilots[1].lane > 1.9f);
            guest.input(2, true); until(() -> guestEvents.state.paused); float seconds = guestEvents.state.seconds;
            Thread.sleep(180); assertEquals(seconds, guestEvents.state.seconds, .04);
            guest.input(2, false); until(() -> !guestEvents.state.paused && guestEvents.state.seconds > seconds);
            host.close(); assertTrue(guestEvents.failed.await(3, TimeUnit.SECONDS));
        } finally {
            host.close(); if (guest != null) guest.close(); LanSession finalGuest = guest; until(() -> host.workersStopped() && (finalGuest == null || finalGuest.workersStopped()));
        }
    }
    @Test public void wrongPinIsRejectedAndHostCanAcceptNextCorrectGuest() throws Exception {
        Events hostEvents = new Events(), wrongEvents = new Events(), goodEvents = new Events();
        LanSession host = LanSession.loopbackHost(Runnable::run, hostEvents), wrong = null, good = null;
        try {
            assertTrue(hostEvents.room.await(3, TimeUnit.SECONDS)); wrong = LanSession.loopbackJoin(hostEvents.port, "000000", Runnable::run, wrongEvents);
            assertTrue(wrongEvents.failed.await(3, TimeUnit.SECONDS)); assertEquals(1, hostEvents.connected.getCount());
            good = LanSession.loopbackJoin(hostEvents.port, "314159", Runnable::run, goodEvents); assertTrue(goodEvents.connected.await(3, TimeUnit.SECONDS));
        } finally {
            host.close(); if (wrong != null) wrong.close(); if (good != null) good.close(); LanSession a = wrong, b = good;
            until(() -> host.workersStopped() && (a == null || a.workersStopped()) && (b == null || b.workersStopped()));
        }
    }
    @Test public void cancellationClosesAnIdleHostingSocket() throws Exception {
        Events events = new Events(); LanSession host = LanSession.loopbackHost(Runnable::run, events);
        assertTrue(events.room.await(3, TimeUnit.SECONDS)); host.close(); until(host::workersStopped); assertTrue(host.isClosed()); assertEquals(1, events.failed.getCount());
    }
    @Test public void malformedPeerDoesNotDestroyWaitingRoom() throws Exception {
        Events events = new Events(); LanSession host = LanSession.loopbackHost(Runnable::run, events);
        try {
            assertTrue(events.room.await(3, TimeUnit.SECONDS));
            try (Socket malformed = new Socket("127.0.0.1", events.port)) { malformed.getOutputStream().write(new byte[]{-1, -1, -1, -1}); malformed.getOutputStream().flush(); }
            Events accepted = new Events(); LanSession guest = LanSession.loopbackJoin(events.port, "314159", Runnable::run, accepted);
            try { assertTrue(accepted.connected.await(3, TimeUnit.SECONDS)); } finally { guest.close(); until(guest::workersStopped); }
        } finally { host.close(); until(host::workersStopped); }
    }
    @Test public void immediateCancellationStopsClientsDuringSocketCreationAndHandshake() throws Exception {
        java.util.List<LanSession> clients = new java.util.ArrayList<>();
        java.util.List<Events> events = new java.util.ArrayList<>();
        java.util.List<Socket> accepted = new java.util.concurrent.CopyOnWriteArrayList<>();
        try (ServerSocket room = new ServerSocket(0, 50, InetAddress.getByName("127.0.0.1"))) {
            Thread peer = new Thread(() -> {
                try { while (!room.isClosed()) accepted.add(room.accept()); }
                catch (IOException closed) { }
            }); peer.setDaemon(true); peer.start();
            try {
                // The peer intentionally sends no welcome, so cancellation must close an in-flight
                // connect / handshake rather than relying on the network's five-second timeout.
                for (int i = 0; i < 60; i++) {
                    Events listener = new Events(); events.add(listener);
                    LanSession client = LanSession.loopbackJoin(room.getLocalPort(), "314159", Runnable::run, listener);
                    clients.add(client); client.close();
                }
                until(() -> clients.stream().allMatch(LanSession::workersStopped));
                for (Events listener : events) {
                    assertEquals("Cancelled join emitted a late failure", 1, listener.failed.getCount());
                    assertEquals("Cancelled join entered gameplay", 1, listener.connected.getCount());
                }
            } finally {
                for (LanSession client : clients) client.close();
                room.close(); peer.join(1000);
                for (Socket socket : accepted) socket.close();
            }
        }
    }
    @Test public void publicHostsDnsAndLoopbackAreUnavailableToProduction() throws Exception {
        assertThrows(IOException.class, () -> LanSession.privateAddress("8.8.8.8", false));
        assertThrows(IOException.class, () -> LanSession.privateAddress("example.com", false));
        assertThrows(IOException.class, () -> LanSession.privateAddress("127.0.0.1", false));
        assertThrows(IOException.class, () -> LanSession.privateAddress("192.168.2.999", false));
        assertTrue(LanSession.privateAddress("192.168.5.1", false).isSiteLocalAddress()); assertTrue(LanSession.privateAddress("10.0.0.4", false).isSiteLocalAddress());
    }
    @Test public void finalSnapshotIsAcknowledgedBeforePeerCloseAndDelayedUiCallbacks() throws Exception {
        Events events = new Events(); BlockingQueue<Runnable> callbacks = new LinkedBlockingQueue<>();
        DuelMatch match = new DuelMatch(4, 0); for (int i = 0; i < 5000; i++) match.step(1, 1, false);
        try (ServerSocket room = new ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))) {
            Thread peer = new Thread(() -> {
                try (Socket socket = room.accept()) {
                    socket.setSoTimeout(3000);
                    LanProtocol.read(socket.getInputStream()); LanProtocol.write(socket.getOutputStream(), LanProtocol.WELCOME, LanProtocol.welcome(4, 0));
                    LanProtocol.write(socket.getOutputStream(), LanProtocol.SNAPSHOT, LanProtocol.snapshot(match.snapshot()));
                    while (LanProtocol.read(socket.getInputStream()).type != LanProtocol.FINISH_ACK) { }
                } catch (IOException error) { throw new RuntimeException(error); }
            }); peer.setDaemon(true); peer.start();
            LanSession guest = LanSession.loopbackJoin(room.getLocalPort(), "314159", callbacks::add, events);
            try {
                until(() -> guest.latest() != null && guest.latest().finished && callbacks.size() >= 2); Runnable callback; while ((callback = callbacks.poll()) != null) callback.run();
                assertNotNull(events.state); assertTrue(events.state.finished); assertEquals(75, events.state.seconds, 0); assertEquals(1, events.failed.getCount());
            } finally { guest.close(); peer.join(2000); }
        }
    }
}
