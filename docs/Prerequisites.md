# Prerequisites

[README](../README.md) · [AprilTag detections](AprilTagDetections.md) · [Install](Install.md)

EasyATL is a **filter on top of detections you already have**. It does not create an FTC project, open a webcam, or replace odometry. Check this list before you copy sample code.

| You need | Why | If you do not have it |
| --- | --- | --- |
| An FTC **Android Studio** project (SDK **11.1.0+**) | EasyATL is a Gradle library you add to TeamCode | [Get an FTC project](#1-ftc-android-studio-project) |
| A **webcam** in the robot configuration | EasyATL never opens the camera | [Configure a webcam](#2-webcam-in-the-configuration) |
| **AprilTag detections** with `ftcPose` | `localize(...)` only runs on that list | **[AprilTag detections](AprilTagDetections.md)** |
| Camera **lens** measurements (tape) | Wrong mount = wrong pose at every station | [README coordinates](../README.md#coordinates-read-once) · [Tuning: geometry](Tuning.md) |
| Tag IDs on a **map** | Unconfigured IDs are ignored | `addCurrentGameTags()` or `addTag(...)` — [README §2](../README.md#configure-the-robot) |
| (Optional) **odometry** | Vision *corrects* your drive pose; it does not replace dead wheels | Keep using Pedro / Road Runner / your localizer |
| (Optional) **Pedro Pathing** | Only the Pedro tuner and Pedro sample need it | Use the [SDK tuner](../tuning/sdk/README.md) instead |

**You are ready for EasyATL when** an OpMode already prints raw tag IDs and `ftcPose.range` on the Driver Station. If that line is still empty, stop and finish [AprilTag detections](AprilTagDetections.md). Filters cannot invent a pose from zero detections.

This documentation matches **1.2.2**. If `PoseCorrector`, `DefaultSdkConstants`, or camera pitch do not exist after sync, you are on an older tag — use `implementation 'com.github.IamAki123:EasyATL:1.2.2'` and sync again.

---

## 1. FTC Android Studio project

EasyATL is not a Blocks project and not a standalone app. You add it to a normal FIRST Tech Challenge Robot Controller project.

1. Install [Android Studio](https://developer.android.com/studio) (use the **Embedded JDK** for Gradle).
2. Follow FIRST’s current [Android Studio / Java](https://ftc-docs.firstinspires.org/en/latest/programming_resources/android_studio_java/Android-Studio-Tutorial.html) setup and clone or unzip the [FtcRobotController](https://github.com/FIRST-Tech-Challenge/FtcRobotController) repo for this season.
3. Confirm you can deploy the stock `ConceptAprilTag` (or any OpMode) to the Robot Controller.

Need SDK **11.1.0+** (`FtcEasyATL` compiles against Vision + RobotCore). Season SDK downloads: [ftc-docs](https://ftc-docs.firstinspires.org/).

Building *this* EasyATL GitHub repo from the command line is different (JDK 17+, Android SDK): [Contributing](../CONTRIBUTING.md).

## 2. Webcam in the configuration

On the Driver Station: **Configure Robot** → your webcam (often named `Webcam 1`).

The name in code must match that config string. The AAR default and SDK sample use `DefaultSdkConstants.WEBCAM_NAME` (`"Webcam 1"`). Hardware config tutorial: [ftc-docs configuring](https://ftc-docs.firstinspires.org/en/latest/hardware_and_software_configuration/configuring/index.html).

USB bandwidth, lighting, and focus matter. EasyATL cannot fix a black preview.

## 3. AprilTag detections

This is the prerequisite teams skip. EasyATL’s only vision input is a `List<AprilTagDetection>` (or camera-frame `Observation`s).

**Full walkthrough (VisionPortal + processor + `getDetections()`, including `ftcPose == null`):** [AprilTag detections](AprilTagDetections.md).

## 4. Official or practice tags

DECODE goal tags for localization are **20** and **24**. Obelisk tags **21–23** move each match — do not use them for pose.

On the robot, prefer:

```java
localizer.useLatestSeason();
```

That loads DECODE (newest bundled table). Older games, SDK metadata, or a practice field: [AprilTag field sets](FieldTagSets.md).

## 5. Odometry (optional, recommended)

EasyATL outputs a field pose when tags are accepted. Between tags, keep wheel / pin-point odometry running. Sample TeleOps leave `APPLY_VISION_CORRECTION = false` until tape matches vision.

## 6. Pedro Pathing (optional)

Copy [`tuning/pedro/PedroEasyATLTuning.java`](../tuning/pedro/PedroEasyATLTuning.java) only if you already use [Pedro Pathing](https://pedropathing.com/) (`SelectableOpMode`, `Follower`). Otherwise copy [`tuning/sdk/`](../tuning/sdk/README.md).

---

**Next:** [Install EasyATL](Install.md) → [README first-time list](../README.md#first-time-here).
