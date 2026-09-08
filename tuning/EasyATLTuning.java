package org.firstinspires.ftc.teamcode.easyatl;

import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.Pose;
import com.pedropathing.telemetry.SelectableOpMode;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.Gamepad;

import org.firstinspires.ftc.easyatl.EasyATL;
import org.firstinspires.ftc.easyatl.FieldPose;
import org.firstinspires.ftc.easyatl.FtcEasyATL;
import org.firstinspires.ftc.teamcode.Mechanisms.AprilTagWebcam;
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection;

import java.util.List;
import java.util.Locale;

/**
 * Practice tuner (Pedro {@code SelectableOpMode} list in init). Do not use in matches.
 *
 * <p>Copy with {@code EasyATLConstants.java} into TeamCode. Configure tags, camera, webcam, and
 * follower only in {@link EasyATLConstants}.</p>
 *
 * <p>Before Play: D-pad up/down moves the highlight, right selects, left goes back.
 * After Play: drive with sticks/triggers; D-pad changes the live {@link EasyATL.Config} on
 * filter tests. Telemetry shows PASS/FAIL and a snippet to paste into Constants.</p>
 *
 * <p>Not compiled into the EasyATL AAR.</p>
 */
@TeleOp(name = "EasyATL Tuning", group = "EasyATL")
public class EasyATLTuning extends SelectableOpMode {
    static Follower follower;
    static AprilTagWebcam webcam;
    static FtcEasyATL localizer;
    static EasyATL.Config config;

    public EasyATLTuning() {
        super("Select a test", s -> {
            s.add("Vision telemetry", VisionTelemetry::new);
            s.add("Apply vision correction", ApplyCorrection::new);
            s.add("Max range (in)", MaxRangeTuner::new);
            s.add("Max bearing (deg)", MaxBearingTuner::new);
            s.add("Max tag yaw (deg)", MaxTagYawTuner::new);
            s.add("XY outlier (in)", OutlierDistanceTuner::new);
            s.add("Heading outlier (deg)", OutlierHeadingTuner::new);
            s.add("Smoothing alpha", SmoothingAlphaTuner::new);
            s.add("Quality decay rate", QualityDecayTuner::new);
        });
    }

    @Override
    public void onSelect() {
        follower = EasyATLConstants.createFollower(hardwareMap);
        webcam = EasyATLConstants.createWebcam(hardwareMap, telemetry);
        config = EasyATLConstants.config();
        localizer = EasyATLConstants.createLocalizer(config);
    }

    @Override
    public void onLog(List<String> lines) {}

    static void visionTick() {
        webcam.update();
        follower.update();
        localizer.localize(webcam.getDetectedTags());
    }

    static void applyConfig() {
        localizer.setConfig(config);
    }

    static void drive(Gamepad gamepad) {
        follower.setTeleOpDrive(-gamepad.left_stick_y, gamepad.left_trigger - gamepad.right_trigger,
                -gamepad.right_stick_x, true);
    }

    static void printPose(OpMode opMode) {
        if (localizer.hasPose()) {
            FieldPose p = localizer.getPose();
            opMode.telemetry.addData("Vision", "(%.1f, %.1f, %.1f deg)",
                    p.x, p.y, Math.toDegrees(p.heading));
            opMode.telemetry.addData("Quality", "%.0f%%", 100 * localizer.getQuality());
            opMode.telemetry.addData("Visible", localizer.getVisibleTags());
            opMode.telemetry.addData("Accepted", localizer.getAcceptedTags());
        } else {
            opMode.telemetry.addLine("No vision pose yet");
        }
        Pose odo = follower.getPose();
        opMode.telemetry.addData("Drive pose", "(%.1f, %.1f, %.1f deg)",
                odo.getX(), odo.getY(), Math.toDegrees(odo.getHeading()));
    }

