package org.firstinspires.ftc.teamcode.easyatl;

import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.easyatl.DefaultSdkConstants;
import org.firstinspires.ftc.easyatl.EasyATL;
import org.firstinspires.ftc.easyatl.FieldTags;
import org.firstinspires.ftc.easyatl.FtcEasyATL;
import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.vision.VisionPortal;
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor;

/**
 * Optional TeamCode override of {@link org.firstinspires.ftc.easyatl.DefaultSdkConstants}.
 * Copy into TeamCode and point the sample/tuner at <em>this</em> class to change lens offsets,
 * webcam name, or tags. If you never copy this file, the AAR defaults still compile and run.
 */
public final class EasyATLSdkConstants {
    private EasyATLSdkConstants() {}

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

    public static AprilTagProcessor createProcessor() {
        return DefaultSdkConstants.createProcessor();
    }

    public static VisionPortal createPortal(HardwareMap hardwareMap, AprilTagProcessor processor,
            Telemetry telemetry) {
        return DefaultSdkConstants.createPortal(hardwareMap, processor, telemetry, WEBCAM_NAME);
    }

    /** Newest bundled season. Swap one line in {@link #addTags} for a past game or custom tags. */
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
