# EasyATL API reference

[README](../README.md) · [Install](Install.md) · [What each file does](LibraryFiles.md) · [Sample OpMode](SampleOpMode.md)

**Most FTC teams can skip this page.** SDK / Road Runner: `new FtcEasyATL()` or `DefaultSdkConstants.createLocalizer()` works with no TeamCode constants file. Override later by copying [`EasyATLSdkConstants`](../tuning/sdk/EasyATLSdkConstants.java). Pedro: put camera, tags, and `EasyATL.Config` in [`EasyATLConstants`](../tuning/EasyATLConstants.java). For a short “which class do I use?” overview, see [What each file does](LibraryFiles.md).

| | |
| --- | --- |
| Input (FTC) | `AprilTagDetection` via `FtcEasyATL.localize(...)` |
| Output | Field X, Y (inches), heading (radians) |
| Filtering | Range, bearing, yaw |
| Also | Multi-tag fusion, outlier rejection, smoothing, quality heuristic |

Fluent setters (`addTag`, `setConfig`, `setMaxRangeInches`, `setSmoothingAlpha`, `setCameraConfig`) return `this` so they can be chained. Pipeline thresholds belong on `EasyATL.Config`; `addTag()` is field geometry.

**On this page:** [Config](#config) · [CameraConfig](#cameraconfig) · [Observation](#observation) · [EasyATL](#easyatl) · [FtcEasyATL](#ftceasyatl) · [DefaultSdkConstants](#defaultsdkconstants) · [FieldTags](#fieldtags) · [EasyATLObservations](#easyatlobservations) · [FieldPose](#fieldpose) · [Debug](#debug)

Formulas: [Math](Math.md). Field maps: [AprilTag field sets](FieldTagSets.md).

### `EasyATL.Config`
<a id="config"></a>

Tunable localization pipeline. Construct with `new EasyATL.Config()` and override only what you need. The localizer **copies** the object, so later edits to your `Config` instance do not apply until you pass it again with `setConfig()`.

```java
// Library defaults. The copy-in EasyATLConstants.config() sample uses
// setMaxRangeInches(72) and setSmoothingAlpha(0.70) as a starting point.
EasyATL.Config config = new EasyATL.Config()
        .setMaxRangeInches(96)
        .setMaxBearingDegrees(55)
        .setMaxTagYawDegrees(45)
        .setOutlierDistanceInches(12)
        .setOutlierHeadingDegrees(25)
        .setSmoothingAlpha(0.65)
        .setQualityDecayRate(0.8)
        .setWeightRangeScaleInches(36)
        .setDecisionMarginScale(50);
```

| Method | Default | What it does | Caveats |
| --- | --- | --- | --- |
| `setMaxRangeInches(value)` | 96 | Rejects detections with `range` farther than this. | Must be finite and `> 0`. |
| `setMaxBearingDegrees(value)` | 55 | Rejects detections with \|bearing\| larger than this. | Finite and `≥ 0`. Degrees. |
| `setMaxTagYawDegrees(value)` | 45 | Rejects detections with \|tag yaw\| larger than this. | Finite and `≥ 0`. Degrees. |
| `setOutlierDistanceInches(value)` | 12 | After the robust center is chosen, drops tag estimates farther than this in XY. | Must be `> 0`. Only matters with multiple tags. |
| `setOutlierHeadingDegrees(value)` | 25 | Same as above for heading disagreement. | Must be `> 0`. Degrees. |
| `setSmoothingAlpha(value)` | 0.65 | Blend of new measurement vs previous pose. `1.0` is unsmoothed. | Clamped to `[0, 1]`. First accepted pose is never blended. |
| `setQualityDecayRate(value)` | 0.8 | Exponential quality decay per second while no pose is accepted. | Finite and `≥ 0`. `0` freezes quality. Not a probability model. |
| `setWeightRangeScaleInches(value)` | 36 | Range scale in `1 / (1 + (range/scale)²)`. | Must be `> 0`. Larger keeps far tags heavier. |
| `setDecisionMarginScale(value)` | 50 | Margin that counts as full weight. | Must be `> 0`. Margin `≤ 0` is ignored. |
| `setMinWeight(value)` | 0.05 | Floor on per-tag weight. | Clamped to `[0, 1]`. |
| `setMaxObservationAgeMs(value)` | 0 | Drop frames older than this. | `0` disables. Needs a capture timestamp. |
| `setMaxStepInches(value)` / `setMaxStepDegrees(value)` | 0 | Clamp XY / heading change per accepted frame. | `0` unlimited. First pose is never clamped. |
| `copy()` | — | Returns an independent snapshot. | Getters (`getMaxRangeInches()`, …) read the current values. |

**Returns:** each setter returns `this`.

**Caveats:** `null` config is rejected by `EasyATL` / `FtcEasyATL`. Weight and quality formulas: [Math](Math.md).

### `EasyATL.CameraConfig`
<a id="cameraconfig"></a>

Camera lens pose relative to robot center.

```java
new EasyATL.CameraConfig(double forward, double right, double yawRadians)
new EasyATL.CameraConfig(double forward, double right, double yawRadians, double pitchRadians)
new EasyATL.CameraConfig(double forward, double right, double yawRadians, double pitchRadians, double rollRadians)
```

| Parameter | Meaning |
| --- | --- |
| `forward` | Inches from robot center toward robot forward. Negative is behind center. |
| `right` | Inches from robot center toward robot right. Negative is left of center. |
| `yawRadians` | Camera yaw relative to robot forward, CCW-positive. Negative yaws the camera to the right. |
| `pitchRadians` | Positive tilts the optical axis **up**; negative tilts it **down**. Omitted constructors use `0`. |
| `rollRadians` | Rotation about the optical axis. Omitted constructors use `0`. |

**Returns:** an immutable config. Fields: `forward`, `right`, `yawRadians`, `pitchRadians`, `rollRadians`.

**Caveats:** all values must be finite; `null` is rejected when passed into `EasyATL`. This describes the **lens**, not the camera housing. Leaving pitch at `0` on a tilted camera introduces range error that grows with distance.

### `EasyATL.Observation`
<a id="observation"></a>

One camera-frame tag measurement for the core localizer. FTC teams usually never construct this; `FtcEasyATL` maps `AprilTagDetection.ftcPose` into it.

```java
new EasyATL.Observation(int id, double right, double forward, double range,
        double bearingDegrees, double yawDegrees)
new EasyATL.Observation(int id, double right, double forward, double range,
        double bearingDegrees, double yawDegrees, double z, double decisionMargin, long captureNanoTime)
```

| Parameter | Meaning |
| --- | --- |
| `id` | AprilTag ID. |
| `right` | Tag position in the camera frame, inches to the right of the lens. |
| `forward` | Tag position in the camera frame, inches in front of the lens. |
| `range` | Distance to the tag, inches. |
| `bearingDegrees` | Horizontal bearing of the tag in the camera frame, degrees. |
| `yawDegrees` | Tag yaw relative to the camera, degrees. |
| `z` | Inches above the lens (`ftcPose.z`). Used with camera pitch. Default `0`. |
| `decisionMargin` | FTC detector score. `≤ 0` is ignored. Default `0`. |
| `captureNanoTime` | Capture time, or `0` if unknown. |

**Caveats:** `bearingDegrees` and `yawDegrees` are **degrees**. Field headings elsewhere in the API are **radians**. The constructor does not validate. `localize()` ignores non-finite values and `range <= 0` (they never become a pose).

### `EasyATL`
<a id="easyatl"></a>

Core multi-tag localizer. Independent of the FTC Vision SDK.

#### `EasyATL(CameraConfig camera)` / `EasyATL(CameraConfig camera, Config config)`

Creates a localizer with the given camera mount. The one-argument constructor uses `new Config()` (library defaults).

**Parameters:** `camera` — required lens pose. `config` — optional pipeline thresholds; copied on construction.

**Returns:** a new `EasyATL` with no tags configured.

**Caveats:** `camera` and `config` cannot be `null`. Until `addTag()` is called, `localize()` cannot accept a pose.

#### `addTag(int id, double x, double y, double facingHeadingRadians)` / `addTags(FieldTags fieldTags)`

Registers a field AprilTag, or every tag in a [season map](#fieldtags).

**Parameters:**

| Parameter | Meaning |
| --- | --- |
| `id` | Tag ID. Re-adding the same ID overwrites the previous pose. |
| `x`, `y` | Tag center on the field, inches. |
| `facingHeadingRadians` | Direction **out of the printed face toward the camera**, radians, CCW-positive. |

**Returns:** `this`.

**Caveats:** unconfigured IDs are ignored even if the camera sees them. `x`, `y`, and heading must be finite. Prefer `FtcEasyATL.addCurrentGameTags()` on the robot.

#### `setCameraConfig(CameraConfig value)`

Replaces the camera mount after construction.

**Parameters:** `value` — new lens pose.

**Returns:** `this`.

**Caveats:** `null` throws. `FtcEasyATL` does not expose this method; recreate the adapter if the mount changes. Only available on `EasyATL`.

#### `setConfig(Config value)` / `getConfig()`

Replaces or reads the pipeline config.

**Parameters:** `value` — full `Config`. Copied; later mutations of the passed object are ignored until `setConfig` is called again.

**Returns:** `setConfig` returns `this`. `getConfig()` returns a copy of the active config.

**Caveats:** `null` throws. Prefer this over adding many localizer setters when changing bearing, yaw, outliers, or decay.

#### `setEnabled(boolean)` / `isEnabled()` / `reset()` / `setPose(FieldPose)`

`setEnabled(false)` updates visible-tag telemetry but will not accept a new pose. `reset()` clears pose, quality, and debug (tags stay registered). `setPose` seeds the smoothed pose and sets quality to 1.

#### `setMaxRangeInches(double value)` / `setSmoothingAlpha(double value)`

Convenience writers for the same values on the active `Config`.

**Returns:** `this`.

**Caveats:** same validation as `Config`. Other thresholds have no localizer shortcuts; use `setConfig()`.

#### `localize(List<Observation> observations)`

Fuses configured, in-range, consistent tags from this frame into a field pose.

**Parameters:** `observations` — camera-frame detections. `null` is treated as an empty list.

**Returns:** `true` if a new pose was accepted this call; `false` if nothing usable was fused.

**Caveats:**

- Only tags added with `addTag()` are considered.
- A detection must have finite pose data, `range` in `(0, maxRange]`, `|bearing| ≤ maxBearing`, and `|yaw| ≤ maxTagYaw` (defaults 96 in, 55°, 45°).
- Multiple tags are range/angle weighted (optional decision margin). A robust medoid then drops estimates farther than `outlierDistance` or `outlierHeading` from the consensus (defaults 12 in, 25°).
- `localize(observations, captureNanoTime)` and `localizeFromCameras(...)` are overloads for timestamps and multi-camera frames (`EasyATL.CameraObservations`).
- On failure, the last pose (if any) is **kept**. `getAcceptedTags()` becomes empty. `getQuality()` starts decaying.
- Call once per camera frame, after detections for that frame are ready.

#### `getPose()`

**Returns:** the latest smoothed field pose, or `null` if no pose has ever been accepted.

**Caveats:** this is the last vision estimate, not live odometry. After tags are lost it stays frozen until the next accepted frame.

#### `hasPose()`

**Returns:** `true` after at least one pose has been accepted.

#### `getVisibleTags()`

**Returns:** IDs of configured tags seen in the **latest** `localize()` call, including tags later rejected by range/angle/outlier checks. Unmodifiable.

**Caveats:** unconfigured detections never appear. The list is replaced every `localize()` call.

#### `getAcceptedTags()`

**Returns:** IDs that contributed to the pose on the latest `localize()` call. Empty when that call returned `false`. Unmodifiable.

#### `getQuality()`

Heuristic measurement quality in `[0, 1]`.

**Returns:** a score from inlier count, per-tag weights, and time since the last accepted pose.

**Caveats:** not a probability and not calibrated. While no new pose is accepted it decays as `e^{-qualityDecayRate · t}` (default rate 0.8/sec). Read it after `localize()`.

#### `getConfidence()`

**Deprecated.** Same as `getQuality()`. Planned removal in a future **major** version.

#### `getDebug()` / `getUncertainty()`

See [Debug](#debug).

### `FtcEasyATL`
<a id="ftceasyatl"></a>

FTC adapter around `EasyATL`. Feed `AprilTagProcessor.getDetections()` (or an equivalent `List<AprilTagDetection>`) each loop.

Configuration methods (`addTag`, `setConfig`, `setMaxRangeInches`, `setSmoothingAlpha`, `setCameraConfig`, `setEnabled`, `reset`, `setPose`, `useLatestSeason`, `useSeason`, `useFieldSet`) match `EasyATL` and return `this`.

#### `FtcEasyATL()`

**Returns:** adapter with AAR defaults: lens at robot center, `new EasyATL.Config()`, and `useLatestSeason()`. Same as `DefaultSdkConstants.createLocalizer()`.

#### `FtcEasyATL(EasyATL.CameraConfig camera)` / `FtcEasyATL(camera, Config config)` / `FtcEasyATL(camera, config, FieldTags tags)` / `FtcEasyATL(camera, config, Season season)`

**Parameters:** `camera` — lens pose relative to robot center. `config` — optional; defaults if omitted. Copied.

**Returns:** a new adapter. The camera-only and two-argument constructors have an empty tag map until `useLatestSeason` / `addTag` / `addCurrentGameTags`. Constructors that take `FieldTags` or `Season` register that set immediately. The no-arg constructor already registers latest-season tags.

#### `useLatestSeason()` / `useSeason(Season)` / `useFieldSet(FieldTags)` / `clearTags()`

Replace (or clear) the tag map. See [AprilTag field sets](FieldTagSets.md).

#### `addTag(...)` / `addTags(FieldTags)` / `addCurrentGameTags()` / `addTags(AprilTagLibrary)`

Merge tags. `addCurrentGameTags()` loads `AprilTagGameDatabase.getCurrentGameTagLibrary()` and skips tags with no field position (DECODE obelisk IDs).

#### `setConfig(Config config)` / `getConfig()` / `setMaxRangeInches(double value)` / `setSmoothingAlpha(double value)` / `setCameraConfig(...)`

Same as `EasyATL`.

#### `localize(List<AprilTagDetection> detections)`

Converts FTC detections to observations, then runs `EasyATL.localize()`.

**Parameters:** `detections` from the vision processor. `null` is treated as no detections.

**Returns:** `true` if a new pose was accepted this call.

**Caveats:** detections with `ftcPose == null` are skipped (common when the SDK has an ID but no pose). Mapping is `ftcPose.x → right`, `ftcPose.y → forward`, plus `range`, `bearing`, `yaw`, `z`, `decisionMargin`, and `frameAcquisitionNanoTime`. Call after the webcam/processor has been updated for this loop.

#### `getPose()` / `hasPose()` / `getVisibleTags()` / `getAcceptedTags()` / `getQuality()` / `getConfidence()` / `getDebug()` / `getUncertainty()`

Same meaning, return types, and caveats as `EasyATL`. `getConfidence()` is deprecated.

### `DefaultSdkConstants`
<a id="defaultsdkconstants"></a>

Shipped in the AAR so SDK OpModes compile without a TeamCode constants file.

| Call | Meaning |
| --- | --- |
| `camera()` | Lens at robot center, yaw/pitch `0` |
| `config()` | `new EasyATL.Config()` (library defaults) |
| `WEBCAM_NAME` | `"Webcam 1"` |
| `createProcessor()` | Stock `AprilTagProcessor.Builder().build()` |
| `createPortal(hardwareMap, processor, telemetry)` | Opens that webcam and attaches the processor |
| `createLocalizer()` / `createLocalizer(Config)` | Camera + config + `useLatestSeason()` |
| `customAprilTags()` | Example homemade set; not used unless you `useFieldSet` it |

Copy [`tuning/sdk/EasyATLSdkConstants.java`](../tuning/sdk/EasyATLSdkConstants.java) into TeamCode when you need different lens numbers, webcam name, or tags. When to skip vs copy: [Tuning → AAR defaults](Tuning.md#aar-defaults-no-constants-file).

### `FieldPose`
<a id="fieldpose"></a>

Immutable robot field pose.

```java
new FieldPose(double x, double y, double heading)
```

| Field | Meaning |
| --- | --- |
| `x`, `y` | Robot center on the field, inches. |
| `heading` | Robot heading in radians, CCW-positive, `0` along field `+X`. |
| `headingDegrees()` | Same heading in degrees. |

**Caveats:** `heading` is **radians**, not degrees. Convert with `pose.headingDegrees()` or `Math.toDegrees(pose.heading)` for telemetry. EasyATL does not depend on Pedro or Road Runner; convert at the call site (`new Pose(pose.x, pose.y, pose.heading)` or `new Pose2d(...)`).

### `FieldTags`
<a id="fieldtags"></a>

Named field maps. Full tables and `customAprilTags()`: [AprilTag field sets](FieldTagSets.md).

| Call | Meaning |
| --- | --- |
| `FieldTags.latest()` / `useLatestSeason()` | Newest bundled season (DECODE 2025–26, tags 20 and 24) |
| `FieldTags.season(Season.INTO_THE_DEEP)` / `useSeason(...)` | Previous bundled game |
| `FieldTags.custom("name").add(id, x, y, facing).build()` | Practice field; use with `useFieldSet(...)` |
| `addCurrentGameTags()` | Installed SDK library |

`useLatestSeason` / `useSeason` / `useFieldSet` **replace** the tag map. `addTag` **merges**. `Season.CENTERSTAGE` is not hardcoded — use the SDK or a custom set.

**INTO THE DEEP → DECODE:** `useLatestSeason()` (or pin `Season.DECODE`). Do not fuse obelisk 21–23.

### `EasyATLObservations`
<a id="easyatlobservations"></a>

Helpers for Limelight / custom detectors. No Limelight dependency.

- `cameraFrame(id, right, forward, range, bearingDeg, yawDeg)` — same axes as FTC `ftcPose`
- `fromPolar(id, range, bearingDeg, yawDeg)` — when you have range + `tx`-style bearing

If you already trust Limelight MegaTag `botpose`, do not run it through EasyATL. This is for **per-tag** camera-frame measurements.

### Debug
<a id="debug"></a>

`getDebug()` returns the latest fusion snapshot (`accepted`, `rawMeasurement` before smoothing, `uncertainty`, per-tag `TagDebug` with `weight` and `rejectReason`). `getUncertainty()` is a rough diagonal σ (inches / radians) plus residual — relative, not calibrated. Reject reasons: `range`, `bearing`, `yaw`, `outlier`, `stale`, `disabled`, `non-finite`.

---

[README](../README.md) · [Sample OpMode](SampleOpMode.md) · [Tuning](Tuning.md) · [Math](Math.md) · [Troubleshooting](Troubleshooting.md)