    static void printDetections(OpMode opMode) {
        List<AprilTagDetection> detections = webcam.getDetectedTags();
        opMode.telemetry.addData("Raw tags", detections.size());
        for (AprilTagDetection d : detections) {
            if (d.ftcPose == null) {
                opMode.telemetry.addData("Tag " + d.id, "no FTC pose");
                continue;
            }
            opMode.telemetry.addData("Tag " + d.id, "range %.1f  bear %.1f  yaw %.1f",
                    d.ftcPose.range, d.ftcPose.bearing, d.ftcPose.yaw);
        }
    }

    static void printSnippet(OpMode opMode) {
        opMode.telemetry.addLine("--- copy into Config ---");
        opMode.telemetry.addLine(String.format(Locale.US,
                ".setMaxRangeInches(%.0f).setMaxBearingDegrees(%.0f).setMaxTagYawDegrees(%.0f)",
                config.getMaxRangeInches(), config.getMaxBearingDegrees(), config.getMaxTagYawDegrees()));
        opMode.telemetry.addLine(String.format(Locale.US,
                ".setOutlierDistanceInches(%.0f).setOutlierHeadingDegrees(%.0f)",
                config.getOutlierDistanceInches(), config.getOutlierHeadingDegrees()));
        opMode.telemetry.addLine(String.format(Locale.US,
                ".setSmoothingAlpha(%.2f).setQualityDecayRate(%.2f)",
                config.getSmoothingAlpha(), config.getQualityDecayRate()));
    }

    static double bump(Gamepad g, double value, double small, double large, double min, double max) {
        if (g.dpadUpWasPressed()) value += small;
        if (g.dpadDownWasPressed()) value -= small;
        if (g.dpadRightWasPressed()) value += large;
        if (g.dpadLeftWasPressed()) value -= large;
        return Math.max(min, Math.min(max, value));
    }
}

class VisionTelemetry extends OpMode {
    @Override
    public void init() {}

    @Override
    public void start() {
        EasyATLTuning.follower.startTeleopDrive();
    }

    @Override
    public void loop() {
        EasyATLTuning.visionTick();
        EasyATLTuning.drive(gamepad1);
        telemetry.addLine("Telemetry only. Does not set Pedro pose.");
        EasyATLTuning.printPose(this);
        EasyATLTuning.printDetections(this);
        telemetry.update();
    }

    @Override
    public void stop() {
        EasyATLTuning.webcam.stop();
    }
}

class ApplyCorrection extends OpMode {
    @Override
    public void init() {}

    @Override
    public void start() {
        EasyATLTuning.follower.startTeleopDrive();
    }

    @Override
    public void loop() {
        EasyATLTuning.webcam.update();
        EasyATLTuning.follower.update();
        boolean accepted = EasyATLTuning.localizer.localize(EasyATLTuning.webcam.getDetectedTags());
        EasyATLTuning.drive(gamepad1);
        if (accepted && EasyATLTuning.localizer.getQuality() >= 0.20 && EasyATLTuning.localizer.hasPose()) {
            FieldPose v = EasyATLTuning.localizer.getPose();
            EasyATLTuning.follower.setPose(new Pose(v.x, v.y, v.heading));
        }
        telemetry.addLine("Sets Pedro pose when a quality-approved frame is accepted.");
        EasyATLTuning.printPose(this);
        telemetry.update();
    }

    @Override
    public void stop() {
        EasyATLTuning.webcam.stop();
    }
}

class MaxRangeTuner extends OpMode {
    @Override
    public void init() {}

    @Override
    public void start() {
        EasyATLTuning.follower.startTeleopDrive();
    }

