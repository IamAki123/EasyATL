# What each library file does

[README](../README.md) · [API](API.md)

These classes are the **EasyATL AAR** (what JitPack puts on your robot). Copy-in tuners under [`tuning/`](../tuning/) are **not** in the library — they are examples you paste into TeamCode.

Almost every FTC team only talks to **`FtcEasyATL`**. If you never copy a Constants file, `DefaultSdkConstants` / `new FtcEasyATL()` still work.

```text
AprilTagProcessor.getDetections()
        │
        ▼
   FtcEasyATL          ←  you call this
        │  converts FTC detections → Observation
        ▼
    EasyATL            ←  filters, fuses, smooths
        │
        ▼
    FieldPose          ←  X, Y, heading

FieldTags              ←  official tag X/Y/facing (optional)
DefaultSdkConstants    ←  AAR camera / webcam / latest-season defaults
PoseCorrector          ←  apply FieldPose to any drivetrain (optional)
EasyATLObservations    ←  Limelight / non-FTC detections (optional)
```

---

## `FtcEasyATL.java`

**The class FTC teams construct.**

It wraps `EasyATL` so you can pass `List<AprilTagDetection>` from `AprilTagProcessor.getDetections()` instead of building camera-frame numbers by hand.

Typical use:

```java
FtcEasyATL localizer = new FtcEasyATL(); // AAR defaults + latest season
boolean accepted = localizer.localize(processor.getDetections());
FieldPose pose = localizer.getPose();
```

Or `DefaultSdkConstants.createLocalizer()` / `createPortal(...)` if you have not copied TeamCode constants.

Custom camera still: `new FtcEasyATL(camera, config).useLatestSeason()`.

Also:

- Maps `ftcPose.x/y` → right/forward, plus range, bearing, yaw, `z`, `decisionMargin`, timestamp
- `addCurrentGameTags()` loads the **installed SDK** field map
- Same `getQuality()`, `getDebug()`, `reset()`, `setPose()`, `setEnabled()` as the core

If `ftcPose` is null, that detection is skipped. EasyATL still does not open the webcam — you must build a `VisionPortal` (or your own helper) first. `DefaultSdkConstants.createPortal(...)` does that with webcam `Webcam 1`.

---

## `DefaultSdkConstants.java`

**Shipped in the AAR.** Camera at robot center, library `Config`, `useLatestSeason()`, webcam name `Webcam 1`.

Use this when you have not copied `EasyATLSdkConstants` into TeamCode:

```java
localizer = DefaultSdkConstants.createLocalizer();
portal = DefaultSdkConstants.createPortal(hardwareMap, processor, telemetry);
```

`new FtcEasyATL()` is the same localizer. Copy [`tuning/sdk/EasyATLSdkConstants.java`](../tuning/sdk/EasyATLSdkConstants.java) only when you need different lens numbers, webcam name, or tags — then point the sample/tuner at that class. When to skip vs copy: [Tuning → AAR defaults](Tuning.md#aar-defaults-no-constants-file).

---

## `EasyATL.java`

**The localization engine.** No FTC Vision types. Independent of Pedro and Road Runner.

`localize(List<Observation>)` does the work described in [the simple walkthrough](MathButDumbed.md): range/bearing/yaw filters, per-tag weights, multi-tag outlier cut, smoothing, quality.

Nested types live in this same file (they are not separate `.java` files):

| Nested type | Role |
| --- | --- |
| `EasyATL.Observation` | One tag in **camera** inches (right, forward, range, bearing°, yaw°) |
| `EasyATL.CameraConfig` | Lens vs robot center (forward, right, yaw, pitch, roll) |
| `EasyATL.Config` | Filter knobs (max range, smoothing, …) |
| `EasyATL.CameraObservations` | One camera + its observations (multi-camera fusion) |
| `EasyATL.DebugFrame` / `TagDebug` / `Uncertainty` | Tuner/telemetry leftovers from the last `localize()` |

Use `EasyATL` directly only if you already have camera-frame measurements (tests, Limelight after conversion). FTC teams should stay on `FtcEasyATL`.

---

## `FieldPose.java`

**The answer:** robot on the field.

Three public fields:

- `x`, `y` — inches
- `heading` — radians, CCW-positive, `0` along field `+X`

`headingDegrees()` is the same heading in degrees for telemetry.

EasyATL does not depend on Pedro or Road Runner. You convert at the call site, for example `new Pose(pose.x, pose.y, pose.heading)` or `new Pose2d(...)`.

`getPose()` returns this type (or `null` until a pose has been accepted).

---

## `FieldTags.java`

**A named set of field AprilTags** (ID, X, Y, facing).

| Call | Meaning |
| --- | --- |
| `FieldTags.latest()` | Newest bundled season (DECODE) |
| `FieldTags.season(Season.INTO_THE_DEEP)` | A previous game |
| `FieldTags.custom("Practice").add(...).build()` | Your own set (`customAprilTags()` in Constants) |

`localizer.useLatestSeason()` / `useSeason(...)` / `useFieldSet(...)` replace the map. Full tables: [AprilTag field sets](FieldTagSets.md).

---

## `EasyATLObservations.java`

**Helpers for detections that are not `AprilTagProcessor`.**

No Limelight (or other) SDK is bundled. You convert *their* camera-frame numbers into an `EasyATL.Observation`, then call `EasyATL.localize(...)`.

- `cameraFrame(...)` — same axes as FTC `ftcPose` (right, forward, range, bearing, yaw)
- `fromPolar(...)` — when you only have range + a `tx`-style bearing angle

If you already trust Limelight MegaTag **botpose** (a finished field pose), you do not need EasyATL fusion. This file is for **per-tag** camera measurements so EasyATL can filter and fuse them.

---

## What is *not* in the AAR

| Location | What it is |
| --- | --- |
| [`tuning/pedro/`](../tuning/pedro/README.md) | Pedro constants, tuner, `PedroPoseCorrector`, sample — copy into TeamCode |
| [`tuning/sdk/`](../tuning/sdk/README.md) | Same knobs with VisionPortal, no Pedro |
| `src/test/...` | Unit tests; not shipped to the robot |

Method-by-method reference: [API](API.md).

---

[README](../README.md) · [How it works](MathButDumbed.md) · [API](API.md)
