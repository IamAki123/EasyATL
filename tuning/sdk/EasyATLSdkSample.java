package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.easyatl.DefaultSdkConstants;
import org.firstinspires.ftc.easyatl.FieldPose;
import org.firstinspires.ftc.easyatl.FtcEasyATL;
import org.firstinspires.ftc.vision.VisionPortal;
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection;
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor;

/**
 * Minimal AprilTag TeleOp with <strong>no Pedro / Road Runner dependency</strong>.
 *
 * <p>Prints EasyATL pose. Uses {@link DefaultSdkConstants} from the AAR so this file compiles
 * without copying {@code EasyATLSdkConstants}. To override camera/tags, copy that class into
 * TeamCode and switch the three factory calls below to {@code EasyATLSdkConstants}.</p>
 */
@TeleOp(name = "EasyATL SDK Sample", group = "EasyATL")
public class EasyATLSdkSample extends OpMode {
    private static final boolean APPLY_VISION_CORRECTION = false;

    private AprilTagProcessor processor;
    private VisionPortal portal;
    private FtcEasyATL localizer;

    @Override
    public void init() {
        processor = DefaultSdkConstants.createProcessor();
        portal = DefaultSdkConstants.createPortal(hardwareMap, processor, telemetry);
        localizer = DefaultSdkConstants.createLocalizer(DefaultSdkConstants.config());
        telemetry.addLine("SDK sample: vision telemetry only until APPLY_VISION_CORRECTION is true");
        telemetry.update();
    }

    @Override
    public void loop() {
        boolean accepted = localizer.localize(processor.getDetections());

        if (APPLY_VISION_CORRECTION && accepted && localizer.getQuality() >= 0.20 && localizer.hasPose()) {
            FieldPose vision = localizer.getPose();
            // Road Runner: drive.setPoseEstimate(new Pose2d(vision.x, vision.y, vision.heading));
            telemetry.addData("Would correct to", "(%.1f, %.1f, %.1f deg)",
                    vision.x, vision.y, vision.headingDegrees());
        }

        if (localizer.hasPose()) {
            FieldPose vision = localizer.getPose();
            telemetry.addData("Vision pose", "(%.1f, %.1f, %.1f deg)",
                    vision.x, vision.y, vision.headingDegrees());
            telemetry.addData("Quality", "%.0f%%", 100 * localizer.getQuality());
            telemetry.addData("Visible", localizer.getVisibleTags());
            telemetry.addData("Accepted", localizer.getAcceptedTags());
        } else {
            telemetry.addLine("No vision pose yet");
        }

        telemetry.addData("Raw tags", processor.getDetections().size());
        for (AprilTagDetection detection : processor.getDetections()) {
            if (detection.ftcPose == null) {
                telemetry.addData("Tag " + detection.id, "no FTC pose");
            } else {
                telemetry.addData("Tag " + detection.id, "range %.1f  bear %.1f  yaw %.1f",
                        detection.ftcPose.range, detection.ftcPose.bearing, detection.ftcPose.yaw);
            }
        }
        telemetry.addData("New vision measurement", accepted);
        telemetry.update();
    }

    @Override
    public void stop() {
        if (portal != null) portal.close();
    }
}
