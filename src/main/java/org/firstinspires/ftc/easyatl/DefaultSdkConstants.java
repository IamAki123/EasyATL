package org.firstinspires.ftc.easyatl;

import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName;
import org.firstinspires.ftc.vision.VisionPortal;
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor;

/**
 * Defaults shipped in the EasyATL AAR. Used when TeamCode has no
 * {@code EasyATLSdkConstants} copy-in file.
 *
 * <p>Safe starting point: lens at robot center, pointed forward and level, library
 * {@link EasyATL.Config} defaults, newest bundled season tags, webcam name
 * {@code Webcam 1}.</p>
 *
 * <p>To override, copy {@code tuning/sdk/EasyATLSdkConstants.java} into TeamCode and point
 * the sample/tuner at that class instead of this one.</p>
 */
public final class DefaultSdkConstants {
    private DefaultSdkConstants() {}

    /** Robot configuration name of the USB webcam. */
    public static final String WEBCAM_NAME = "Webcam 1";

    public static final double CAMERA_FORWARD_IN = 0;
    public static final double CAMERA_RIGHT_IN = 0;
    public static final double CAMERA_YAW_RAD = 0;
    public static final double CAMERA_PITCH_RAD = 0;

    public static EasyATL.CameraConfig camera() {
        return new EasyATL.CameraConfig(CAMERA_FORWARD_IN, CAMERA_RIGHT_IN, CAMERA_YAW_RAD, CAMERA_PITCH_RAD);
    }

    /** Library pipeline defaults ({@code new EasyATL.Config()}). */
    public static EasyATL.Config config() {
        return new EasyATL.Config();
    }

    public static AprilTagProcessor createProcessor() {
        return new AprilTagProcessor.Builder().build();
    }

    public static VisionPortal createPortal(HardwareMap hardwareMap, AprilTagProcessor processor,
            Telemetry telemetry) {
        return createPortal(hardwareMap, processor, telemetry, WEBCAM_NAME);
    }

    public static VisionPortal createPortal(HardwareMap hardwareMap, AprilTagProcessor processor,
            Telemetry telemetry, String webcamName) {
        if (hardwareMap == null) throw new IllegalArgumentException("hardwareMap cannot be null");
        if (processor == null) throw new IllegalArgumentException("processor cannot be null");
        if (webcamName == null || webcamName.length() == 0) {
            throw new IllegalArgumentException("webcam name cannot be empty");
        }
        VisionPortal portal = new VisionPortal.Builder()
                .setCamera(hardwareMap.get(WebcamName.class, webcamName))
                .addProcessor(processor)
                .build();
        if (telemetry != null) telemetry.addData("Webcam", webcamName);
        return portal;
    }

    /** Camera + {@link #config()} + {@link FieldTags#latest()}. */
    public static FtcEasyATL createLocalizer() {
        return createLocalizer(config());
    }

    public static FtcEasyATL createLocalizer(EasyATL.Config pipeline) {
        FtcEasyATL localizer = new FtcEasyATL(camera(), pipeline);
        addTags(localizer);
        return localizer;
    }

    public static void addTags(FtcEasyATL localizer) {
        if (localizer == null) throw new IllegalArgumentException("localizer cannot be null");
        localizer.useLatestSeason();
    }

    /** Example homemade set; not applied unless you {@code useFieldSet(customAprilTags())}. */
    public static FieldTags customAprilTags() {
        return FieldTags.custom("Practice field")
                .add(21, 8, 8, Math.toRadians(45), "left wall")
                .build();
    }
}
