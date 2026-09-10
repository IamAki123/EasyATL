# Sample SDK TeleOp (no Pedro)

[README](../README.md) · [Pedro sample](SampleOpMode.md) · [Tuning](Tuning.md)

Use this if you drive with **Road Runner**, a custom localizer, or anything that is not Pedro Pathing. Copy [`tuning/sdk/EasyATLSdkSample.java`](../tuning/sdk/EasyATLSdkSample.java) into TeamCode. It uses `DefaultSdkConstants` from the AAR, so it compiles without a constants file.

To override camera, webcam name, or tags, also copy [`tuning/sdk/EasyATLSdkConstants.java`](../tuning/sdk/EasyATLSdkConstants.java) into `org.firstinspires.ftc.teamcode.easyatl`, then in this sample’s `init()` replace the three `DefaultSdkConstants` calls (and the import) with `EasyATLSdkConstants`. Exact lines: [Tuning → Where to switch](Tuning.md#where-to-switch-to-easyatlsdkconstants).

The sample opens `Webcam 1` with the FTC `VisionPortal` / `AprilTagProcessor` APIs. It does **not** set a drivetrain pose until you uncomment the correction line.

## Before you paste

1. [Install](Install.md) EasyATL and sync Gradle.
2. Defaults: lens at robot center, library `Config`, latest season, webcam `Webcam 1`. To override, set `WEBCAM_NAME` and lens forward/right/yaw/**pitch** in `EasyATLSdkConstants`. Pitch `0` is level; negative is tilted down. Measure the **lens**, not the housing.
3. `createLocalizer` calls `useLatestSeason()` (DECODE). Past seasons or homemade tags: [AprilTag field sets](FieldTagSets.md).
4. Keep `APPLY_VISION_CORRECTION = false` until Driver Station vision pose matches tape.

**On the Driver Station:** OpMode name `EasyATL SDK Sample`. Compare **Vision pose** to tape. `New vision measurement` is `localize()`’s return value.

`getQuality() >= 0.20` is a sample gate, not a library default.

### Loop (after `init` built processor + localizer)

```java
boolean accepted = localizer.localize(processor.getDetections());

if (APPLY_VISION_CORRECTION && accepted && localizer.getQuality() >= 0.20) {
    FieldPose vision = localizer.getPose();
    // Road Runner example:
    // drive.setPoseEstimate(new Pose2d(vision.x, vision.y, vision.heading));
}
```

Full class: [`tuning/sdk/EasyATLSdkSample.java`](../tuning/sdk/EasyATLSdkSample.java). Tuner (D-pad, no Pedro): [`tuning/sdk/EasyATLSdkTuner.java`](../tuning/sdk/EasyATLSdkTuner.java).

Pose still wrong: [Troubleshooting](Troubleshooting.md). Tape matches: [Tuning](Tuning.md).

---

[README](../README.md) · [API](API.md) · [Math](Math.md)
