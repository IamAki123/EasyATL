# Changelog

All notable releases are listed here. Install with JitPack using the **git tag** as the version:

`implementation 'com.github.IamAki123:EasyATL:1.1.1'`

## Unreleased

- Unit tests for `EasyATL` (known-pose round-trips, filters, outliers, smoothing, quality decay)
- Unit tests for `FtcEasyATL` pose mapping
- GitHub Actions (`./gradlew test` on push and pull request)
- CONTRIBUTING, issue/PR templates, SECURITY.md

## 1.1.1

Documentation patch. Localization behavior is unchanged from 1.1.0.

- Javadocs on the public API for IDE autocomplete

## 1.1.0

Compatible feature release. `new FtcEasyATL(camera)` still uses the same defaults as 1.0.0.

- `EasyATL.Config` for detection filtering, outlier rejection, smoothing, and quality decay
- Tuning guide, defaults table, and sample Pedro TeleOp in the README

## 1.0.0

- First public release
- Compiles against FTC SDK 11.1.0
