# Changelog

All notable releases are listed here. Install with JitPack using the **git tag** as the version:

`implementation 'com.github.IamAki123:EasyATL:1.2.1'`

## Unreleased

## 1.2.1

Compatible with 1.2.0 filter defaults (`new EasyATL.Config()`, `new FtcEasyATL(camera)`).

- `PoseCorrector` in the AAR; Pedro copy-in files live under `tuning/pedro/` (`PedroEasyATLConstants`, no Super Sigma / OFSB1 placeholders)
- `localize()` stages extracted; quality formula in `scoreQuality()` with `setQualityCountBase` / `setQualityCountPerTag` (same 1.2.0 numeric defaults)
- `FieldPose` `equals` / `hashCode` / `toString`; NaN / `range <= 0` observations skipped
- Pedro **Vision telemetry** no longer requires a follower

## 1.2.0

Compatible feature release. `new FtcEasyATL(camera)` and `new EasyATL.Config()` keep 1.1.x filter defaults. Quality for a **single** tag is unchanged; multi-tag quality now includes a residual consistency term.

**High**
- `DefaultSdkConstants` in the AAR and `new FtcEasyATL()` so SDK code runs without a TeamCode constants file
- Pedro-free tuner and VisionPortal sample under `tuning/sdk/`
- `FieldTags.latest()` / `useLatestSeason()`, `useSeason(...)`, `useFieldSet(...)`, and `customAprilTags()` in copy-in Constants ([field sets](docs/FieldTagSets.md))
- `FieldTags.decode()` / `intoTheDeep()` and `FtcEasyATL.addCurrentGameTags()` (SDK metadata)
- Optional camera **pitch** and **roll** on `CameraConfig`

**Medium**
- Configurable weight range scale, decision-margin scale, min weight
- `decisionMargin`, `z`, and capture timestamps mapped from FTC detections
- `getDebug()` / `getUncertainty()`; Huber weights inside the inlier set
- `reset()`, `setPose()`, `setEnabled()`, `setMaxStepInches` / `setMaxStepDegrees`, `setMaxObservationAgeMs`
- Multi-camera `localizeFromCameras`
- `EasyATLObservations` for Limelight-style per-tag camera-frame data (no Limelight dependency)

**Docs / tests**
- [Math](docs/Math.md), why-EasyATL, accuracy guidance, INTO THE DEEP → DECODE note
- Quality-decay clock injection, heading wrap, pitch, stress, and a cheap `localize` timing test
- CI `assembleRelease` in addition to unit tests

`getConfidence()` stays deprecated; removal is reserved for a future **major** version. Maven Central is not published; use JitPack or the [local module](docs/Install.md) path.

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
