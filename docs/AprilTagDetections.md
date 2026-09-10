# Get AprilTag detections

[README](../README.md) · [Prerequisites](Prerequisites.md) · [SDK sample](SampleOpModeSdk.md)

EasyATL does **not** open the camera. `FtcEasyATL.localize(...)` only filters a list you already built. If Driver Station **Raw tags** is `0`, this page is the fix — not range filters or smoothing.

Official FIRST overview: [AprilTag Introduction](https://ftc-docs.firstinspires.org/en/latest/apriltag/vision_portal/apriltag_intro/apriltag-intro.html).

## You are done when

On the Driver Station, after Play, you see something like:

```text
Raw tags    1
Tag 20      range 36.2 in, bearing 2.1 deg, yaw -1.0 deg
```

Then you can pass `processor.getDetections()` into EasyATL. If you only see an ID and **no FTC pose**, skip to [ftcPose is null](#ftcpose-is-null).

## 1. Hardware

- USB webcam on the Control Hub or a supported phone camera (webcam is typical).
- Robot configuration includes that camera. Remember the **exact name** (example: `Webcam 1`).
- Printed AprilTags at the **correct size** for this season (DECODE goals are larger than older 4 in tags). Crooked, shiny, or undersized prints break pose.

## 2. Minimal OpMode (no EasyATL yet)

Use the FTC Vision SDK only. Copy this into TeamCode, set the webcam name, deploy, and **Play**. Do not add EasyATL until this prints a range.

```java
package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.vision.VisionPortal;
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection;
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor;

@TeleOp(name = "AprilTag detections test", group = "EasyATL")
public class AprilTagDetectionsTest extends OpMode {
    private AprilTagProcessor processor;
    private VisionPortal portal;

    @Override
    public void init() {
        processor = new AprilTagProcessor.Builder().build();
        portal = new VisionPortal.Builder()
                .setCamera(hardwareMap.get(WebcamName.class, "Webcam 1"))
                .addProcessor(processor)
                .build();
    }

    @Override
    public void loop() {
        telemetry.addData("Raw tags", processor.getDetections().size());
        for (AprilTagDetection d : processor.getDetections()) {
            if (d.ftcPose == null) {
                telemetry.addData("Tag " + d.id, "no FTC pose");
            } else {
                telemetry.addData("Tag " + d.id, "range %.1f in  bear %.1f  yaw %.1f",
                        d.ftcPose.range, d.ftcPose.bearing, d.ftcPose.yaw);
            }
        }
        telemetry.update();
    }

    @Override
    public void stop() {
        if (portal != null) portal.close();
    }
}
```

The stock SDK sample **ConceptAprilTag** does the same job if you prefer FIRST’s file.

[`EasyATLSdkSample`](../tuning/sdk/EasyATLSdkSample.java) already contains this VisionPortal setup plus EasyATL. Use it once detections work.

## 3. If Raw tags = 0

| Check | What to try |
| --- | --- |
| Preview is black | USB cable, Hub port, camera name in config vs `"Webcam 1"` |
| Tag not in view / too far / too dark | Lights, 2–6 ft, tag filling a decent part of the image |
| Wrong family or size | This season’s 36h11 tags at the official print size |
| Processor not on the portal | `.addProcessor(processor)` before `.build()` |
| Portal never built | `init()` must run; look for exceptions in logcat |

FIRST: [AprilTag troubleshooting](https://ftc-docs.firstinspires.org/en/latest/apriltag/vision_portal/apriltag_intro/apriltag-intro.html) and camera calibration pages if pose exists but is wildly wrong.

## 4. `ftcPose` is null

The SDK saw an ID but did **not** compute a 3D pose. EasyATL **skips** those rows.

Common causes:

- Tag ID is **not** in the AprilTag library (custom print, or processor built without the current-game library).
- Default `new AprilTagProcessor.Builder().build()` already uses `AprilTagGameDatabase.getCurrentGameTagLibrary()`. If you passed a custom library, include tag **size** metadata.
- Some frames report an ID before pose is ready — wait a loop; if it stays null, it is a library/size problem.

DECODE: IDs **20** and **24** (goals) are the localization tags. **21–23** (obelisk) are for motif, not field pose.

## 5. Plug detections into EasyATL

After the test OpMode shows range/bearing/yaw:

```java
boolean accepted = localizer.localize(processor.getDetections());
```

`accepted == true` means EasyATL fused a usable pose **this loop**, not merely that a tag is on screen. Next: [SDK sample](SampleOpModeSdk.md) or [README §3](../README.md#use-it-in-an-opmode).

Limelight / other processors: convert each tag to camera-frame inches (same axes as `ftcPose`) with [`EasyATLObservations`](API.md#easyatlobservations). Do not pass MegaTag field `botpose` in as if it were `ftcPose`.

---

[Prerequisites](Prerequisites.md) · [Troubleshooting](Troubleshooting.md) · [Install](Install.md)
