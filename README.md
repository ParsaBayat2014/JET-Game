# JET — Open-source Android arcade game

[![Android CI](https://github.com/ParsaBayat2014/JET-Game/actions/workflows/android.yml/badge.svg)](https://github.com/ParsaBayat2014/JET-Game/actions/workflows/android.yml)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)

JET is a native Android arcade game written primarily in Java, with GLES2/Canvas rendering, campaign stages, local multiplayer and offline play. This repository contains version 2.0.0 (build 206), including source code, build scripts, tests, original game assets and reproducible soundtrack tooling.

The project is maintained by its creator. Bug reports, focused pull requests and code review are welcome. See [CONTRIBUTING.md](CONTRIBUTING.md) before making a change.

## Maintenance commitments

- Keep the default branch buildable with the documented toolchain.
- Run unit tests and Android lint for every proposed change.
- Review issues and pull requests as time permits, with security reports handled privately.
- Publish release notes that distinguish verified behavior from planned or device-dependent work.

See [CHANGELOG.md](CHANGELOG.md) for the version history and [RELEASE_PROCESS.md](RELEASE_PROCESS.md) for the repeatable release checklist.

## Included here

- Native Java game with GLES2/Canvas rendering, 48 campaign stages and local multiplayer.
- Source, unit tests, Android build scripts, world images, effects and license files.
- Complete soundtrack generator and SHA-256 manifest. Generated WAV output is ignored by Git; all 12 tracks were verified against the original build-206 files.
- [Build instructions](BUILD.md) and the [build-206 QA report](docs/QA-STORE206.md).

128 unit tests and debug lint passed during GitHub preparation. No revenue, active-user or installation metrics are claimed. Real purchases and two-physical-phone multiplayer remain unverified. Version-3 functionality is not included.

## Project status

The repository documents the tested build-206 state honestly. Device and service validation is still required for real purchases and two-phone multiplayer. The current in-app name is JET. Google Play billing and publication are not included.

## Credentials and evaluation

Real signing keys, passwords, billing configuration, local SDK settings and personal/device logs are excluded. Configure the provided examples locally. The original signed build-206 APK is held in the local releases folder and is not uploaded as a GitHub release asset. Never commit credentials or personal data.

## License

Original source code is licensed under GNU GPL v3. See [LICENSE](LICENSE). Third-party assets, fonts, music and dependencies retain the licenses stated in their respective files.

Security issues should be reported privately according to [SECURITY.md](SECURITY.md). Community participation follows [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md).
