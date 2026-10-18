# JET / جت — Myket Store build 206

Date: 2026-10-13 (Asia/Tehran). Package `com.neonrush.arcade`, versionName `2.0.0`, versionCode `206`.

## Artifact and original signature

Final APK: `releases/JET-2.0.0-build206-release.apk`; 250,771,595 bytes. SHA-256: `bfa1b3bc80e9c2ba1c5eaca368a13c4b0aa3bc74e5bae03cf7953396dddbd9ed`.

The original 1.0.0 APK and its original private signing configuration were located in the user's previous project output. Its package is `com.neonrush.arcade`, code 1. The authenticated Myket release list independently showed approved, published version 1.0.0 / code 1 and 1021.9 KB. The new version is code 206.

Both APK certificates have SHA-256 `f076d5b239ba759a5aee4cbbf6c83f1c706acb46c436904b2eaadbf9f6abd4c5`. JDK confirmed access to the original key and certificate equality without printing credentials. The new APK's v2 signature verifies, with RSA 3072. Private settings/keys stay local and are excluded from source packaging.

Myket public RSA key is configured and structurally validated. The three existing active SKUs and prices remain unchanged. Product copy now matches permanent cosmetic entitlements; no paid-only trails or backgrounds are promised. Market prices are still loaded by the SDK.

## v1 migration and real upgrade

The original compiled APK and source revealed schema-1 keys in `profile-v1.json` that differed from the new schema. Migration now maps best score, flight statistics, coins, free skin slots/selection, settings, semantic achievement IDs and UTC daily claims. XP is converted to preserve pilot level and relative progress. A legacy cosmetic name never grants a paid slot. Exact original bytes are atomically backed up before saving schema 5; backup failure or a conflicting backup prevents overwriting the v1 file.

Android QA using real AtomicFile checked exact backup bytes, reload without double XP migration, same-day claimed rewards, UTC rollover and clock rollback, nine XP boundaries, absence of paid grants, failed backup/retry and preservation of conflicting backup bytes.

The actual original app was already installed in BlueStacks at version 1.0.0. Store206 was installed over it without uninstalling. Package inspection confirmed code206 with unchanged firstInstallTime `2026-10-04 13:38:37`. Actual before/after screenshots show pilot level3 and balance18 retained; the selected free slot also remains selected though artwork was redesigned. The main menu now shows the separate new Action Flight record, initially0; the old survival record is migrated separately.

Evidence: `docs/previews/android-v1-before206.png`, `android-store206-installed.png`, `android-audit206-final.png`.

## Validation

- 128 JUnit tests: zero failures/errors/skips. Release Lint: no issues. Store assembly, R8 and shrinking passed.
- Native Android QA: 15 PASS, 0 FAIL, 1 NOT TESTED. The added case is original schema-1 migration. Small-window/font1.6 requires API29+ and remains untested on API25.
- Exact APK asset checks passed: 248,750,781 useful asset bytes,16 distinct1080x1920 worlds,12 streamed stereo tracks totaling1366.621 seconds. No padding.
- Earlier build205 six native promotional pictures remain applicable; production rendering/model assets are unchanged by billing setup and migration. Their capture provenance remains documented in RELEASE-GALLERY.md.

## Upload and remaining scope

The Myket form was opened and its latest published version was read. Actual file upload/draft acceptance must be verified in the portal before reporting success. Desktop screenshot capture still reports FrameArrived timeout; indexed click reports unavailable geometry. Keyboard/address-bar navigation partly works. No publication, real purchase, financial transaction or account-permission change has been performed.

Two physical phones, large-font device coverage and end-to-end billing with a Myket tester remain unverified. User requested APK upload; this does not by itself prove a submitted or published release.
