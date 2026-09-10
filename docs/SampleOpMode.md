# Sample Pedro TeleOp

[README](../README.md) · [Install](Install.md) · [Tuning](Tuning.md) · [SDK sample](SampleOpModeSdk.md)

Pedro Pathing only. Copy [`tuning/pedro/PedroEasyATLConstants.java`](../tuning/pedro/PedroEasyATLConstants.java), [`PedroPoseCorrector.java`](../tuning/pedro/PedroPoseCorrector.java), and [`PedroEasyATLSample.java`](../tuning/pedro/PedroEasyATLSample.java) into TeamCode.

The sample opens `Webcam 1` with a `VisionPortal` and applies vision through `PoseCorrector` / `PedroPoseCorrector` — not a hard-coded `follower.setPose` in the loop.

## Before you paste

1. [Install](Install.md) EasyATL **1.2.1** and sync Gradle.
2. Fill in `PedroEasyATLConstants.createFollower` with **your** Pedro `Constants.createFollower`. Do not import Super Sigma / OFSB1 classes.
3. Tape-measure the **lens** (forward/right/yaw/**pitch**) and tags. Put those numbers in Constants. `createLocalizer` calls `useLatestSeason()`.
4. Keep `APPLY_VISION_CORRECTION = false` until Driver Station vision pose matches tape.

**On the Driver Station:** OpMode name `EasyATL Pedro Sample`. `getQuality() >= 0.20` is a sample gate, not a library default.

### Loop (after `init` built follower, portal, localizer, and `PedroPoseCorrector`)

```java
follower.update();
boolean accepted = localizer.localize(processor.getDetections());

if (APPLY_VISION_CORRECTION && accepted && localizer.getQuality() >= 0.20 && localizer.hasPose()) {
    corrector.apply(localizer.getPose());
}
```

Road Runner / no Pedro: [SDK sample](SampleOpModeSdk.md) (commented `drive.setPoseEstimate`, or your own `PoseCorrector`).

Pose still wrong: [Troubleshooting](Troubleshooting.md). Tape matches: [Tuning](Tuning.md).

---

[README](../README.md) · [Install](Install.md) · [API](API.md) · [Troubleshooting](Troubleshooting.md)
