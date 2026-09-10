package org.firstinspires.ftc.teamcode.easyatl;

import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.Pose;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.easyatl.DefaultSdkConstants;
import org.firstinspires.ftc.easyatl.EasyATL;
import org.firstinspires.ftc.easyatl.FieldTags;
import org.firstinspires.ftc.easyatl.FtcEasyATL;
import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.vision.VisionPortal;
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor;

/**
 * Pedro-only EasyATL setup. Copy this file into TeamCode with {@code PedroEasyATLTuning}.
 * Camera, tags, {@link EasyATL.Config}, webcam name, and the Pedro follower live here so match
 * OpModes do not duplicate them.
 *
 * <p>Keep drivetrain PID and motor names in your existing Pedro {@code Constants}. This class
 * only needs {@code Constants.createFollower} — fill in {@link #createFollower(HardwareMap)}.</p>
 */
public final class PedroEasyATLConstants {
    private PedroEasyATLConstants() {}

    public static final String WEBCAM_NAME = "Webcam 1";

    /**
     * Lens vs robot center. Tape these. {@code 0} yaw = optical axis pointed robot-forward.
     * Negative yaw = lens pointed right; negative pitch = tilted down.
     */
    public static final double CAMERA_FORWARD_IN = 0.75;
    public static final double CAMERA_RIGHT_IN = 0.25;
    public static final double CAMERA_YAW_RAD = 0;
    public static final double CAMERA_PITCH_RAD = 0;

    public static EasyATL.CameraConfig camera() {
        return new EasyATL.CameraConfig(CAMERA_FORWARD_IN, CAMERA_RIGHT_IN, CAMERA_YAW_RAD, CAMERA_PITCH_RAD);
    }

    public static EasyATL.Config config() {
        return new EasyATL.Config()
                .setMaxRangeInches(72)
                .setMaxBearingDegrees(55)
                .setMaxTagYawDegrees(45)
                .setOutlierDistanceInches(12)
                .setOutlierHeadingDegrees(25)
                .setSmoothingAlpha(0.70)
                .setQualityDecayRate(0.8)
                .setWeightRangeScaleInches(36);
    }

    public static Pose startingPose() {
        return new Pose(0, 0, 0);
    }

    /**
     * Point this at <em>your</em> Pedro {@code Constants.createFollower}. Do not import a
     * Super Sigma / OFSB1 class — use the Constants file already in your FTC project.
     */
    public static Follower createFollower(HardwareMap hardwareMap) {
        if (hardwareMap == null) throw new IllegalArgumentException("hardwareMap cannot be null");
        // Follower follower = org.firstinspires.ftc.teamcode.Constants.createFollower(hardwareMap);
        // follower.setStartingPose(startingPose());
        // return follower;
        throw new UnsupportedOperationException(
                "PedroEasyATLConstants.createFollower: return your Pedro Constants.createFollower(hardwareMap)");
    }

    public static AprilTagProcessor createProcessor() {
        return DefaultSdkConstants.createProcessor();
    }

    public static VisionPortal createPortal(HardwareMap hardwareMap, AprilTagProcessor processor,
            Telemetry telemetry) {
        return DefaultSdkConstants.createPortal(hardwareMap, processor, telemetry, WEBCAM_NAME);
    }

    public static FtcEasyATL createLocalizer(EasyATL.Config pipeline) {
        FtcEasyATL localizer = new FtcEasyATL(camera(), pipeline);
        addTags(localizer);
        return localizer;
    }

    public static void addTags(FtcEasyATL localizer) {
        localizer.useLatestSeason();
        // localizer.useSeason(FieldTags.Season.INTO_THE_DEEP);
        // localizer.useFieldSet(customAprilTags());
        // localizer.addCurrentGameTags();
    }

    public static FieldTags customAprilTags() {
        return FieldTags.custom("Practice field")
                .add(21, 8, 8, Math.toRadians(45), "left wall")
                .build();
    }
}
