package org.firstinspires.ftc.teamcode;

import com.pedropathing.follower.Follower;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.easyatl.FieldPose;
import org.firstinspires.ftc.easyatl.FtcEasyATL;
import org.firstinspires.ftc.easyatl.PoseCorrector;
import org.firstinspires.ftc.teamcode.easyatl.PedroEasyATLConstants;
import org.firstinspires.ftc.teamcode.easyatl.PedroPoseCorrector;
import org.firstinspires.ftc.vision.VisionPortal;
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection;
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor;

import java.util.List;

/**
 * Pedro drivetrain + AprilTag camera test. Copy into TeamCode with {@code PedroEasyATLConstants}.
 *
 * <p>Fill in {@link PedroEasyATLConstants#createFollower} first. Keep
 * {@code APPLY_VISION_CORRECTION = false} until tape matches vision pose.</p>
 */
@TeleOp(name = "EasyATL Pedro Sample", group = "EasyATL")
public class PedroEasyATLSample extends OpMode {
    private static final boolean APPLY_VISION_CORRECTION = false;

    private Follower follower;
    private PoseCorrector corrector;
    private AprilTagProcessor processor;
    private VisionPortal portal;
    private FtcEasyATL localizer;

    @Override
    public void init() {
        follower = PedroEasyATLConstants.createFollower(hardwareMap);
        corrector = new PedroPoseCorrector(follower);
        processor = PedroEasyATLConstants.createProcessor();
        portal = PedroEasyATLConstants.createPortal(hardwareMap, processor, telemetry);
        localizer = PedroEasyATLConstants.createLocalizer(PedroEasyATLConstants.config());
        telemetry.addLine("Pedro sample: vision telemetry only until APPLY_VISION_CORRECTION is true");
        telemetry.update();
    }

    @Override
    public void init_loop() {
        displayCameraTelemetry();
        telemetry.update();
    }

    @Override
    public void start() {
        follower.startTeleopDrive();
    }

    @Override
    public void loop() {
        follower.update();
        boolean accepted = localizer.localize(processor.getDetections());

        if (APPLY_VISION_CORRECTION && accepted && localizer.getQuality() >= 0.20 && localizer.hasPose()) {
            corrector.apply(localizer.getPose());
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

        follower.setTeleOpDrive(-gamepad1.left_stick_y,
                gamepad1.left_trigger - gamepad1.right_trigger, -gamepad1.right_stick_x, true);

        telemetry.addData("Drive pose", "(%.1f, %.1f, %.1f deg)",
                follower.getPose().getX(), follower.getPose().getY(),
                Math.toDegrees(follower.getPose().getHeading()));
        displayCameraTelemetry();
        telemetry.addData("New vision measurement", accepted);
        telemetry.update();
    }

    private void displayCameraTelemetry() {
        List<AprilTagDetection> detections = processor.getDetections();
        telemetry.addData("Raw tags", detections.size());
        for (AprilTagDetection detection : detections) {
            if (detection.ftcPose == null) {
                telemetry.addData("Tag " + detection.id, "no FTC pose");
            } else {
                telemetry.addData("Tag " + detection.id, "range %.1f  bear %.1f  yaw %.1f",
                        detection.ftcPose.range, detection.ftcPose.bearing, detection.ftcPose.yaw);
            }
        }
    }

    @Override
    public void stop() {
        if (portal != null) portal.close();
    }
}
