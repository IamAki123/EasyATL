package org.firstinspires.ftc.teamcode.easyatl;

import com.pedropathing.follower.Follower;
import com.pedropathing.telemetry.SelectableOpMode;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.Gamepad;

import org.firstinspires.ftc.easyatl.EasyATL;
import org.firstinspires.ftc.easyatl.FieldPose;
import org.firstinspires.ftc.easyatl.FtcEasyATL;
import org.firstinspires.ftc.easyatl.PoseCorrector;
import org.firstinspires.ftc.vision.VisionPortal;
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection;
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor;

import java.util.List;
import java.util.Locale;

/**
 * Pedro practice tuner ({@code SelectableOpMode} list in init). Do not use in matches.
 *
 * <p>Copy with {@code PedroEasyATLConstants} into TeamCode. <strong>Vision telemetry</strong> does
 * not create a Pedro follower — fill in {@code createFollower} only when you pick a driving test
 * or Apply vision correction.</p>
 *
 * <p>Not compiled into the EasyATL AAR.</p>
 */
@TeleOp(name = "EasyATL Tuning", group = "EasyATL")
public class PedroEasyATLTuning extends SelectableOpMode {
    static Follower follower;
    static PoseCorrector corrector;
    static AprilTagProcessor processor;
    static VisionPortal portal;
    static FtcEasyATL localizer;
    static EasyATL.Config config;

    public PedroEasyATLTuning() {
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
        processor = PedroEasyATLConstants.createProcessor();
        portal = PedroEasyATLConstants.createPortal(hardwareMap, processor, telemetry);
        config = PedroEasyATLConstants.config();
        localizer = PedroEasyATLConstants.createLocalizer(config);
        follower = null;
        corrector = null;
    }

    @Override
    public void onLog(List<String> lines) {}

    /** Vision only — no follower, no driving. */
    static void visionTick() {
        localizer.localize(processor.getDetections());
    }

    static void applyConfig() {
        localizer.setConfig(config);
    }

    static void ensureDrive(OpMode opMode) {
        if (follower != null) return;
        follower = PedroEasyATLConstants.createFollower(opMode.hardwareMap);
        corrector = new PedroPoseCorrector(follower);
        follower.startTeleopDrive();
    }

    static void driveTick(OpMode opMode) {
        ensureDrive(opMode);
        follower.update();
        Gamepad gamepad = opMode.gamepad1;
        follower.setTeleOpDrive(-gamepad.left_stick_y, gamepad.left_trigger - gamepad.right_trigger,
                -gamepad.right_stick_x, true);
    }

    static void printPose(OpMode opMode) {
        if (localizer.hasPose()) {
            FieldPose p = localizer.getPose();
            opMode.telemetry.addData("Vision", "(%.1f, %.1f, %.1f deg)",
                    p.x, p.y, p.headingDegrees());
            opMode.telemetry.addData("Quality", "%.0f%%", 100 * localizer.getQuality());
            opMode.telemetry.addData("Visible", localizer.getVisibleTags());
            opMode.telemetry.addData("Accepted", localizer.getAcceptedTags());
        } else {
            opMode.telemetry.addLine("No vision pose yet");
        }
        if (follower != null) {
            opMode.telemetry.addData("Drive pose", "(%.1f, %.1f, %.1f deg)",
                    follower.getPose().getX(), follower.getPose().getY(),
                    Math.toDegrees(follower.getPose().getHeading()));
        }
    }

    static void printDetections(OpMode opMode) {
        List<AprilTagDetection> detections = processor.getDetections();
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

    static void printDebug(OpMode opMode) {
        EasyATL.DebugFrame debug = localizer.getDebug();
        if (debug.uncertainty != null) {
            opMode.telemetry.addData("Residual", "%.1f in, %.1f deg",
                    debug.uncertainty.residualInches,
                    Math.toDegrees(debug.uncertainty.residualHeadingRadians));
        }
        for (EasyATL.TagDebug tag : debug.tags) {
            if (tag.rejectReason != null) {
                opMode.telemetry.addData("Tag " + tag.id, "REJECT %s  w=%.2f", tag.rejectReason, tag.weight);
            } else {
                opMode.telemetry.addData("Tag " + tag.id, "OK  w=%.2f", tag.weight);
            }
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
                ".setSmoothingAlpha(%.2f).setQualityDecayRate(%.2f).setWeightRangeScaleInches(%.0f)",
                config.getSmoothingAlpha(), config.getQualityDecayRate(), config.getWeightRangeScaleInches()));
    }

    static double bump(Gamepad g, double value, double small, double large, double min, double max) {
        if (g.dpadUpWasPressed()) value += small;
        if (g.dpadDownWasPressed()) value -= small;
        if (g.dpadRightWasPressed()) value += large;
        if (g.dpadLeftWasPressed()) value -= large;
        return Math.max(min, Math.min(max, value));
    }

    static void closePortal() {
        if (portal != null) portal.close();
    }
}

class VisionTelemetry extends OpMode {
    @Override
    public void init() {}

