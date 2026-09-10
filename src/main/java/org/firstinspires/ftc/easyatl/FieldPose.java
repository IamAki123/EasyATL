package org.firstinspires.ftc.easyatl;

/**
 * Immutable robot field pose. Position is inches; heading is radians, CCW-positive, with
 * {@code 0} along field {@code +X}.
 *
 * <p>EasyATL does not depend on Pedro or Road Runner. Convert at the call site, for example
 * {@code new Pose(pose.x, pose.y, pose.heading)} (Pedro) or
 * {@code new Pose2d(pose.x, pose.y, pose.heading)} (Road Runner).</p>
 */
public final class FieldPose {
    /** Robot center field X, inches. */
    public final double x;
    /** Robot center field Y, inches. */
    public final double y;
    /** Robot heading, radians. */
    public final double heading;

    /**
     * @param x field X, inches
     * @param y field Y, inches
     * @param heading radians, CCW-positive
     */
    public FieldPose(double x, double y, double heading) {
        this.x = x;
        this.y = y;
        this.heading = heading;
    }

    /** @return heading in degrees, CCW-positive */
    public double headingDegrees() {
        return Math.toDegrees(heading);
    }
}
