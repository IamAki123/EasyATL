package org.firstinspires.ftc.teamcode.easyatl;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.Gamepad;

import org.firstinspires.ftc.easyatl.DefaultSdkConstants;
import org.firstinspires.ftc.easyatl.EasyATL;
import org.firstinspires.ftc.easyatl.FieldPose;
import org.firstinspires.ftc.easyatl.FtcEasyATL;
import org.firstinspires.ftc.vision.VisionPortal;
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection;
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor;

import java.util.Locale;

/**
 * Practice tuner with <strong>no Pedro dependency</strong>. D-pad menu in init; after Play the
 * D-pad changes the selected filter. Push the robot by hand (or use your own TeleOp) and compare
 * vision pose to tape.
 *
 * <p>Uses {@link DefaultSdkConstants} from the AAR if you do not copy {@code EasyATLSdkConstants}.
 * To override camera/tags, copy that class into this package and switch the factory calls below.</p>
 */
@TeleOp(name = "EasyATL Tuner", group = "EasyATL")
public class EasyATLSdkTuner extends OpMode {
    private enum Screen {
        TELEMETRY("Vision telemetry"),
        RANGE("Max range (in)"),
        BEARING("Max bearing (deg)"),
        YAW("Max tag yaw (deg)"),
        OUTLIER_XY("XY outlier (in)"),
        OUTLIER_H("Heading outlier (deg)"),
        SMOOTH("Smoothing alpha"),
        DECAY("Quality decay rate"),
        WEIGHT("Weight range scale (in)");

        final String label;
        Screen(String label) { this.label = label; }
    }

    private Screen screen = Screen.TELEMETRY;
    private AprilTagProcessor processor;
    private VisionPortal portal;
    private FtcEasyATL localizer;
    private EasyATL.Config config;

    @Override
    public void init() {
        processor = DefaultSdkConstants.createProcessor();
        portal = DefaultSdkConstants.createPortal(hardwareMap, processor, telemetry);
        config = DefaultSdkConstants.config();
        localizer = DefaultSdkConstants.createLocalizer(config);
        telemetry.addLine("Init: D-pad up/down picks a test. Press Play, then D-pad changes that knob.");
        telemetry.update();
    }

    @Override
    public void init_loop() {
        if (gamepad1.dpadUpWasPressed()) screen = prev(screen);
        if (gamepad1.dpadDownWasPressed()) screen = next(screen);
        telemetry.addLine("Selected: " + screen.label);
        telemetry.addLine("Play to run. Left stick is unused (no drivetrain).");
        printDetections();
        telemetry.update();
    }

    @Override
    public void loop() {
        bump(gamepad1);
        localizer.setConfig(config);
        localizer.localize(processor.getDetections());
        telemetry.addLine(screen.label);
        telemetry.addLine(hint());
        printKnob();
        printPose();
        printDebug();
        printDetections();
        printSnippet();
        telemetry.update();
    }

    @Override
    public void stop() {
        if (portal != null) portal.close();
    }

    private void bump(Gamepad g) {
        switch (screen) {
            case RANGE:
                config.setMaxRangeInches(clamp(config.getMaxRangeInches() + delta(g, 1, 6), 1, 200));
                break;
            case BEARING:
                config.setMaxBearingDegrees(clamp(config.getMaxBearingDegrees() + delta(g, 1, 5), 0, 90));
                break;
            case YAW:
                config.setMaxTagYawDegrees(clamp(config.getMaxTagYawDegrees() + delta(g, 1, 5), 0, 90));
                break;
            case OUTLIER_XY:
                config.setOutlierDistanceInches(clamp(config.getOutlierDistanceInches() + delta(g, 1, 4), 1, 48));
                break;
            case OUTLIER_H:
                config.setOutlierHeadingDegrees(clamp(config.getOutlierHeadingDegrees() + delta(g, 1, 5), 1, 90));
                break;
            case SMOOTH:
                config.setSmoothingAlpha(clamp(config.getSmoothingAlpha() + delta(g, 0.05, 0.15), 0, 1));
                break;
            case DECAY:
                config.setQualityDecayRate(clamp(config.getQualityDecayRate() + delta(g, 0.05, 0.2), 0, 4));
                break;
            case WEIGHT:
                config.setWeightRangeScaleInches(clamp(config.getWeightRangeScaleInches() + delta(g, 1, 6), 6, 120));
                break;
            default:
                break;
        }
    }

    private static double delta(Gamepad g, double small, double large) {
        double d = 0;
        if (g.dpadUpWasPressed()) d += small;
        if (g.dpadDownWasPressed()) d -= small;
        if (g.dpadRightWasPressed()) d += large;
        if (g.dpadLeftWasPressed()) d -= large;
        return d;
    }

