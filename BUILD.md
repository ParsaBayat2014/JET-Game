# Build JET version 2

## Requirements

- JDK 17, Android SDK platform/build-tools 34.
- Gradle wrapper 8.7 (included), Android Gradle Plugin 8.5.2.
- Python 3.11 plus the pinned content-generation packages.
- Network access for the first dependency installation/build. Offline mode works only after dependencies are cached.

## First setup

Set JAVA_HOME to your JDK. Copy local.properties.example to local.properties and set sdk.dir to your Android SDK path.

```sh
python -m pip install -r tools/requirements-content.txt
python tools/prepare_music.py
```

The second command generates missing soundtrack files and validates all 12 against the canonical hash manifest for the current operating system. Small floating-point differences between Windows and Linux are tracked in separate manifests; this keeps verification strict without treating a cross-platform build as an unverified success. Android preBuild also calls this step; override the Python executable with -PpythonExecutable=/absolute/path/to/python if needed.

## Build an evaluation APK

Windows:

```powershell
.\gradlew.bat :app:assemblePreview
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug
```

macOS/Linux:

```sh
./gradlew :app:assemblePreview
./gradlew :app:testDebugUnitTest :app:lintDebug
```

The APK is app/build/outputs/apk/preview/app-preview.apk. It has a separate .preview application ID and a test signing certificate; it does not update the Myket store app or enable real store purchases. The intended default version code in this snapshot is 206.

## Production builds

Set up private signing/release.properties and myket.properties from the examples. Do not commit real credentials. Do not use a replacement certificate to update the existing app. After checking the latest store version, use a strictly higher version code:

```powershell
.\gradlew.bat :app:storeRelease -PreleaseVersionCode=YOUR_NEXT_VERSION_CODE
```

Replace YOUR_NEXT_VERSION_CODE with a verified integer before running. A foreign-store launch needs its own billing, store metadata and platform compliance work. Some older helper scripts and historical documents still use build-205 output filenames; the wrapper commands above avoid relying on those names.

The existing local signed APK, original project archive and exact build-206 report are the reference delivery artifacts. This setup has been checked through compilation, unit tests, debug lint and exact soundtrack regeneration; it is not a new full device certification.
