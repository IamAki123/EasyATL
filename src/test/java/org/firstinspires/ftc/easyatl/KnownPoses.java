package org.firstinspires.ftc.easyatl;

/**
 * Inverse of {@link EasyATL}'s camera-to-field math: a known robot pose and tag produce the
 * observation that {@code localize()} should turn back into that pose.
 */
final class KnownPoses {
    private KnownPoses() {}

    static EasyATL.Observation observation(
            int id, double tagX, double tagY, double tagFacing,
            EasyATL.CameraConfig camera, FieldPose robot) {
        double h = robot.heading;
        double dx = tagX - robot.x;
        double dy = tagY - robot.y;
        double f = dx * Math.cos(h) + dy * Math.sin(h);
        double r = dx * Math.sin(h) - dy * Math.cos(h);
        double fp = f - camera.forward;
        double rp = r - camera.right;
        double c = Math.cos(camera.yawRadians);
        double s = Math.sin(camera.yawRadians);
        // Inverse of yaw, then pitch (z=0 in generated observations).
        double y2 = fp * c - rp * s;
        double x1 = fp * s + rp * c;
        double cp = Math.cos(camera.pitchRadians);
        if (Math.abs(cp) < 1e-9) {
            throw new IllegalArgumentException("KnownPoses cannot invert a ±90° camera pitch");
        }
        double forward = y2 / cp;
        double cr = Math.cos(camera.rollRadians);
        double right = cr == 0 ? x1 : x1 / cr;
        double yawDegrees = Math.toDegrees(wrap(tagFacing + Math.PI - h - camera.yawRadians));
        double range = Math.hypot(right, forward);
        double bearingDegrees = Math.toDegrees(Math.atan2(right, forward));
        return new EasyATL.Observation(id, right, forward, range, bearingDegrees, yawDegrees);
    }

    static EasyATL.Observation withRange(EasyATL.Observation o, double range) {
        return new EasyATL.Observation(o.id, o.right, o.forward, range, o.bearingDegrees, o.yawDegrees);
    }

    static EasyATL.Observation withBearing(EasyATL.Observation o, double bearingDegrees) {
        return new EasyATL.Observation(o.id, o.right, o.forward, o.range, bearingDegrees, o.yawDegrees);
    }

    static EasyATL.Observation withYaw(EasyATL.Observation o, double yawDegrees) {
        return new EasyATL.Observation(o.id, o.right, o.forward, o.range, o.bearingDegrees, yawDegrees);
    }

    static double wrap(double a) {
        while (a > Math.PI) a -= 2 * Math.PI;
        while (a <= -Math.PI) a += 2 * Math.PI;
        return a;
    }
}
