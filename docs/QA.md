# JET / جت — build 205 validation

Date: **2026-10-13 (Asia/Tehran)**. Version: **2.0.0-preview**, code **205**. The 2.5D option describes rendering, not version 2.5.

## Build and automated checks

- Final preview artifact: `releases/JET-2.0.0-build205-preview.apk`, **250,769,803 bytes (250.770 decimal MB)**. SHA-256: `b880f02d7d4501ac857b02b0f516b2837aacd67893450de16cd229ea93b0b4f4`. This checksum follows the last Art graphics fix.
- Package `com.neonrush.arcade.preview`; Persian label جت / English JET; minSDK 24 / targetSDK 34. Preview uses a test certificate. The original published signer is necessary for a Myket update.
- **128 JUnit tests: 0 failures, 0 errors, 0 skipped.** Android Lint is clean. Final optimized APK assembly, R8, resource shrinking, v2 signature verification and exact packaged asset checks passed.
- **77 core tests:** all 48 campaign stages remain reachable in standard and gentle difficulty; the six original solo-mode seeded fingerprints are unchanged. Action-party tests cover independent input/fuel, timing, collisions, fairness and reachable team goals.
- **20 network/duel tests** cover protocol/session behavior, connection cleanup and shared simulation rules.
- Additional core pressure harness: **700 runs / 4,857,266 fixed steps**, combining lane input, Slow Motion, repeated Overdrive, jump and duck. Score/time remained finite, speed/fuel stayed bounded, obstacle corridors stayed open and terminal events did not repeat.

## Final audit changes

- Completed/dead runs reject lane changes; lane statistics cannot change after a terminal result.
- Action Flight training no longer starts guardian encounters or their warnings.
- An engine run sequence lets solo settlement reject duplicate results while accepting a later restart of the same engine. Live, tutorial, party and Zen runs cannot award solo economy.
- Failed daily claims and character unlocks restore coins, XP where applicable, claimed/owned flags and newly granted achievements. Storage errors remain visible to the user.
- Extreme/non-finite saved duration, including JSON `1e300` cast to float infinity, is sanitized before future saves.
- Gameplay intensity changes preserve temporary audio ducking. Focus gain/loss and repeated release/late callbacks are handled safely.
- Canvas stays visible while waiting for the first successful GLES frame. Rendering readiness/failure is tied to the current scene; fallback keeps saved renderer preferences.
- Shared touch uses real pointer IDs, keeps each pilot's ownership when fingers cross, rejects a second finger taking an occupied pilot and clears ownership/gestures on cancellation or pause.
- LAN cancellation immediately closes pending connection sockets.
- Solo pause uses compact title/control layout.

**Graphics regression fixed and visually verified:** hardware Canvas discarded some unit-coordinate paths under large scales, leaving faces/glow without hulls, wings or flames. Art now expands paths into pixel coordinates with reusable per-thread Matrix/Path objects and preserves the final transform and stroke. Native before/after captures confirm the complete bodies. All six final gallery images were individually inspected, including ship/cockpit/flames and character silhouettes in images 1, 5 and 6. GLES images 2–4 also rendered correctly. No per-frame geometry allocation was introduced.

## Multiplayer scope

Four shared-device modes remain available: a 75-second score duel or team 80-spark mission, and a 100-second Action duel or team 4-guardian mission. Each action pilot owns independent jump, duck, fuel and Turbo. Both pilots receive the same seed and difficulty. Party collisions cost 100 points without elimination; solo Action Flight retains hearts and elimination.

Detached FlightSceneFrame publication provides one EGL surface/context with independent scissor, depth, projection and camera per track. Narrow portrait viewports adapt FOV consistently with tap hit testing. Both shared-device and local Wi-Fi score matches support actual 3D or Canvas 2.5D.

LAN retains the bounded, host-authoritative 75-second score protocol. Network Action/co-op and Bluetooth are not implemented.

## Actual Android execution

