# Sample Pedro TeleOp

[README](../README.md) · [Install](Install.md) · [Tuning](Tuning.md) · [SDK sample](SampleOpModeSdk.md)

A drivetrain + camera test you copy into **TeamCode**. It does not configure tags or filters; those live in [`EasyATLConstants`](../tuning/EasyATLConstants.java).

## Before you paste this

1. [Install](Install.md) EasyATL **1.2.0** and sync Gradle.
2. Copy `EasyATLConstants.java` into TeamCode. Point `createWebcam` / `createFollower` at **your** classes (the file ships with Super Sigma placeholders).
3. Tape-measure camera lens vs robot center and every tag’s field X/Y/facing. Put those numbers in Constants.
4. Keep `APPLY_VISION_CORRECTION = false` until Driver Station vision pose matches tape.

Replace the `AprilTagWebcam` import if `createWebcam` returns a different type.

**Controls:** left stick Y = forward/back, triggers = strafe, right stick X = turn.

**On the Driver Station:** OpMode name `EasyATL Sample`. In init you should see raw tag telemetry. After Play, compare **Vision pose** to tape. `New vision measurement` is `localize()`’s return value (`true` = a new accepted estimate this loop).

`getQuality() >= 0.20` is a **sample** gate used here and in the tuner, not a library default.

If you are adding EasyATL to an OpMode you already have, you need these **imports**:

```java
import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.Pose;

import org.firstinspires.ftc.easyatl.FieldPose;
import org.firstinspires.ftc.easyatl.FtcEasyATL;
import org.firstinspires.ftc.teamcode.easyatl.EasyATLConstants;
import org.firstinspires.ftc.teamcode.Mechanisms.AprilTagWebcam;
```

and these **fields**:

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

Or paste the full class below. Same pieces are in the [README](../README.md) under **3. Use it in an OpMode**.

## Code

```java
package org.firstinspires.ftc.teamcode;

import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.Pose;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.Mechanisms.AprilTagWebcam;
import org.firstinspires.ftc.teamcode.easyatl.EasyATLConstants;
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection;

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
        follower = EasyATLConstants.createFollower(hardwareMap);
        webcam = EasyATLConstants.createWebcam(hardwareMap, telemetry);
        localizer = EasyATLConstants.createLocalizer(EasyATLConstants.config());

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

Pose still wrong after this runs: [Troubleshooting](Troubleshooting.md). Tape matches and you want filters: [Tuning](Tuning.md).

---

[README](../README.md) · [Install](Install.md) · [API](API.md) · [Troubleshooting](Troubleshooting.md)
