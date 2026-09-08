# <img src="EasyATLLogo.png" alt="EasyATL Logo" width="40" valign="middle"/> EasyATL

# EasyATL

FTC AprilTag localization, simplified.

[![Release](https://img.shields.io/github/v/release/IamAki123/EasyATL)](https://github.com/IamAki123/EasyATL/releases)
[![JitPack](https://jitpack.io/v/IamAki123/EasyATL.svg)](https://jitpack.io/#IamAki123/EasyATL)
[![Tests](https://github.com/IamAki123/EasyATL/actions/workflows/tests.yml/badge.svg)](https://github.com/IamAki123/EasyATL/actions/workflows/tests.yml)
[![License: MIT](https://img.shields.io/github/license/IamAki123/EasyATL)](LICENSE)

## About EasyATL

EasyATL turns FTC AprilTag detections into a filtered, field-relative robot pose. You configure the camera and tags once, tune filters from a Driver Station menu, then reuse that setup in TeleOp and auto.

It does **not** replace odometry. When `localize()` accepts a pose, you *may* correct your drive localizer. Otherwise odometry should keep tracking.

**Current release:** [1.1.2](https://github.com/IamAki123/EasyATL/releases/tag/1.1.2)

## First time here?

Do these in order. Each step has a longer page if you get stuck.

| Step | What you do | Details |
| --- | --- | --- |
| 1 | Add the JitPack dependency and sync Gradle | [Install](docs/Install.md) |
| 2 | Copy `EasyATLConstants` into TeamCode and point it at *your* webcam and Pedro follower | [below](#configure-the-robot) |
| 3 | Measure camera mount and tag field poses with a tape | Filters cannot fix wrong geometry |
| 4 | Run a TeleOp that only *prints* vision pose (`APPLY_VISION_CORRECTION = false`) | [Sample OpMode](docs/SampleOpMode.md) |
| 5 | When tape and vision agree, tune filters, then turn correction on | [Tuning](docs/Tuning.md) |

Pedro Pathing is required only for the copy-in tuner and sample TeleOp. Core `EasyATL` / `FtcEasyATL` have no Pedro dependency.

```text
EasyATLConstants  →  EasyATL Tuning (practice)  →  match TeleOp / auto
     camera, tags,         pick a test,              localize(...)
     Config, webcam        paste snippet
```

## 1. Install

In a stock FTC SDK project, add JitPack next to `mavenCentral()` and `google()` in the **root** `build.dependencies.gradle`:

```gradle
repositories {
    mavenCentral()
    google()
    maven { url = 'https://jitpack.io' }
}
```

Then in `TeamCode/build.gradle`, inside `dependencies`:

```gradle
implementation 'com.github.IamAki123:EasyATL:1.1.2'
```

If Pedro (or other libraries) already live in `build.dependencies.gradle`’s `dependencies` block, put that `implementation` line there instead.

**Sync:** File → Sync Project with Gradle Files. Use Android Studio’s Embedded JDK for the Gradle JVM (File → Settings → Build → Gradle JDK).

Sync errors about repositories, a local source module, or JDK versions: [Install](docs/Install.md).

<a id="configure-the-robot"></a>
## 2. Configure the robot once

Copy [`tuning/EasyATLConstants.java`](tuning/EasyATLConstants.java) into TeamCode (package `org.firstinspires.ftc.teamcode.easyatl` if you also copy the tuner).

There is no separate config file. Camera, tags, pipeline `Config`, webcam, and Pedro follower belong in **that class**. `config()` is a method on it.

The copy-in file imports Super Sigma placeholders (`AprilTagWebcam`, `OFSB1.Constants`). Replace those with **your** webcam helper and Pedro `Constants.createFollower`. Keep drivetrain PID and motor names in your existing Pedro `Constants`.

Then edit the EasyATL values. The `Config` numbers below are a **sample starting point**, not library defaults (`new EasyATL.Config()` uses 96 in range and 0.65 smoothing — [API](docs/API.md)):

```java
// Lens vs robot center (inches; yaw radians CCW from robot forward)
public static final double CAMERA_FORWARD_IN = 0.75;
public static final double CAMERA_RIGHT_IN = 0.25;
public static final double CAMERA_YAW_RAD = Math.toRadians(-4);

public static EasyATL.Config config() {
    return new EasyATL.Config()
            .setMaxRangeInches(72)
            .setMaxBearingDegrees(55)
            .setMaxTagYawDegrees(45)
            .setOutlierDistanceInches(12)
            .setOutlierHeadingDegrees(25)
            .setSmoothingAlpha(0.70)
            .setQualityDecayRate(0.8);
}

public static void addTags(FtcEasyATL localizer) {
    localizer.addTag(21, 8, 8, Math.toRadians(45));
    // localizer.addTag(22, 72, 8, Math.toRadians(90));
}
```

`createLocalizer(config())` builds `FtcEasyATL` from `camera()`, that `Config`, and `addTags()`. EasyATL does not open the webcam; `createWebcam` does.

### Coordinates (read once)

| Quantity | Units / convention |
| --- | --- |
| Field X/Y, camera forward/right, tag X/Y | Inches |
| Robot heading, tag facing, camera yaw | Radians, CCW-positive, `0` along field `+X` |
| Tag facing | Out of the printed face **toward the camera** |
| `CameraConfig` | The **lens**, not the housing. Negative forward = behind center; negative right = left; negative yaw = camera pointed right |
| Detection bearing and tag yaw (`ftcPose`) | Degrees (EasyATL converts as needed) |

Add every tag ID the camera might see. Unconfigured IDs are ignored.

<a id="use-in-opmode"></a>
## 3. Use it in an OpMode

Do not paste camera, tags, or `new EasyATL.Config()` into TeleOp. Call the factories from Constants.

Leave `APPLY_VISION_CORRECTION` **false** until printed vision pose matches tape. `0.20` is a sample quality gate in the sample/tuner, not a library constant. Swap `AprilTagWebcam` if `createWebcam` returns a different type.

**Imports**

```java
import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.Pose;

import org.firstinspires.ftc.easyatl.FieldPose;
import org.firstinspires.ftc.easyatl.FtcEasyATL;
import org.firstinspires.ftc.teamcode.easyatl.EasyATLConstants;
import org.firstinspires.ftc.teamcode.Mechanisms.AprilTagWebcam;
```

**Fields**

```java
private static final boolean APPLY_VISION_CORRECTION = false;

private Follower follower;
private AprilTagWebcam webcam;
private FtcEasyATL localizer;
```

**`init()`**

```java
follower = EasyATLConstants.createFollower(hardwareMap);
webcam = EasyATLConstants.createWebcam(hardwareMap, telemetry);
localizer = EasyATLConstants.createLocalizer(EasyATLConstants.config());
```

**`loop()`** (after you already call `follower.update()`, or include it as shown)

```java
webcam.update();
follower.update();

boolean accepted = localizer.localize(webcam.getDetectedTags());

if (APPLY_VISION_CORRECTION && accepted && localizer.getQuality() >= 0.20) {
    FieldPose visionPose = localizer.getPose();
    follower.setPose(new Pose(visionPose.x, visionPose.y, visionPose.heading));
}
```

`accepted == true` means a new usable estimate this loop, not merely a tag on screen. Detections with `ftcPose == null` are skipped.

Drive, telemetry, and `startTeleopDrive()`: [Sample OpMode](docs/SampleOpMode.md). Constructing `FtcEasyATL` yourself: [API](docs/API.md).

## 4. Tune (practice, not matches)

[`EasyATLTuning`](tuning/EasyATLTuning.java) is for the practice field or pit. Do not select it during a match. Copy it into TeamCode next to Constants. It needs Pedro Pathing (`SelectableOpMode`).

1. Driver Station → **EasyATL Tuning**. **Before Play**, pick a row from the list:
   - **D-pad up / down** — move the highlight
   - **D-pad right** — select that test
   - **D-pad left** — go back
   Start with **Vision telemetry** (prints pose only; does not write Pedro pose).
2. Press **Play**. Drive with the sticks and triggers. On a filter test, D-pad now **changes that one setting** (up/down = small step, left/right = large step). Change one value at a time.
3. Copy the printed snippet into `EasyATLConstants.config()`. Match TeleOp already uses `createLocalizer(EasyATLConstants.config())`.

Tape the camera mount and tag map first.

[Tuning guide](docs/Tuning.md) · [copy checklist](tuning/README.md)

## Docs

| Page | When to open it |
| --- | --- |
| [Docs index](docs/DocsInfo.md) | List of all guide pages |
| [Install](docs/Install.md) | Gradle, JitPack, local module, JDK |
| [Sample OpMode](docs/SampleOpMode.md) | Full TeleOp to copy |
| [Tuning](docs/Tuning.md) | Driver Station menu, knobs, field procedure |
| [API](docs/API.md) | Method-by-method reference |
| [Troubleshooting](docs/Troubleshooting.md) | Sync, mirrored pose, jumps, frozen pose |
| [Changelog](CHANGELOG.md) | What changed between releases |
| [Contributing](CONTRIBUTING.md) | Building this repo from source |

## Credits

Akash Vijay Aradhya — #23918 Super Sigma Robotics

AI tools (Cursor, ChatGPT, OpenAI Codex in Cursor) were used as development assistants for code generation, debugging, documentation, and refinement. Architecture, requirements, testing, validation, and final implementation decisions were directed and reviewed by the author.
