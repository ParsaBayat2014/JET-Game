# Maintainer case

This document is a factual summary for maintainers and program reviewers. It is intentionally conservative: repository evidence is preferred over projections.

## Project

JET is an open-source Android arcade game written primarily in Java. Version 2.0.0 / build 206 contains GLES2 and Canvas rendering, 48 campaign stages, offline play, local multiplayer, unit tests, Android lint configuration and reproducible soundtrack tooling.

## Maintenance work

The maintainer is responsible for build reproducibility, issue triage, pull-request review, release notes, dependency updates, security reports and device validation. The repository provides a contribution guide, issue forms, pull-request checklist, CODEOWNERS, CI, Dependabot configuration, a security policy and a release process.

## Honest project status

- The project does not claim verified revenue, downloads or active users.
- Real purchases and two-physical-phone multiplayer still need device/service validation.
- Signing keys, store credentials and private configuration are excluded.
- The default branch is the supported development line; `v2.0.0` identifies the build-206 source snapshot.

## Intended use of Codex and API credits

- Review pull requests for correctness, regression risk and test coverage.
- Investigate reproducible bug reports and prepare small, reviewable fixes.
- Improve unit tests around game rules, profile migration, local multiplayer protocol and receipt handling.
- Maintain release notes, build documentation and dependency updates.
- Triage security findings and verify fixes before release.

## Intended security review scope

Security review should focus on local profile integrity, receipt validation boundaries, exported Android components, local-network protocol parsing, dependency/build configuration and accidental credential exposure. The review should preserve the project's documented limitation that this version has no server-authoritative online economy or matchmaking.

## Evidence links

- [Build instructions](../BUILD.md)
- [Security policy](../SECURITY.md)
- [Release process](../RELEASE_PROCESS.md)
- [License inventory](../LICENSES.md)
- [Changelog](../CHANGELOG.md)
