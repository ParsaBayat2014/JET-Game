# Historical development README

Historical development notes; current setup is in ../BUILD.md. Some build-205 status statements below have been superseded by QA-STORE206.md.

# JET / ط·آ¬ط·ع¾ أ¢â‚¬â€‌ version 2.0

Native Android arcade game in Persian and English. VersionName remains **2.0.0**; this repository contains the **build 206 source snapshot**. The 2.5D option describes a renderer, not a version number.

## Content

- **100-second Action Flight:** jump orange hurdles, duck purple beams, collect fuel rings and time manual Turbo to smash barriers. Three telegraphed guardian encounters include damage, health and defeat rewards.
- **Actual GLES2 3D:** perspective and following cameras, a depth buffer, lighting/fog, road/tunnel/city geometry and animated ship/animal forms. Solo defaults to 3D; unsupported or failed GL temporarily falls back to the lighter renderer.
- **2.5D Canvas:** depth-sorted extruded hazards, a widening track, banking, jump/duck poses, themed scenery, trails, countdown, particles and short action callouts. Solo, same-phone multiplayer and the local Wi-Fi duel support both views.
- Six original action sounds for jump, duck, Turbo, stunt, guardian entry and guardian defeat; music intensity responds to gameplay.
- 48 campaign stages in 12 free chapters, objectives, one-to-three stars and sequential unlocks. Original first 24 stage IDs and seeds are retained.
- Survival, 90-second Sprint, 120-second Daily Circuit, 75-second Energy Rush, 180-second Zen and campaign.
- **Same-phone multiplayer:** simultaneous independent touch and four modes: a 75-second score duel or team 80-spark mission, and a 100-second Action duel or team 4-guardian mission. Jump, duck, fuel and Turbo are independent for each pilot. Choose a shared world and gentle/standard difficulty; crashes cost 100 points without eliminating a pilot.
- **Two-phone local Wi-Fi/hotspot duel:** 75-second score competition, private IPv4, an ephemeral port, a six-digit room PIN, a bounded TCP protocol and host-authoritative simulation. No external game server or internet data is required. Network Action/co-op and Bluetooth are not implemented.
- Gentle difficulty, six obstacle formations, warning lanes, pooled entities, close calls, charged Overdrive and barrier smashing.
- Shield, Magnet, Slow Motion and Double Score.
- 36 vector characters: 28 earned and eight optional paid cosmetics. All 28 free characters can be tried in party modes.
- 12 earned trails, eight campaign-star badges and 16 original illustrated worlds.
- 12 original 110أ¢â‚¬â€œ120-second stereo compositions, about 22 minutes 47 seconds in total; streamed music room and independent track selection.
- Five daily mission types, 32 achievements, XP, separate solo records and local shared-device match statistics.
- Persian RTL, accessible lane buttons, adaptive controls, scrollable pause, high contrast and reduced motion.
- Myket billing SDK with live prices, signed receipt validation, restore and optional purchase confirmation. Cosmetic packs do not affect performance.

## First clone: prepare generated soundtrack

The 12 original WAV tracks (241 MB) are deliberately not stored in Git. Their complete deterministic generator is included. All 12 regenerated files were verified byte-for-byte against the original build 206. Use Python 3.11:

```powershell
python -m pip install -r tools/requirements-content.txt
python tools/prepare_music.py
```

Android builds also run the checksum-checked preparation step automatically. If needed, pass `-PpythonExecutable=/absolute/path/to/python` to Gradle. Install dependencies first; generated files remain ignored by Git. Gameplay and audio content are unchanged.

## Build

Installed toolchain: JDK 17, Gradle 8.7, Android SDK/build-tools 34 and AGP 8.5.2. Minimum Android 7 / API 24.

Application ID stays `com.neonrush.arcade` for the existing Myket app. Source namespace `com.toosarax.jetjo` is internal and does not set the displayed title.

```powershell
.\tools\build.ps1 -Mode Preview -GoogleMirror -Offline
.\tools\build.ps1 -Mode Check -GoogleMirror -Offline
# After configuring the public billing key, original signing certificate and next store versionCode:
.\tools\build.ps1 -Mode Store -ReleaseVersionCode 205 -GoogleMirror
```

**205 is only an example store versionCode; it has not been confirmed against the published app.** Preview uses `com.neonrush.arcade.preview` and a test certificate. It installs alongside the published app and is not a Myket update.

For another machine, set JAVA_HOME and local.properties. The optional Google mirror is scoped to AndroidX/Google artifacts.