    private void printKnob() {
        switch (screen) {
            case RANGE:
                telemetry.addData("maxRangeInches", config.getMaxRangeInches());
                break;
            case BEARING:
                telemetry.addData("maxBearingDegrees", config.getMaxBearingDegrees());
                break;
            case YAW:
                telemetry.addData("maxTagYawDegrees", config.getMaxTagYawDegrees());
                break;
            case OUTLIER_XY:
                telemetry.addData("outlierDistanceInches", config.getOutlierDistanceInches());
                break;
            case OUTLIER_H:
                telemetry.addData("outlierHeadingDegrees", config.getOutlierHeadingDegrees());
                break;
            case SMOOTH:
                telemetry.addData("smoothingAlpha", "%.2f", config.getSmoothingAlpha());
                break;
            case DECAY:
                telemetry.addData("qualityDecayRate", "%.2f /s", config.getQualityDecayRate());
                break;
            case WEIGHT:
                telemetry.addData("weightRangeScaleInches", config.getWeightRangeScaleInches());
                break;
            default:
                telemetry.addLine("Telemetry only. Does not write a drive pose.");
                break;
        }
    }

    private String hint() {
        switch (screen) {
            case RANGE:
                return "Increase if useful tags drop for range.";
            case BEARING:
                return "Increase if edge-of-frame tags are still stable.";
            case YAW:
                return "Increase if steep yaw still looks accurate.";
            case OUTLIER_XY:
                return "Need 2+ tags. Good tag dropped → increase; pose jumps → decrease.";
            case OUTLIER_H:
                return "Need 2+ tags. Heading jumps → decrease.";
            case SMOOTH:
                return "1.0 = no smoothing (jitter). Lower = steadier, more lag.";
            case DECAY:
                return "Cover the lens: quality should fall.";
            case WEIGHT:
                return "Larger scale keeps far tags heavier. Smaller prefers close tags.";
            default:
                return "Compare Vision pose to tape. Geometry first, then filters.";
        }
    }

    private void printPose() {
        if (localizer.hasPose()) {
            FieldPose p = localizer.getPose();
            telemetry.addData("Vision", "(%.1f, %.1f, %.1f deg)", p.x, p.y, p.headingDegrees());
            telemetry.addData("Quality", "%.0f%%", 100 * localizer.getQuality());
            telemetry.addData("Visible", localizer.getVisibleTags());
            telemetry.addData("Accepted", localizer.getAcceptedTags());
        } else {
            telemetry.addLine("No vision pose yet");
        }
    }

    private void printDebug() {
        EasyATL.DebugFrame debug = localizer.getDebug();
        if (debug.uncertainty != null) {
            telemetry.addData("Residual", "%.1f in, %.1f deg",
                    debug.uncertainty.residualInches,
                    Math.toDegrees(debug.uncertainty.residualHeadingRadians));
            telemetry.addData("Sigma XY", "%.1f in", debug.uncertainty.sigmaXInches);
        }
        for (EasyATL.TagDebug tag : debug.tags) {
            if (tag.rejectReason != null) {
                telemetry.addData("Tag " + tag.id, "REJECT %s  w=%.2f", tag.rejectReason, tag.weight);
            } else {
                telemetry.addData("Tag " + tag.id, "OK  w=%.2f", tag.weight);
            }
        }
    }

    private void printDetections() {
        telemetry.addData("Raw tags", processor.getDetections().size());
        for (AprilTagDetection d : processor.getDetections()) {
            if (d.ftcPose == null) {
                telemetry.addData("Tag " + d.id, "no FTC pose");
                continue;
            }
            telemetry.addData("Tag " + d.id, "range %.1f  bear %.1f  yaw %.1f  margin %.0f",
                    d.ftcPose.range, d.ftcPose.bearing, d.ftcPose.yaw, d.decisionMargin);
        }
    }

    private void printSnippet() {
        telemetry.addLine("--- copy into Config ---");
        telemetry.addLine(String.format(Locale.US,
                ".setMaxRangeInches(%.0f).setMaxBearingDegrees(%.0f).setMaxTagYawDegrees(%.0f)",
                config.getMaxRangeInches(), config.getMaxBearingDegrees(), config.getMaxTagYawDegrees()));
        telemetry.addLine(String.format(Locale.US,
                ".setOutlierDistanceInches(%.0f).setOutlierHeadingDegrees(%.0f)",
                config.getOutlierDistanceInches(), config.getOutlierHeadingDegrees()));
        telemetry.addLine(String.format(Locale.US,
                ".setSmoothingAlpha(%.2f).setQualityDecayRate(%.2f).setWeightRangeScaleInches(%.0f)",
                config.getSmoothingAlpha(), config.getQualityDecayRate(), config.getWeightRangeScaleInches()));
    }

    private static Screen next(Screen s) {
        Screen[] v = Screen.values();
        return v[(s.ordinal() + 1) % v.length];
    }

    private static Screen prev(Screen s) {
        Screen[] v = Screen.values();
        return v[(s.ordinal() + v.length - 1) % v.length];
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
