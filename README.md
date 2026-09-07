# EasyATL

`EasyATL` turns FTC AprilTag detections into a robot field pose: **X**, **Y**, and **heading**. Configure the field pose of each AprilTag and the camera lens pose on the robot; then call `localize()` every OpMode loop.

It supports multiple simultaneous tags, rejects inconsistent detections, smooths accepted measurements, and provides a heuristic quality score. The core math is independent of Pedro Pathing and Road Runner. The FTC adapter works directly with `AprilTagDetection` results.

## Coordinate convention

All positions are **inches** and all headings passed to the library are **radians**.

- Robot heading `0` points along field `+X`.
- Positive heading rotates counter-clockwise.
- Camera forward of robot center is positive.
- Camera right of robot center is positive.
- A tag `facingHeading` points **out of the printed tag face toward the camera**.

Use the same field coordinate system that your drive localizer uses. Pedro teams can convert the output directly to `new Pose(x, y, heading)`.

## Install into an FTC SDK project

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

This example assumes you already have an FTC `AprilTagProcessor` or a wrapper whose `getDetectedTags()` returns `List<AprilTagDetection>`. In this project, `AprilTagWebcam` provides that list.

```java
import org.firstinspires.ftc.easyatl.EasyATL;
import org.firstinspires.ftc.easyatl.FieldPose;
import org.firstinspires.ftc.easyatl.FtcEasyATL;
```

Add a field:

```java
private FtcEasyATL localizer;
```

Create it in `init()` after initializing your camera:

```java
localizer = new FtcEasyATL(
        // Camera lens: forward of robot center, right of robot center, yaw CCW from robot forward.
        new EasyATL.CameraConfig(0.75, 0.25, Math.toRadians(-4)))
        // Tag ID, field X, field Y, direction out of the tag face.
        .addTag(21, 8, 8, Math.toRadians(45))
        // Add every tag your robot might use:
        // .addTag(22, 72, 8, Math.toRadians(90))
        // .addTag(23, 136, 72, Math.toRadians(180))
        .setMaxRangeInches(72)
        .setSmoothingAlpha(0.70);
```

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
- Default rejection limits: 96 in range, 55 degrees bearing, and 45 degrees tag yaw. Use `setMaxRangeInches()` to change the range limit.
- Multiple visible tags are range/angle weighted.
- A robust medoid is used before averaging to reject inconsistent tag estimates.
- `setSmoothingAlpha(1.0)` disables smoothing; lower values reduce noise but add lag.
- `getQuality()` is a heuristic score from 0 to 1, not a calibrated probability. `getConfidence()` is a deprecated compatibility alias.

## Troubleshooting

| Symptom | Check |
| --- | --- |
| `easyatl` cannot resolve | Confirm the module is in `settings.gradle`, the TeamCode dependency is present, and Gradle sync completed. |
| Vision pose is mirrored/rotated | Recheck tag facing heading and camera yaw sign. |
| Vision pose is consistently offset | Re-measure the camera lens location from robot center. |
| Pose stops changing when no tag is visible | Expected: vision requires a tag. Let odometry carry the robot until a tag returns. |
| Pose jumps | Lower `setMaxRangeInches()`, improve camera mount values, or reduce smoothing alpha after confirming tag geometry. |