## Architecture and settlement

`core` contains Android-free seeded simulation, stages, progression and shared-device duel rules. A monotonically increasing engine run sequence identifies each start; terminal input cannot alter a completed run, and training does not start guardian encounters.

`multiplayer` contains bounded framed TCP, protocol/PIN validation, host simulation and connection cleanup. Socket I/O and simulation run outside the UI thread; snapshots are 20 Hz. Cancellation closes outstanding connection sockets immediately.

`data` contains catalogs and a versioned AtomicFile profile. Schema 5 retains old indices and adds independent Action Flight records/render preferences; purchase receipts remain separate. Solo results settle once per completed engine run. Live, training, party and Zen runs cannot grant solo economy. Daily claims and character unlocks roll back their state if saving fails. Published v1 save compatibility still needs its original APK/source.

`game` owns fixed-step simulation, Canvas perspective and GLES2 meshes. Detached FlightSnapshot publication prevents GL access to live engine/profile state. FlightSceneFrame publishes paired pilots coherently; one EGL context renders separate scissored viewports with independent depth and cameras. Narrow portrait viewports use a matching adaptive FOV. FlightCamera shares projection constants with tap hit testing; FlightPerspective supplies the 2.5D collision-plane projection. Asynchronous image decode retains a bounded bitmap cache. Canvas remains visible until the first successful GPU frame. Pointer ownership uses actual Android IDs and clears on cancellation or pause.

`audio` streams one soundtrack with MediaPlayer and pools short effects. Temporary audio-focus ducking remains active when gameplay intensity changes; released audio objects ignore later callbacks. The media pack is never loaded into the heap as one byte array.

Activities provide solo navigation, a two-player lobby, same-phone race/team results and local network rooms. Solo pause uses compact controls that fit below its title.

## Controls and validation

Action Flight is the primary menu button. Tap/swipe left or right to choose a lane. Swipe up or press Jump for orange hurdles; swipe down or press Duck for overhead purple beams. Turbo requires 50% fuel. Gold rings refill 23%; energy and action clears also refill fuel. During guardians, action gates and pickups deal damage. An empty lane provides a safe alternate route. Action records and defeated guardian counts persist separately.

Select 3D/2.5D in the menu/settings or the multiplayer lobbies. Reduced motion disables decorative camera sway, flashes/streaks and particles while preserving action poses. The user confirmed the corrected 3D appearance in build 203. Build 205 adds the final audit fixes to the paired-rendering and multiplayer changes from build 204.

Build 205 currently has **128 passing JUnit tests**, clean Android Lint, and **14 passing actual Android QA cases** on BlueStacks API 25. The small-window/font-1.6 case remains untested because its isolated configuration override needs API 29+. See [QA](docs/QA.md) for precise coverage, verified character rendering and remaining physical-device/store checks.

## Images, assets and dependencies

Six 900ط£â€”1600 introduction images use the actual project assets and Android Canvas/GLES renderers with curated scenes and a dedicated promotional layout. They do not reproduce every production menu or HUD. See [release gallery](docs/RELEASE-GALLERY.md) for scene provenance and reproduction. All six final images were individually inspected; character bodies and flames are fully visible after the hardware Canvas fix. The PNGs and offline gallery are in `releases/JET-2.0.0-build205-images.zip`.

Generate desktop 2.5D design previews with `python tools/preview_flight.py`. It compiles the drawing sources through a Java2D Canvas adapter; HUD/control layouts are fixtures. Those previews are not Android screenshots or GLES output.

Runtime dependency: official `com.github.myketstore:myket-billing-client:1.19` and its Material/AndroidX/Play Services dependencies. Tests use JUnit 4.13.2. Asset generation uses Python, Pillow and NumPy only during generation.

Original vector art and the generated background/music pack include reproducible scripts and a CC0 declaration. Vazirmatn includes its SIL OFL license. The approximately 251 MB APK contains playable/renderable media without padding. WAVs remain uncompressed for streamed AssetFileDescriptor playback.

Solo and same-phone gameplay work offline. Wi-Fi play uses only a private paired phone; billing/support links need Myket/browser internet. There are no ads, game analytics, location, contacts or storage access. INTERNET permission supports local TCP. The optional SDK installer is disabled.

See [local multiplayer](docs/LOCAL-MULTIPLAYER.fa.md), [Myket listing](docs/MYKET-LISTING.fa.md), [Myket setup](docs/MYKET-SETUP.fa.md) and [QA](docs/QA.md).