    @Override
    public void loop() {
        PedroEasyATLTuning.visionTick();
        telemetry.addLine("Vision only. No Pedro follower. Does not set drive pose.");
        PedroEasyATLTuning.printPose(this);
        PedroEasyATLTuning.printDebug(this);
        PedroEasyATLTuning.printDetections(this);
        telemetry.update();
    }

    @Override
    public void stop() {
        PedroEasyATLTuning.closePortal();
    }
}

class ApplyCorrection extends OpMode {
    @Override
    public void init() {}

    @Override
    public void start() {
        PedroEasyATLTuning.ensureDrive(this);
    }

    @Override
    public void loop() {
        PedroEasyATLTuning.driveTick(this);
        boolean accepted = PedroEasyATLTuning.localizer.localize(
                PedroEasyATLTuning.processor.getDetections(), PedroEasyATLTuning.corrector, 0.20);
        telemetry.addLine("Sets Pedro pose when a quality-approved frame is accepted.");
        telemetry.addData("Applied this loop", accepted && PedroEasyATLTuning.localizer.getQuality() >= 0.20);
        PedroEasyATLTuning.printPose(this);
        telemetry.update();
    }

    @Override
    public void stop() {
        PedroEasyATLTuning.closePortal();
    }
}

class MaxRangeTuner extends OpMode {
    @Override
    public void init() {}

    @Override
    public void start() {
        PedroEasyATLTuning.ensureDrive(this);
    }

    @Override
    public void loop() {
        PedroEasyATLTuning.config.setMaxRangeInches(PedroEasyATLTuning.bump(gamepad1,
                PedroEasyATLTuning.config.getMaxRangeInches(), 1, 6, 1, 200));
        PedroEasyATLTuning.applyConfig();
        PedroEasyATLTuning.driveTick(this);
        PedroEasyATLTuning.visionTick();
        telemetry.addLine("D-pad U/D ±1 in, L/R ±6 in. Increase if useful tags are dropped for range.");
        telemetry.addData("maxRangeInches", PedroEasyATLTuning.config.getMaxRangeInches());
        for (AprilTagDetection d : PedroEasyATLTuning.processor.getDetections()) {
            if (d.ftcPose == null) continue;
            boolean in = d.ftcPose.range > 0 && d.ftcPose.range <= PedroEasyATLTuning.config.getMaxRangeInches();
            telemetry.addData("Tag " + d.id, "range %.1f  %s", d.ftcPose.range, in ? "PASS" : "FAIL range");
        }
        PedroEasyATLTuning.printPose(this);
        PedroEasyATLTuning.printSnippet(this);
        telemetry.update();
    }

    @Override
    public void stop() {
        PedroEasyATLTuning.closePortal();
    }
}

class MaxBearingTuner extends OpMode {
    @Override
    public void init() {}

    @Override
    public void start() {
        PedroEasyATLTuning.ensureDrive(this);
    }

    @Override
    public void loop() {
        PedroEasyATLTuning.config.setMaxBearingDegrees(PedroEasyATLTuning.bump(gamepad1,
                PedroEasyATLTuning.config.getMaxBearingDegrees(), 1, 5, 0, 90));
        PedroEasyATLTuning.applyConfig();
        PedroEasyATLTuning.driveTick(this);
        PedroEasyATLTuning.visionTick();
        telemetry.addLine("D-pad U/D ±1°, L/R ±5°. Increase if edge-of-frame tags are still stable.");
        telemetry.addData("maxBearingDegrees", PedroEasyATLTuning.config.getMaxBearingDegrees());
        for (AprilTagDetection d : PedroEasyATLTuning.processor.getDetections()) {
            if (d.ftcPose == null) continue;
            boolean in = Math.abs(d.ftcPose.bearing) <= PedroEasyATLTuning.config.getMaxBearingDegrees();
            telemetry.addData("Tag " + d.id, "bearing %.1f  %s", d.ftcPose.bearing, in ? "PASS" : "FAIL bearing");
        }
        PedroEasyATLTuning.printPose(this);
        PedroEasyATLTuning.printSnippet(this);
        telemetry.update();
    }

    @Override
    public void stop() {
        PedroEasyATLTuning.closePortal();
    }
}

class MaxTagYawTuner extends OpMode {
    @Override
    public void init() {}

    @Override
    public void start() {
        PedroEasyATLTuning.ensureDrive(this);
    }

    @Override
    public void loop() {
        PedroEasyATLTuning.config.setMaxTagYawDegrees(PedroEasyATLTuning.bump(gamepad1,
                PedroEasyATLTuning.config.getMaxTagYawDegrees(), 1, 5, 0, 90));
        PedroEasyATLTuning.applyConfig();
        PedroEasyATLTuning.driveTick(this);
        PedroEasyATLTuning.visionTick();
        telemetry.addLine("D-pad U/D ±1°, L/R ±5°. Increase if steep yaw still looks accurate.");
        telemetry.addData("maxTagYawDegrees", PedroEasyATLTuning.config.getMaxTagYawDegrees());
        for (AprilTagDetection d : PedroEasyATLTuning.processor.getDetections()) {
            if (d.ftcPose == null) continue;
            boolean in = Math.abs(d.ftcPose.yaw) <= PedroEasyATLTuning.config.getMaxTagYawDegrees();
            telemetry.addData("Tag " + d.id, "yaw %.1f  %s", d.ftcPose.yaw, in ? "PASS" : "FAIL yaw");
        }
        PedroEasyATLTuning.printPose(this);
        PedroEasyATLTuning.printSnippet(this);
        telemetry.update();
    }

