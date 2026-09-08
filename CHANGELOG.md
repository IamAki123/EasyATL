# Changelog

All notable releases are listed here. Install with JitPack using the **git tag** as the version:

`implementation 'com.github.IamAki123:EasyATL:1.1.2'`

## Unreleased

## 1.1.2

Documentation and practice-tuner workflow. Localization behavior is unchanged from 1.1.1.

- First-time README path and docs under `docs/` (install, sample OpMode, tuning, API, troubleshooting)
- Copy-into-TeamCode `EasyATLConstants` and `EasyATLTuning` (flat D-pad list: up/down move, right select, left back)
- CONTRIBUTING notes for JDK 17+ and Android SDK

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
