package org.firstinspires.ftc.easyatl;

import java.util.Locale;

/**
 * Immutable robot field pose. Position is inches; heading is radians, CCW-positive, with
 * {@code 0} along field {@code +X}.
 *
 * <p>EasyATL does not depend on Pedro or Road Runner. Convert at the call site, for example
 * {@code new Pose(pose.x, pose.y, pose.heading)} (Pedro) or
 * {@code new Pose2d(pose.x, pose.y, pose.heading)} (Road Runner). Or implement
 * {@link PoseCorrector}.</p>
 *
 * <p>{@link #equals(Object)} / {@link #hashCode()} treat headings that wrap to the same angle as
 * equal ({@code π} and {@code −π} are the same heading). The stored {@code heading} field is not
 * rewritten.</p>
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
        if (!Double.isFinite(x)) throw new IllegalArgumentException("pose x must be finite");
        if (!Double.isFinite(y)) throw new IllegalArgumentException("pose y must be finite");
        if (!Double.isFinite(heading)) throw new IllegalArgumentException("pose heading must be finite");
        this.x = x;
        this.y = y;
        this.heading = heading;
    }

    /** @return heading in degrees, CCW-positive */
    public double headingDegrees() {
        return Math.toDegrees(heading);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof FieldPose)) return false;
        FieldPose other = (FieldPose) obj;
        return Double.compare(x, other.x) == 0
                && Double.compare(y, other.y) == 0
                && Double.compare(EasyATLMath.wrap(heading), EasyATLMath.wrap(other.heading)) == 0;
    }

    @Override
    public int hashCode() {
        int result = Double.valueOf(x).hashCode();
        result = 31 * result + Double.valueOf(y).hashCode();
        result = 31 * result + Double.valueOf(EasyATLMath.wrap(heading)).hashCode();
        return result;
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "FieldPose(x=%.3f, y=%.3f, heading=%.4f rad)", x, y, heading);
    }
}