    @Override
    public void loop() {
        EasyATLTuning.config.setMaxRangeInches(EasyATLTuning.bump(gamepad1,
                EasyATLTuning.config.getMaxRangeInches(), 1, 6, 1, 200));
        EasyATLTuning.applyConfig();
        EasyATLTuning.visionTick();
        EasyATLTuning.drive(gamepad1);
        telemetry.addLine("D-pad U/D ±1 in, L/R ±6 in. Increase if useful tags are dropped for range.");
        telemetry.addData("maxRangeInches", EasyATLTuning.config.getMaxRangeInches());
        for (AprilTagDetection d : EasyATLTuning.webcam.getDetectedTags()) {
            if (d.ftcPose == null) continue;
            boolean in = d.ftcPose.range > 0 && d.ftcPose.range <= EasyATLTuning.config.getMaxRangeInches();
            telemetry.addData("Tag " + d.id, "range %.1f  %s", d.ftcPose.range, in ? "PASS" : "FAIL range");
        }
        EasyATLTuning.printPose(this);
        EasyATLTuning.printSnippet(this);
        telemetry.update();
    }

    @Override
    public void stop() {
        EasyATLTuning.webcam.stop();
    }
}

class MaxBearingTuner extends OpMode {
    @Override
    public void init() {}

    @Override
    public void start() {
        EasyATLTuning.follower.startTeleopDrive();
    }

    @Override
    public void loop() {
        EasyATLTuning.config.setMaxBearingDegrees(EasyATLTuning.bump(gamepad1,
                EasyATLTuning.config.getMaxBearingDegrees(), 1, 5, 0, 90));
        EasyATLTuning.applyConfig();
        EasyATLTuning.visionTick();
        EasyATLTuning.drive(gamepad1);
        telemetry.addLine("D-pad U/D ±1°, L/R ±5°. Increase if edge-of-frame tags are still stable.");
        telemetry.addData("maxBearingDegrees", EasyATLTuning.config.getMaxBearingDegrees());
        for (AprilTagDetection d : EasyATLTuning.webcam.getDetectedTags()) {
            if (d.ftcPose == null) continue;
            boolean in = Math.abs(d.ftcPose.bearing) <= EasyATLTuning.config.getMaxBearingDegrees();
            telemetry.addData("Tag " + d.id, "bearing %.1f  %s", d.ftcPose.bearing, in ? "PASS" : "FAIL bearing");
        }
        EasyATLTuning.printPose(this);
        EasyATLTuning.printSnippet(this);
        telemetry.update();
    }

    @Override
    public void stop() {
        EasyATLTuning.webcam.stop();
    }
}

class MaxTagYawTuner extends OpMode {
    @Override
    public void init() {}

    @Override
    public void start() {
        EasyATLTuning.follower.startTeleopDrive();
    }

    @Override
    public void loop() {
        EasyATLTuning.config.setMaxTagYawDegrees(EasyATLTuning.bump(gamepad1,
                EasyATLTuning.config.getMaxTagYawDegrees(), 1, 5, 0, 90));
        EasyATLTuning.applyConfig();
        EasyATLTuning.visionTick();
        EasyATLTuning.drive(gamepad1);
        telemetry.addLine("D-pad U/D ±1°, L/R ±5°. Increase if steep yaw still looks accurate.");
        telemetry.addData("maxTagYawDegrees", EasyATLTuning.config.getMaxTagYawDegrees());
        for (AprilTagDetection d : EasyATLTuning.webcam.getDetectedTags()) {
            if (d.ftcPose == null) continue;
            boolean in = Math.abs(d.ftcPose.yaw) <= EasyATLTuning.config.getMaxTagYawDegrees();
            telemetry.addData("Tag " + d.id, "yaw %.1f  %s", d.ftcPose.yaw, in ? "PASS" : "FAIL yaw");
        }
        EasyATLTuning.printPose(this);
        EasyATLTuning.printSnippet(this);
        telemetry.update();
    }

    @Override
    public void stop() {
        EasyATLTuning.webcam.stop();
    }
}

class OutlierDistanceTuner extends OpMode {
    @Override
    public void init() {}

    @Override
    public void start() {
        EasyATLTuning.follower.startTeleopDrive();
    }