    @Override
    public void stop() {
        PedroEasyATLTuning.closePortal();
    }
}

class OutlierDistanceTuner extends OpMode {
    @Override
    public void init() {}

    @Override
    public void start() {
        PedroEasyATLTuning.ensureDrive(this);
    }

    @Override
    public void loop() {
        PedroEasyATLTuning.config.setOutlierDistanceInches(PedroEasyATLTuning.bump(gamepad1,
                PedroEasyATLTuning.config.getOutlierDistanceInches(), 1, 4, 1, 48));
        PedroEasyATLTuning.applyConfig();
        PedroEasyATLTuning.driveTick(this);
        PedroEasyATLTuning.visionTick();
        telemetry.addLine("Need 2+ tags. If a good tag is dropped, increase; if pose jumps, decrease.");
        telemetry.addData("outlierDistanceInches", PedroEasyATLTuning.config.getOutlierDistanceInches());
        PedroEasyATLTuning.printPose(this);
        PedroEasyATLTuning.printSnippet(this);
        telemetry.update();
    }

    @Override
    public void stop() {
        PedroEasyATLTuning.closePortal();
    }
}

class OutlierHeadingTuner extends OpMode {
    @Override
    public void init() {}

    @Override
    public void start() {
        PedroEasyATLTuning.ensureDrive(this);
    }

    @Override
    public void loop() {
        PedroEasyATLTuning.config.setOutlierHeadingDegrees(PedroEasyATLTuning.bump(gamepad1,
                PedroEasyATLTuning.config.getOutlierHeadingDegrees(), 1, 5, 1, 90));
        PedroEasyATLTuning.applyConfig();
        PedroEasyATLTuning.driveTick(this);
        PedroEasyATLTuning.visionTick();
        telemetry.addLine("Need 2+ tags. Heading jumps → decrease. Good tags dropped → increase.");
        telemetry.addData("outlierHeadingDegrees", PedroEasyATLTuning.config.getOutlierHeadingDegrees());
        PedroEasyATLTuning.printPose(this);
        PedroEasyATLTuning.printSnippet(this);
        telemetry.update();
    }

    @Override
    public void stop() {
        PedroEasyATLTuning.closePortal();
    }
}

class SmoothingAlphaTuner extends OpMode {
    @Override
    public void init() {}

    @Override
    public void start() {
        PedroEasyATLTuning.ensureDrive(this);
    }

    @Override
    public void loop() {
        PedroEasyATLTuning.config.setSmoothingAlpha(PedroEasyATLTuning.bump(gamepad1,
                PedroEasyATLTuning.config.getSmoothingAlpha(), 0.05, 0.15, 0, 1));
        PedroEasyATLTuning.applyConfig();
        PedroEasyATLTuning.driveTick(this);
        PedroEasyATLTuning.visionTick();
        telemetry.addLine("1.0 = no smoothing (jitter). Lower = steadier, more lag.");
        telemetry.addData("smoothingAlpha", "%.2f", PedroEasyATLTuning.config.getSmoothingAlpha());
        PedroEasyATLTuning.printPose(this);
        PedroEasyATLTuning.printSnippet(this);
        telemetry.update();
    }

    @Override
    public void stop() {
        PedroEasyATLTuning.closePortal();
    }
}

class QualityDecayTuner extends OpMode {
    @Override
    public void init() {}

    @Override
    public void start() {
        PedroEasyATLTuning.ensureDrive(this);
    }

    @Override
    public void loop() {
        PedroEasyATLTuning.config.setQualityDecayRate(PedroEasyATLTuning.bump(gamepad1,
                PedroEasyATLTuning.config.getQualityDecayRate(), 0.05, 0.2, 0, 4));
        PedroEasyATLTuning.applyConfig();
        PedroEasyATLTuning.driveTick(this);
        PedroEasyATLTuning.visionTick();
        telemetry.addLine("Cover the lens: quality should fall. Higher rate = untrusted sooner.");
        telemetry.addData("qualityDecayRate", "%.2f /s", PedroEasyATLTuning.config.getQualityDecayRate());
        telemetry.addData("Quality now", "%.0f%%", 100 * PedroEasyATLTuning.localizer.getQuality());
        PedroEasyATLTuning.printPose(this);
        PedroEasyATLTuning.printSnippet(this);
        telemetry.update();
    }

    @Override
    public void stop() {
        PedroEasyATLTuning.closePortal();
    }
}
