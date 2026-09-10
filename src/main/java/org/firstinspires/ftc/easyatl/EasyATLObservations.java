package org.firstinspires.ftc.easyatl;

/**
 * Camera-frame observation helpers for sources that are not {@code AprilTagProcessor}
 * (Limelight fiducials, MegaTag per-tag arrays, custom detectors).
 *
 * <p>Keep {@link EasyATL} itself free of those SDKs. Convert each tag into the same camera-frame
 * quantities FTC calls {@code ftcPose}: inches right/forward, range, bearing degrees, tag yaw
 * degrees. Then call {@link EasyATL#localize(java.util.List)}.</p>
 *
 * <p><strong>Limelight:</strong> if you already trust MegaTag {@code botpose}, you do not need
 * EasyATL fusion — use that field pose directly. This helper is for <em>per-tag</em> results so
 * EasyATL can filter and fuse them. Limelight {@code tx} is typically bearing in degrees; if you
 * also have 3D camera-space {@code (x, z)} as right/forward inches, pass those into
 * {@link #cameraFrame(int, double, double, double, double, double)}.</p>
 *
 * <p><strong>Do not</strong> feed Limelight field-space botpose in as if it were camera-frame
 * {@code ftcPose}.</p>
 */
public final class EasyATLObservations {
    private EasyATLObservations() {}

    /**
     * Camera-frame tag, same axes as FTC {@code ftcPose.x/y/range/bearing/yaw}.
     *
     * @param right inches right of the lens ({@code ftcPose.x})
     * @param forward inches in front of the lens ({@code ftcPose.y})
     */
    public static EasyATL.Observation cameraFrame(int id, double right, double forward,
            double range, double bearingDegrees, double yawDegrees) {
        return new EasyATL.Observation(id, right, forward, range, bearingDegrees, yawDegrees);
    }

    /**
     * Camera-frame tag with height and a detector score (Limelight {@code ta} is <em>not</em> a
     * decision margin; leave margin at {@code 0} if you do not have one).
     */
    public static EasyATL.Observation cameraFrame(int id, double right, double forward,
            double range, double bearingDegrees, double yawDegrees, double z, double decisionMargin) {
        return new EasyATL.Observation(id, right, forward, range, bearingDegrees, yawDegrees,
                z, decisionMargin, 0L);
    }

    /**
     * Polar camera-frame measurement. Forward/right are recovered as
     * {@code range · (cos(bearing), sin(bearing))} with bearing in degrees, same as FTC.
     *
     * <p>Useful when a detector gives range + {@code tx}-style bearing but not Cartesian
     * {@code x/y}.</p>
     */
    public static EasyATL.Observation fromPolar(int id, double rangeInches, double bearingDegrees,
            double yawDegrees) {
        double bearing = Math.toRadians(bearingDegrees);
        double forward = rangeInches * Math.cos(bearing);
        double right = rangeInches * Math.sin(bearing);
        return new EasyATL.Observation(id, right, forward, rangeInches, bearingDegrees, yawDegrees);
    }
}