The isolated QA build was installed and launched through BlueStacks' native `installApk` / `launchApp` commands. QA package `com.neonrush.arcade.qa` is separate from the user's preview and never changes the user's coins, purchases or progression. Persistence regression cases use new private QA directories.

Device: **BlueStacks Nougat32, Android 7.1.1 / API 25**, reported model **ONEPLUS A5000**.

The full QA suite was rerun after the final Art fix. Result inspected in [actual Android capture](previews/android-audit205-final.png): **14 PASS, 0 FAIL, 1 NOT TESTED**.

1. Solo 3D first frame, touch, jump, pause/resume: **PASS**.
2. Shared score 3D, actual pointer IDs, two-pointer ownership and crossing fingers: **PASS**.
3. Shared action 3D and independent jump/duck/Turbo: **PASS**.
4. Shared co-op Canvas 2.5D with actual drawn pixels: **PASS**.
5. Shared action Canvas 2.5D with actual drawn pixels: **PASS**.
6. Pause/resume/detach lifecycle and cleared pointer state: **PASS**.
7. Actual Android private IPv4 TCP host/join: **PASS**.
8. Received LAN paired GLES 3D at a 62/38 split: **PASS**.
9. Received LAN Canvas 2.5D, input and pause acknowledgement: **PASS**.
10. Real-time 75-second LAN finish, terminal acknowledgement and worker cleanup: **PASS**.
11. Production SharedDeviceActivity: actual lobby clicks, mounted GL below interactive track, 100-second action HUD, all 12 visible lane/action controls, independent button listeners, touch and pause overlay: **PASS**.
12. Production SharedDeviceActivity: actual 2.5D selector, Action team and guardian target: **PASS**.
13. Child-only 320×480dp window / fontScale 1.6: **NOT TESTED**. The pre-create configuration override needs Android 29+; this emulator is API 25. No global OS/font settings were changed.
14. Actual AtomicFile profile: save/reload, live/duplicate/party/tutorial/Zen settlement rejection, engine restart settlement, single daily claim, failed-save rollback and extreme duration sanitation: **PASS**.
15. Actual Feedback/MediaPlayer initialization, audio focus duck/gain/loss and repeated release/late callbacks: **PASS**. This was silent QA; it did not verify heard audio or measure speaker output volume.

The TCP tests use two real socket peers on **one Android device**, not two physical phones or a real hotspot. Touch tests inject Android MotionEvents; they establish pointer routing, not physical touchscreen responsiveness. Full production two-phone room setup was not automated. The user separately confirmed the corrected 3D appearance in build 203.

## Media, images and source

- 16 original 1080×1920 illustrated worlds, 12 streamed stereo music tracks, 36 character cosmetics, 48 campaign stages, 32 achievements and six action effects remain included. Asset size is about 249 MB, with no padding.
- Six final introduction images are 900×1600 Android captures using actual assets and Canvas/GLES models, fixed curated scenes and dedicated promotional UI. They are not unedited views of every production menu/HUD. [Release gallery](RELEASE-GALLERY.md) records subjects and capture procedure; all six passed final visual review. Delivery: `releases/JET-2.0.0-build205-images.zip`, with six full PNGs, an offline HTML gallery and capture checksums.
- Desktop previews from `tools/preview_flight.py` are Java2D adapters of 2.5D drawing sources with fixture HUDs, not Android/GLES screenshots. `android-*` PNGs are actual emulator captures.
- QA-only activities/application/checks reside in `app/src/qa` and are excluded from preview/release APKs. The source archive includes reproducible QA sources, but excludes keys, credentials, compiled `.class`/`.pyc` files, caches and `app/build` outputs.

## Remaining device/store checks

Two physical phones on Wi-Fi/hotspot, latency/disconnection under device sleep, physical simultaneous touch, frame rate and thermal/battery behavior across GPUs, large-font/small-screen rendering, end-to-end Myket billing and published v1 migration remain unverified.

Myket's public RSA key, three active nonconsumable SKUs, the original published signing key and a confirmed next store versionCode are still required. No store listing or account changes were published. Preview code 205 is not a confirmed next store versionCode.
