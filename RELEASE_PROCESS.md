# Release process

1. Create an issue describing the release scope and any user-visible changes.
2. Update `CHANGELOG.md` with verified changes and known limitations.
3. Run the documented unit tests and Android lint locally or through GitHub Actions.
4. Review the diff for credentials, signing material, private logs and generated files.
5. Build the preview APK and perform device checks where available.
6. Tag the tested commit using a version such as `v2.0.0`.
7. Publish a GitHub release with the changelog and a clear statement of what was and was not device-tested.

The Android CI workflow also builds and uploads the preview APK as a workflow artifact, so reviewers can inspect the exact output of the tested commit without exposing signing credentials.

Production signing keys, billing credentials and private configuration must never be uploaded. A release must not describe an inactive or unverified service as live.
