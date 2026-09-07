package org.firstinspires.ftc.easyatl;

/** Immutable field pose. Position is inches; heading is radians and CCW-positive. */
public final class FieldPose {
    public final double x, y, heading;
    public FieldPose(double x, double y, double heading) { this.x = x; this.y = y; this.heading = heading; }
}
