# EasyATL API reference

[README](../README.md) · [Install](Install.md) · [Sample OpMode](SampleOpMode.md)

**Most FTC teams can skip this page.** Put camera, tags, and `EasyATL.Config` in [`EasyATLConstants`](../tuning/EasyATLConstants.java) and call `createLocalizer(EasyATLConstants.config())` from OpMode `init()`. Use this reference when you construct `FtcEasyATL` / `EasyATL` yourself. Use `EasyATL` directly only if you already have camera-frame observations.

| | |
| --- | --- |
| Input (FTC) | `AprilTagDetection` via `FtcEasyATL.localize(...)` |
| Output | Field X, Y (inches), heading (radians) |
| Filtering | Range, bearing, yaw |
| Also | Multi-tag fusion, outlier rejection, smoothing, quality heuristic |

Fluent setters (`addTag`, `setConfig`, `setMaxRangeInches`, `setSmoothingAlpha`, `setCameraConfig`) return `this` so they can be chained. Pipeline thresholds belong on `EasyATL.Config`; `addTag()` is field geometry.

**On this page:** [Config](#config) · [CameraConfig](#cameraconfig) · [Observation](#observation) · [EasyATL](#easyatl) · [FtcEasyATL](#ftceasyatl) · [FieldPose](#fieldpose)

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
        .setQualityDecayRate(0.8);
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
| `copy()` | — | Returns an independent snapshot. | Getters (`getMaxRangeInches()`, …) read the current values. |

**Returns:** each setter returns `this`.

**Caveats:** range/angle **weights** and the quality formula internals are not configurable. `null` config is rejected by `EasyATL` / `FtcEasyATL`.

### `EasyATL.CameraConfig`
<a id="cameraconfig"></a>

Camera lens pose relative to robot center.

```java
new EasyATL.CameraConfig(double forward, double right, double yawRadians)
```

| Parameter | Meaning |
| --- | --- |
| `forward` | Inches from robot center toward robot forward. Negative is behind center. |
| `right` | Inches from robot center toward robot right. Negative is left of center. |
| `yawRadians` | Camera yaw relative to robot forward, CCW-positive. Negative yaws the camera to the right. |

**Returns:** an immutable config. Fields: `forward`, `right`, `yawRadians`.

**Caveats:** all values must be finite; `null` is rejected when passed into `EasyATL`. This describes the **lens**, not the camera housing.

### `EasyATL.Observation`
<a id="observation"></a>

One camera-frame tag measurement for the core localizer. FTC teams usually never construct this; `FtcEasyATL` maps `AprilTagDetection.ftcPose` into it.

```java
new EasyATL.Observation(int id, double right, double forward, double range,
        double bearingDegrees, double yawDegrees)
```

| Parameter | Meaning |
| --- | --- |
| `id` | AprilTag ID. |
| `right` | Tag position in the camera frame, inches to the right of the lens. |
| `forward` | Tag position in the camera frame, inches in front of the lens. |
| `range` | Distance to the tag, inches. |
| `bearingDegrees` | Horizontal bearing of the tag in the camera frame, degrees. |
| `yawDegrees` | Tag yaw relative to the camera, degrees. |

**Caveats:** `bearingDegrees` and `yawDegrees` are **degrees**. Field headings elsewhere in the API are **radians**. The constructor does not validate. `localize()` ignores non-finite values and `range <= 0` (they never become a pose).

### `EasyATL`
<a id="easyatl"></a>

Core multi-tag localizer. Independent of the FTC Vision SDK.

#### `EasyATL(CameraConfig camera)` / `EasyATL(CameraConfig camera, Config config)`

Creates a localizer with the given camera mount. The one-argument constructor uses `new Config()` (library defaults).

**Parameters:** `camera` — required lens pose. `config` — optional pipeline thresholds; copied on construction.

**Returns:** a new `EasyATL` with no tags configured.

**Caveats:** `camera` and `config` cannot be `null`. Until `addTag()` is called, `localize()` cannot accept a pose.

#### `addTag(int id, double x, double y, double facingHeadingRadians)`

Registers a field AprilTag.

**Parameters:**

| Parameter | Meaning |
| --- | --- |
| `id` | Tag ID. Re-adding the same ID overwrites the previous pose. |
| `x`, `y` | Tag center on the field, inches. |
| `facingHeadingRadians` | Direction **out of the printed face toward the camera**, radians, CCW-positive. |

**Returns:** `this`.

**Caveats:** unconfigured IDs are ignored even if the camera sees them. `x`, `y`, and heading must be finite.

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
- Multiple tags are range/angle weighted. A robust medoid then drops estimates farther than `outlierDistance` or `outlierHeading` from the consensus (defaults 12 in, 25°).
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

**Deprecated.** Same as `getQuality()`.

### `FtcEasyATL`
<a id="ftceasyatl"></a>

FTC adapter around `EasyATL`. Feed `AprilTagProcessor.getDetections()` (or an equivalent `List<AprilTagDetection>`) each loop.

Configuration methods (`addTag`, `setConfig`, `setMaxRangeInches`, `setSmoothingAlpha`) match `EasyATL` and return `this`. There is no `setCameraConfig`; pass the mount into the constructor.

#### `FtcEasyATL(EasyATL.CameraConfig camera)` / `FtcEasyATL(camera, Config config)`

**Parameters:** `camera` — lens pose relative to robot center. `config` — optional; defaults if omitted. Copied.

**Returns:** a new adapter with an empty tag map.

#### `addTag(int id, double x, double y, double facing)`

Same as `EasyATL.addTag(...)`. `facing` is `facingHeadingRadians`.

#### `setConfig(Config config)` / `getConfig()` / `setMaxRangeInches(double value)` / `setSmoothingAlpha(double value)`

Same as `EasyATL`.

#### `localize(List<AprilTagDetection> detections)`

Converts FTC detections to observations, then runs `EasyATL.localize()`.

**Parameters:** `detections` from the vision processor. `null` is treated as no detections.

**Returns:** `true` if a new pose was accepted this call.

**Caveats:** detections with `ftcPose == null` are skipped (common when the SDK has an ID but no pose). Mapping is `ftcPose.x → right`, `ftcPose.y → forward`, plus `range`, `bearing`, and `yaw`. Call after the webcam/processor has been updated for this loop.

#### `getPose()` / `hasPose()` / `getVisibleTags()` / `getAcceptedTags()` / `getQuality()` / `getConfidence()`

Same meaning, return types, and caveats as `EasyATL`. `getConfidence()` is deprecated.

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

**Caveats:** `heading` is **radians**, not degrees. Convert with `Math.toDegrees(pose.heading)` for telemetry. Pedro teams can use `new Pose(pose.x, pose.y, pose.heading)` after validating the estimate.

---

[README](../README.md) · [Sample OpMode](SampleOpMode.md) · [Tuning](Tuning.md) · [Troubleshooting](Troubleshooting.md)

