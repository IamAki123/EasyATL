package org.firstinspires.ftc.teamcode.easyatl;

import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.Pose;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.easyatl.EasyATL;
import org.firstinspires.ftc.easyatl.FtcEasyATL;
import org.firstinspires.ftc.robotcore.external.Telemetry;
// Replace these two imports with your team's webcam helper and Pedro Constants.
import org.firstinspires.ftc.teamcode.Mechanisms.AprilTagWebcam;
import org.firstinspires.ftc.teamcode.OFSB1.Constants;

/**
 * EasyATL-only robot setup. {@link EasyATLTuning} and match OpModes should read camera, tags,
 * {@link EasyATL.Config}, webcam, and Pedro follower from here so those values are not copied
 * into every TeleOp.
 *
 * <p>Keep drivetrain PID, motor names, and other non-vision settings in your existing Pedro
 * {@code Constants} (this class calls {@code Constants.createFollower}). Do not dump the whole
 * robot into this file.</p>
 */
public final class EasyATLConstants {
    private EasyATLConstants() {}

    /** Camera lens: inches forward of robot center, inches right, yaw radians CCW from robot forward. */
    public static final double CAMERA_FORWARD_IN = 0.75;
    public static final double CAMERA_RIGHT_IN = 0.25;
    public static final double CAMERA_YAW_RAD = Math.toRadians(-4);

    public static EasyATL.CameraConfig camera() {
        return new EasyATL.CameraConfig(CAMERA_FORWARD_IN, CAMERA_RIGHT_IN, CAMERA_YAW_RAD);
    }

    public static EasyATL.Config config() {
        return new EasyATL.Config()
                .setMaxRangeInches(72)
                .setMaxBearingDegrees(55)
                .setMaxTagYawDegrees(45)
                .setOutlierDistanceInches(12)
                .setOutlierHeadingDegrees(25)
                .setSmoothingAlpha(0.70)
                .setQualityDecayRate(0.8);
    }

    public static Pose startingPose() {
        return new Pose(0, 0, 0);
    }

    public static Follower createFollower(HardwareMap hardwareMap) {
        Follower follower = Constants.createFollower(hardwareMap);
        follower.setStartingPose(startingPose());
        return follower;
    }

    public static AprilTagWebcam createWebcam(HardwareMap hardwareMap, Telemetry telemetry) {
        AprilTagWebcam webcam = new AprilTagWebcam();
        webcam.init(hardwareMap, telemetry);
        return webcam;
    }

    /** Localizer using {@link #camera()}, the given pipeline, and {@link #addTags(FtcEasyATL)}. */
    public static FtcEasyATL createLocalizer(EasyATL.Config pipeline) {
        FtcEasyATL localizer = new FtcEasyATL(camera(), pipeline);
        addTags(localizer);
        return localizer;
    }

    public static void addTags(FtcEasyATL localizer) {
        localizer.addTag(21, 8, 8, Math.toRadians(45));
        // localizer.addTag(22, 72, 8, Math.toRadians(90));
        // localizer.addTag(23, 136, 72, Math.toRadians(180));
    }
}
