# EasyATL: FTC April Tag Localizer

[![Release](https://img.shields.io/github/v/release/IamAki123/EasyATL)](https://github.com/IamAki123/EasyATL/releases/tag/1.1.1)
[![JitPack](https://jitpack.io/v/IamAki123/EasyATL.svg)](https://jitpack.io/#IamAki123/EasyATL)

## Contents

- [About EasyATL](#about-easyatl)
- [Coordinate convention](#coordinate-convention)
- [Install with JitPack](#install-with-jitpack)
- [Install as a local module](#install-as-a-local-module)
- [TeleOp setup](#teleop-setup)
- [Update every loop](#update-every-loop)
- [First test: telemetry only](#first-test-telemetry-only)
- [Correcting Pedro after validation](#correcting-pedro-after-validation)
- [Quality and filtering](#quality-and-filtering)
- [Configuration vs tuning](#configuration-vs-tuning)
  - [Default configuration](#default-configuration)
- [Tuning EasyATL](#tuning-easyatl)
  - [Recommended procedure](#recommended-procedure)
  - [Measuring accuracy](#measuring-accuracy)
- [API reference](#api-reference)
- [Troubleshooting](#troubleshooting)
- [Sample OpMode](#sample-opmode)
- [Changelog](#changelog)
- [Credits](#credits)


## About EasyATL

`EasyATL` turns FTC AprilTag detections into a robot field pose: **X**, **Y**, and **heading**. Configure the field pose of each AprilTag and the camera lens pose on the robot; then call `localize()` every OpMode loop.

It supports multiple simultaneous tags, rejects inconsistent detections, smooths accepted measurements, and provides a heuristic quality score. Detection limits, outlier rejection, smoothing, and quality decay are tunable through `EasyATL.Config`; defaults match the original behavior. The core math is independent of Pedro Pathing and Road Runner. The FTC adapter works directly with `AprilTagDetection` results.

## Coordinate convention

All positions are **inches** and all headings passed to the library are **radians**.

- Robot heading `0` points along field `+X`.
- Positive heading rotates counter-clockwise.
- Camera forward of robot center is positive.
- Camera right of robot center is positive.
- A tag `facingHeading` points **out of the printed tag face toward the camera**.

Use the same field coordinate system that your drive localizer uses. Pedro teams can convert the output directly to `new Pose(x, y, heading)`.

## Install with JitPack

In the FTC project's `build.dependencies.gradle` (or wherever your `repositories { }` block lives), add JitPack:

```gradle
repositories {
    mavenCentral()
    google()
    maven { url = 'https://jitpack.io' }
}
```

Then in `TeamCode/build.gradle`:

```gradle
dependencies {
    implementation project(':FtcRobotController')
    implementation 'com.github.IamAki123:EasyATL:1.1.1'
}
```

Gradle must run on **Java 11 or newer**. In Android Studio, use the Embedded JDK.

## Install as a local module

1. Copy the complete `EasyATL` directory beside `TeamCode`.

   ```text
   YourFtcProject/
   ├── FtcRobotController/
   ├── TeamCode/
   └── EasyATL/
   ```

2. Add the module to the root `settings.gradle`:

   ```gradle
   include ':FtcRobotController'
   include ':TeamCode'
   include ':EasyATL'
   ```

3. Add the dependency in `TeamCode/build.gradle`:

   ```gradle
   dependencies {
       implementation project(':FtcRobotController')
       implementation project(':EasyATL')
   }
   ```

4. In Android Studio, set the Gradle JDK to Java 11 or newer (Embedded JDK is fine), then run **Sync Project with Gradle Files**.

## TeleOp setup

This example assumes you already have an FTC `AprilTagProcessor` or a wrapper whose `getDetectedTags()` returns `List<AprilTagDetection>`. In this project, `AprilTagWebcam` provides that list. A full Pedro TeleOp is in [Sample OpMode](#sample-opmode).

```java
import org.firstinspires.ftc.easyatl.EasyATL;
import org.firstinspires.ftc.easyatl.FieldPose;
import org.firstinspires.ftc.easyatl.FtcEasyATL;
```

Add a field:

```java
private FtcEasyATL localizer;
```

Create it in `init()` after initializing your camera. Omitting `EasyATL.Config` uses the library defaults (same behavior as earlier EasyATL versions):

```java
EasyATL.Config config = new EasyATL.Config()
        // Detection filtering
        .setMaxRangeInches(72)
        .setMaxBearingDegrees(55)
        .setMaxTagYawDegrees(45)
        // Outlier rejection
        .setOutlierDistanceInches(12)
        .setOutlierHeadingDegrees(25)
        // Smoothing
        .setSmoothingAlpha(0.70)
        // Quality
        .setQualityDecayRate(0.8);

localizer = new FtcEasyATL(
        // Camera lens: forward of robot center, right of robot center, yaw CCW from robot forward.
        new EasyATL.CameraConfig(0.75, 0.25, Math.toRadians(-4)),
        config)
        // Tag ID, field X, field Y, direction out of the tag face.
        .addTag(21, 8, 8, Math.toRadians(45));
// Add every tag your robot might use:
// localizer.addTag(22, 72, 8, Math.toRadians(90));
// localizer.addTag(23, 136, 72, Math.toRadians(180));
```

`addTag()` is field setup, not pipeline tuning. You can still call `setMaxRangeInches()` / `setSmoothingAlpha()` on the localizer; they write the same `Config` values.

### Camera configuration

`CameraConfig(forward, right, yaw)` describes the **camera lens**, measured from the robot center:

```java
new EasyATL.CameraConfig(
    0.75,               // 0.75 in forward of robot center
    0.25,               // 0.25 in right of robot center
    Math.toRadians(-4)  // camera points 4 degrees clockwise/right of robot forward
)
```

Examples:

- Camera behind center: use a negative `forward` value.
- Camera left of center: use a negative `right` value.
- Camera points left: use positive yaw.
- Camera points right: use negative yaw.

## Update every loop

Call your webcam update first, then localize using that frame’s detections:

```java
webcam.update();
boolean accepted = localizer.localize(webcam.getDetectedTags());

if (localizer.hasPose()) {
    FieldPose visionPose = localizer.getPose();
    telemetry.addData("Vision pose", "(%.1f, %.1f, %.1f deg)",
            visionPose.x, visionPose.y, Math.toDegrees(visionPose.heading));
    telemetry.addData("Quality", "%.0f%%", 100 * localizer.getQuality());
    telemetry.addData("Visible tags", localizer.getVisibleTags());
    telemetry.addData("Accepted tags", localizer.getAcceptedTags());
    telemetry.addData("New measurement", accepted);
}
```

`accepted` is true only for a new, reliable vision estimate. When tags are lost, the last vision pose remains available and `getQuality()` decays; no new correction is accepted.

## First test: telemetry only

Do **not** set your odometry/localizer pose immediately. Place the robot at several measured field positions, point it at known headings, and compare the printed `Vision pose` to the real robot pose.

Check:

1. Tag ID is configured correctly.
2. Field X/Y and tag facing heading are correct.
3. Camera forward/right offset and yaw match the physical mount.
4. Vision X/Y/heading are sensible at multiple distances and headings.

## Correcting Pedro after validation

Once the measurements have been validated, apply a correction only when a fresh, quality-approved frame is accepted:

```java
boolean accepted = localizer.localize(webcam.getDetectedTags());

if (accepted && localizer.getQuality() >= 0.20) {
    FieldPose visionPose = localizer.getPose();
    follower.setPose(new Pose(
            visionPose.x,
            visionPose.y,
            visionPose.heading
    ));
}
```

When a tag is visible, Pedro is corrected. When the tag leaves camera view, Pedro keeps updating with odometry. When a tag becomes visible again, Pedro receives another correction.

## Quality and filtering

- Only configured tag IDs are used.
- Detection limits, outlier limits, smoothing, and quality decay live on `EasyATL.Config` (see defaults below).
- Multiple visible tags are range/angle weighted (weights are not configurable).
- A robust medoid is used before averaging to reject inconsistent tag estimates.
- `setSmoothingAlpha(1.0)` disables smoothing; lower values reduce noise but add lag.
- `getQuality()` is a heuristic score from 0 to 1, not a calibrated probability. It decays while no new pose is accepted. `getConfidence()` is a deprecated compatibility alias.

## Configuration vs tuning

**Configuration** controls how EasyATL processes detections: which observations are kept, how disagreeing tags are rejected, how poses are smoothed, and how quality decays. You set that with `EasyATL.Config` (or the `setMaxRangeInches` / `setSmoothingAlpha` shortcuts).

**Tuning** means adjusting those parameters for a **specific** camera, robot, and field setup, using measured results—not copying someone else’s numbers.

There is **no universally optimal configuration**. Defaults are a starting point that matches earlier EasyATL behavior. A wide-FOV webcam on a short mount, a Limelight on a tall stack, and a poorly calibrated lens will not want the same limits. Wrong tuning can reject good tags or keep bad ones; only tape-measured error tells you whether a change helped.

### Default configuration

`new EasyATL.Config()` and `new FtcEasyATL(camera)` use:

| Setting | Default |
| --- | ---: |
| Max range | 96 in |
| Max bearing | 55° |
| Max tag yaw | 45° |
| XY outlier limit | 12 in |
| Heading outlier limit | 25° |
| Smoothing alpha | 0.65 |
| Quality decay | 0.8/s |

## Tuning EasyATL

Start with the defaults and change **one parameter at a time**. Fix camera mount and tag field poses first; configuration cannot correct a wrong `CameraConfig` or tag map.

| Parameter | Default | Increase it when... | Decrease it when... |
| --- | --- | --- | --- |
| `maxRangeInches` | 96 | Tags are frequently lost at useful distances | Far-away detections cause noisy estimates |
| `maxBearingDegrees` | 55 | Tags near the edge of the camera are reliable | Edge-of-frame detections are unstable |
| `maxTagYawDegrees` | 45 | Tags viewed at steep angles are still reliable | Angled tags produce bad estimates |
| `outlierDistanceInches` | 12 | Legitimate multi-tag estimates disagree slightly | Occasional bad tags cause XY jumps |
| `outlierHeadingDegrees` | 25 | Multiple estimates differ slightly in heading | Heading occasionally jumps |
| `smoothingAlpha` | 0.65 | You need faster response | Pose is too jittery |
| `qualityDecayRate` | 0.8 | You want stale vision to become untrusted faster | You want quality to persist longer after tags are lost |

Per-tag range/angle weights are not configurable. If a detection is geometrically valid but still noisy, tighten **filtering** (range / bearing / yaw) before touching outlier or smoothing.

### Recommended procedure

1. **Start with defaults.** `new FtcEasyATL(camera)` is enough. Confirm tag IDs, field poses, and camera mount with telemetry only.
2. **Test one tag at different distances.** Place the robot on tape at 24, 36, 48, and 72 in. Note when `accepted` flips to false and when XY error grows.
3. **Test different camera angles.** Yaw the robot so the tag moves from center to the edge of the frame. If edge poses jump, lower `maxBearingDegrees` or `maxTagYawDegrees`.
4. **Test multiple simultaneous tags.** Point at two configured tags. `getAcceptedTags()` should include both when they agree. If one bad tag yanks the pose, lower outlier distance/heading.
5. **Introduce deliberately difficult observations.** Partial occlusion, steep yaw, and long range. Those frames should be rejected (`accepted == false`) rather than fused.
6. **Adjust one parameter at a time.** Re-run the same taped positions after each change.
7. **Record accuracy.** Compare vision pose to a tape-measured robot pose (see below). Keep the config that reduces error without dropping useful frames.

### Measuring accuracy

EasyATL does not ship published field-error numbers: those depend on your camera, calibration, mount, lighting, and tag map. Measure on **your** robot.

Disable drive correction while recording. For each trial, park at a known pose, call `localize()`, and log the difference:

```text
positionError = hypot(vision.x - measured.x, vision.y - measured.y)
headingErrorDeg = abs(toDegrees(wrap(vision.heading - measured.heading)))
```

Average several frames at each station (ignore trials where `accepted` is false). A simple log looks like:

| Setup | Station | Tags | Mean \|XY\| error (in) | Mean heading error (deg) | Accept rate |
| --- | --- | --- | --- | --- | --- |
| Defaults | 36 in, on-axis | 1 | *record* | *record* | *record* |
| Defaults | 72 in, on-axis | 1 | | | |
| Defaults | 36 in, two tags | 2 | | | |
| Tuned (note which knobs) | same stations | | | | |

After tuning, keep the table in team notes. The useful result is not a universal “EasyATL is X inches accurate,” it is **defaults vs your config on the same taped positions**.

## API reference

Most FTC teams should use `FtcEasyATL`. Use `EasyATL` directly only if you already have camera-frame observations and do not want the FTC `AprilTagDetection` adapter.

Fluent setters (`addTag`, `setConfig`, `setMaxRangeInches`, `setSmoothingAlpha`, `setCameraConfig`) return `this` so they can be chained. Pipeline thresholds belong on `EasyATL.Config`; `addTag()` is field geometry.

### `EasyATL.Config`

Tunable localization pipeline. Construct with `new EasyATL.Config()` and override only what you need. The localizer **copies** the object, so later edits to your `Config` instance do not apply until you pass it again with `setConfig()`.

```java
EasyATL.Config config = new EasyATL.Config()
        .setMaxRangeInches(72)
        .setMaxBearingDegrees(55)
        .setMaxTagYawDegrees(45)
        .setOutlierDistanceInches(12)
        .setOutlierHeadingDegrees(25)
        .setSmoothingAlpha(0.70)
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

**Caveats:** `bearingDegrees` and `yawDegrees` are **degrees**. Field headings elsewhere in the API are **radians**. Non-finite values or `range <= 0` are rejected.

### `EasyATL`

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

Immutable robot field pose.

```java
new FieldPose(double x, double y, double heading)
```

| Field | Meaning |
| --- | --- |
| `x`, `y` | Robot center on the field, inches. |
| `heading` | Robot heading in radians, CCW-positive, `0` along field `+X`. |

**Caveats:** `heading` is **radians**, not degrees. Convert with `Math.toDegrees(pose.heading)` for telemetry. Pedro teams can use `new Pose(pose.x, pose.y, pose.heading)` after validating the estimate.

## Troubleshooting

| Symptom | Check |
| --- | --- |
| `easyatl` cannot resolve | Confirm the module is in `settings.gradle`, the TeamCode dependency is present, and Gradle sync completed. |
| Vision pose is mirrored/rotated | Recheck tag facing heading and camera yaw sign. |
| Vision pose is consistently offset | Re-measure the camera lens location from robot center. |
| Pose stops changing when no tag is visible | Expected: vision requires a tag. Let odometry carry the robot until a tag returns. |
| Pose jumps | Confirm tag geometry and camera mount, then tighten range/bearing/yaw or outlier limits, or lower `smoothingAlpha`. |

## Sample OpMode

Copy this into **TeamCode**. It is not part of the EasyATL library build. Replace `AprilTagWebcam`, `Constants.createFollower`, and motor/camera config with your team’s classes.

Leave `APPLY_VISION_CORRECTION` **false** until printed vision pose matches tape measurements. Then set it true so accepted, quality-approved frames correct Pedro.

Controls: left stick Y = forward/back, triggers = strafe, right stick X = turn.

```java
package org.firstinspires.ftc.teamcode;

import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.Pose;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.Mechanisms.AprilTagWebcam;
import org.firstinspires.ftc.teamcode.OFSB1.Constants;
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection;

import org.firstinspires.ftc.easyatl.EasyATL;
import org.firstinspires.ftc.easyatl.FieldPose;
import org.firstinspires.ftc.easyatl.FtcEasyATL;

import java.util.List;

/**
 * Minimal drivetrain + AprilTag camera test.
 *
 * <p>Use this OpMode to validate EasyATL against physical field measurements.</p>
 *
 * <p>Controls: left stick Y = forward/back, triggers = strafe, right stick X = turn.</p>
 */
@TeleOp(name = "EasyATL Sample", group = "ATL Library")
public class EasyATLSample extends OpMode {
    /** False = telemetry only. True = correct Pedro when a new high-quality pose is accepted. */
    private static final boolean APPLY_VISION_CORRECTION = false;

    private Follower follower;
    private AprilTagWebcam webcam;
    private FtcEasyATL localizer;

    @Override
    public void init() {
        follower = Constants.createFollower(hardwareMap);
        follower.setStartingPose(new Pose(0, 0, 0));

        webcam = new AprilTagWebcam();
        webcam.init(hardwareMap, telemetry);

        EasyATL.Config config = new EasyATL.Config()
                // Detection filtering
                .setMaxRangeInches(72)
                .setMaxBearingDegrees(55)
                .setMaxTagYawDegrees(45)
                // Outlier rejection
                .setOutlierDistanceInches(12)
                .setOutlierHeadingDegrees(25)
                // Smoothing
                .setSmoothingAlpha(0.70)
                // Quality
                .setQualityDecayRate(0.8);

        localizer = new FtcEasyATL(
                // Camera lens: forward of robot center, right of robot center, yaw CCW from robot forward.
                new EasyATL.CameraConfig(0.75, 0.25, Math.toRadians(-4)),
                config)
                // Tag ID, field X, field Y, direction out of the tag face.
                .addTag(21, 8, 8, Math.toRadians(45));
        // localizer.addTag(22, 72, 8, Math.toRadians(90));

        telemetry.addLine("Drive + AprilTag camera test ready");
        telemetry.update();
    }

    @Override
    public void init_loop() {
        webcam.update();
        displayCameraTelemetry();
        telemetry.update();
    }

    @Override
    public void start() {
        follower.startTeleopDrive();
    }

    @Override
    public void loop() {
        webcam.update();
        follower.update();

        boolean accepted = localizer.localize(webcam.getDetectedTags());

        if (APPLY_VISION_CORRECTION && accepted && localizer.getQuality() >= 0.20) {
            FieldPose visionPose = localizer.getPose();
            follower.setPose(new Pose(
                    visionPose.x,
                    visionPose.y,
                    visionPose.heading
            ));
        }

        if (localizer.hasPose()) {
            FieldPose visionPose = localizer.getPose();
            telemetry.addData("Vision pose", "(%.1f, %.1f, %.1f deg)",
                    visionPose.x, visionPose.y, Math.toDegrees(visionPose.heading));
            telemetry.addData("Quality", "%.0f%%", 100 * localizer.getQuality());
            telemetry.addData("Visible tags", localizer.getVisibleTags());
            telemetry.addData("Accepted tags", localizer.getAcceptedTags());
        }

        double forward = -gamepad1.left_stick_y;
        double strafe = gamepad1.left_trigger - gamepad1.right_trigger;
        double turn = -gamepad1.right_stick_x;
        follower.setTeleOpDrive(forward, strafe, turn, true);

        Pose odometryPose = follower.getPose();
        telemetry.addData("Drive pose", "(%.1f, %.1f, %.1f deg)", odometryPose.getX(),
                odometryPose.getY(), Math.toDegrees(odometryPose.getHeading()));
        displayCameraTelemetry();
        telemetry.addData("New vision measurement", accepted);
        telemetry.update();
    }

    private void displayCameraTelemetry() {
        List<AprilTagDetection> detections = webcam.getDetectedTags();
        telemetry.addData("Raw tags detected", detections.size());
        for (AprilTagDetection detection : detections) {
            if (detection.ftcPose == null) {
                telemetry.addData("Tag " + detection.id, "no FTC pose");
                continue;
            }
            telemetry.addData("Tag " + detection.id, "range %.1f in, bearing %.1f deg, yaw %.1f deg",
                    detection.ftcPose.range, detection.ftcPose.bearing, detection.ftcPose.yaw);
        }
    }

    @Override
    public void stop() {
        webcam.stop();
    }
}
```

## Changelog

Install the version you want with JitPack, for example `implementation 'com.github.IamAki123:EasyATL:1.1.1'`.

| Version | Type | Notes |
| --- | --- | --- |
| **1.1.1** | Docs | Javadocs on the public API for IDE autocomplete. Localization behavior is unchanged from 1.1.0. |
| **1.1.0** | Feature | `EasyATL.Config` (filtering, outliers, smoothing, quality decay). Tuning guide, defaults table, and sample Pedro TeleOp. Defaults match 1.0.0. |
| **1.0.0** | Release | First public release. Compiles against FTC SDK 11.1.0. |

## Credits

**Akash Vijay Aradhya**
#23918 Super Sigma Robotics

Built with assistance from:
- Cursor AI
- ChatGPT
- OpenAI Codex in Cursor

AI tools were used as development assistance, including generating, debugging, and refining code. The project was directed, reviewed, tested, and understood by the author.