    @Override
    public void loop() {
        EasyATLTuning.config.setOutlierDistanceInches(EasyATLTuning.bump(gamepad1,
                EasyATLTuning.config.getOutlierDistanceInches(), 1, 4, 1, 48));
        EasyATLTuning.applyConfig();
        EasyATLTuning.visionTick();
        EasyATLTuning.drive(gamepad1);
        telemetry.addLine("Need 2+ tags. If a good tag is dropped, increase; if pose jumps, decrease.");
        telemetry.addData("outlierDistanceInches", EasyATLTuning.config.getOutlierDistanceInches());
        EasyATLTuning.printPose(this);
        EasyATLTuning.printSnippet(this);
        telemetry.update();
    }

    @Override
    public void stop() {
        EasyATLTuning.webcam.stop();
    }
}

class OutlierHeadingTuner extends OpMode {
    @Override
    public void init() {}

    @Override
    public void start() {
        EasyATLTuning.follower.startTeleopDrive();
    }

    @Override
    public void loop() {
        EasyATLTuning.config.setOutlierHeadingDegrees(EasyATLTuning.bump(gamepad1,
                EasyATLTuning.config.getOutlierHeadingDegrees(), 1, 5, 1, 90));
        EasyATLTuning.applyConfig();
        EasyATLTuning.visionTick();
        EasyATLTuning.drive(gamepad1);
        telemetry.addLine("Need 2+ tags. Heading jumps → decrease. Good tags dropped → increase.");
        telemetry.addData("outlierHeadingDegrees", EasyATLTuning.config.getOutlierHeadingDegrees());
        EasyATLTuning.printPose(this);
        EasyATLTuning.printSnippet(this);
        telemetry.update();
    }

    @Override
    public void stop() {
        EasyATLTuning.webcam.stop();
    }
}

class SmoothingAlphaTuner extends OpMode {
    @Override
    public void init() {}

    @Override
    public void start() {
        EasyATLTuning.follower.startTeleopDrive();
    }

    @Override
    public void loop() {
        EasyATLTuning.config.setSmoothingAlpha(EasyATLTuning.bump(gamepad1,
                EasyATLTuning.config.getSmoothingAlpha(), 0.05, 0.15, 0, 1));
        EasyATLTuning.applyConfig();
        EasyATLTuning.visionTick();
        EasyATLTuning.drive(gamepad1);
        telemetry.addLine("1.0 = no smoothing (jitter). Lower = steadier, more lag.");
        telemetry.addData("smoothingAlpha", "%.2f", EasyATLTuning.config.getSmoothingAlpha());
        EasyATLTuning.printPose(this);
        EasyATLTuning.printSnippet(this);
        telemetry.update();
    }

    @Override
    public void stop() {
        EasyATLTuning.webcam.stop();
    }
}

class QualityDecayTuner extends OpMode {
    @Override
    public void init() {}

    @Override
    public void start() {
        EasyATLTuning.follower.startTeleopDrive();
    }

    @Override
    public void loop() {
        EasyATLTuning.config.setQualityDecayRate(EasyATLTuning.bump(gamepad1,
                EasyATLTuning.config.getQualityDecayRate(), 0.05, 0.2, 0, 4));
        EasyATLTuning.applyConfig();
        EasyATLTuning.visionTick();
        EasyATLTuning.drive(gamepad1);
        telemetry.addLine("Cover the lens: quality should fall. Higher rate = untrusted sooner.");
        telemetry.addData("qualityDecayRate", "%.2f /s", EasyATLTuning.config.getQualityDecayRate());
        telemetry.addData("Quality now", "%.0f%%", 100 * EasyATLTuning.localizer.getQuality());
        EasyATLTuning.printPose(this);
        EasyATLTuning.printSnippet(this);
        telemetry.update();
    }

    @Override
    public void stop() {
        EasyATLTuning.webcam.stop();
    }
}
