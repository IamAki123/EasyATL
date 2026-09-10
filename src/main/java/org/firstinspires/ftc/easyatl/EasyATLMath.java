package org.firstinspires.ftc.easyatl;

import java.util.List;

/**
 * Package-private pose math used by {@link EasyATL}. Not part of the public AAR API.
 */
final class EasyATLMath {
    private EasyATLMath() {}

    static final class Estimate {
        final FieldPose pose;
        final double weight;
        final int id;

        Estimate(FieldPose pose, double weight, int id) {
            this.pose = pose;
            this.weight = weight;
            this.id = id;
        }
    }

    static double wrap(double a) {
        while (a > Math.PI) a -= 2 * Math.PI;
        while (a <= -Math.PI) a += 2 * Math.PI;
        return a;
    }

    static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    /**
     * Camera-frame tag (right, forward, z) → robot frame (roll, then pitch, then yaw + mount
     * offset) → field pose using tag XY and facing.
     */
    static FieldPose toPose(EasyATL.CameraConfig cam, double tagX, double tagY, double tagFacing,
            EasyATL.Observation o) {
        double x = o.right;
        double y = o.forward;
        double z = o.z;
        double cr = Math.cos(cam.rollRadians);
        double sr = Math.sin(cam.rollRadians);
        double x1 = x * cr - z * sr;
        double z1 = x * sr + z * cr;
        double cp = Math.cos(cam.pitchRadians);
        double sp = Math.sin(cam.pitchRadians);
        double y2 = y * cp - z1 * sp;
        double c = Math.cos(cam.yawRadians);
        double s = Math.sin(cam.yawRadians);
        double f = y2 * c + x1 * s + cam.forward;
        double r = -y2 * s + x1 * c + cam.right;
        double h = wrap(tagFacing + Math.PI - Math.toRadians(o.yawDegrees) - cam.yawRadians);
        return new FieldPose(tagX - (f * Math.cos(h) + r * Math.sin(h)),
                tagY - (f * Math.sin(h) - r * Math.cos(h)), h);
    }

    static double weight(EasyATL.Observation o, EasyATL.Config config) {
        double scale = config.getWeightRangeScaleInches();
        double range = 1 / (1 + Math.pow(o.range / scale, 2));
        double angle = Math.cos(Math.toRadians(o.bearingDegrees)) * Math.cos(Math.toRadians(o.yawDegrees));
        double margin = 1;
        if (Double.isFinite(o.decisionMargin) && o.decisionMargin > 0) {
            margin = clamp(o.decisionMargin / config.getDecisionMarginScale(), 0, 1);
        }
        return Math.max(config.getMinWeight(), range * Math.max(0, angle) * margin);
    }

    static FieldPose mean(List<Estimate> values, FieldPose center, EasyATL.Config config) {
        double x = 0, y = 0, s = 0, c = 0, total = 0;
        double outlierHeadingRadians = Math.toRadians(config.getOutlierHeadingDegrees());
        for (Estimate e : values) {
            double dist = Math.hypot(e.pose.x - center.x, e.pose.y - center.y);
            double headingErr = Math.abs(wrap(e.pose.heading - center.heading));
            double huber = huber(dist, config.getOutlierDistanceInches())
                    * huber(headingErr, outlierHeadingRadians);
            double w = e.weight * huber;
            x += w * e.pose.x;
            y += w * e.pose.y;
            s += w * Math.sin(e.pose.heading);
            c += w * Math.cos(e.pose.heading);
            total += w;
        }
        return new FieldPose(x / total, y / total, Math.atan2(s, c));
    }

    static double huber(double residual, double scale) {
        if (scale <= 0) return 1;
        double u = residual / scale;
        if (u <= 1) return 1;
        return 1 / u;
    }

    static FieldPose robustCenter(List<Estimate> values, EasyATL.Config config) {
        Estimate best = null;
        double bestCost = Double.POSITIVE_INFINITY;
        double outlierHeadingRadians = Math.toRadians(config.getOutlierHeadingDegrees());
        for (Estimate candidate : values) {
            double cost = 0;
            for (Estimate other : values) {
                cost += Math.min(1, Math.hypot(candidate.pose.x - other.pose.x, candidate.pose.y - other.pose.y)
                        / config.getOutlierDistanceInches())
                        + Math.min(1, Math.abs(wrap(candidate.pose.heading - other.pose.heading))
                        / outlierHeadingRadians);
            }
            if (cost < bestCost || (cost == bestCost && (best == null || candidate.weight > best.weight))) {
                best = candidate;
                bestCost = cost;
            }
        }
        return best.pose;
    }

    static FieldPose limitStep(FieldPose oldPose, FieldPose measurement, EasyATL.Config config) {
        if (oldPose == null) return measurement;
        double x = measurement.x;
        double y = measurement.y;
        double h = measurement.heading;
        if (config.getMaxStepInches() > 0) {
            double dx = x - oldPose.x;
            double dy = y - oldPose.y;
            double dist = Math.hypot(dx, dy);
            if (dist > config.getMaxStepInches()) {
                double s = config.getMaxStepInches() / dist;
                x = oldPose.x + dx * s;
                y = oldPose.y + dy * s;
            }
        }
        if (config.getMaxStepDegrees() > 0) {
            double maxRad = Math.toRadians(config.getMaxStepDegrees());
            double dh = wrap(h - oldPose.heading);
            if (Math.abs(dh) > maxRad) h = wrap(oldPose.heading + Math.copySign(maxRad, dh));
        }
        return new FieldPose(x, y, h);
    }

    static FieldPose blend(FieldPose oldPose, FieldPose newPose, double a) {
        return new FieldPose(
                oldPose.x + a * (newPose.x - oldPose.x),
                oldPose.y + a * (newPose.y - oldPose.y),
                wrap(oldPose.heading + a * wrap(newPose.heading - oldPose.heading)));
    }

    static EasyATL.Uncertainty uncertainty(List<Estimate> inliers, FieldPose measurement) {
        double sumD = 0;
        double sumH = 0;
        double sumRangeProxy = 0;
        for (Estimate e : inliers) {
            sumD += Math.hypot(e.pose.x - measurement.x, e.pose.y - measurement.y);
            sumH += Math.abs(wrap(e.pose.heading - measurement.heading));
            sumRangeProxy += 1.0 / Math.max(e.weight, 1e-6);
        }
        int n = inliers.size();
        double residualInches = sumD / n;
        double residualHeading = sumH / n;
        double spread = Math.max(0.5, residualInches);
        double sigmaXY = spread * Math.sqrt(sumRangeProxy / n) / Math.sqrt(n);
        double sigmaH = Math.max(Math.toRadians(1), residualHeading) / Math.sqrt(n);
        return new EasyATL.Uncertainty(sigmaXY, sigmaXY, sigmaH, residualInches, residualHeading);
    }

    static double scoreQuality(List<Estimate> inliers, int estimateCount, EasyATL.Uncertainty uncertainty,
            EasyATL.Config config) {
        double meanWeight = 0;
        for (Estimate e : inliers) meanWeight += e.weight;
        meanWeight /= inliers.size();
        double inlierRatio = (double) inliers.size() / estimateCount;
        double countBoost = Math.min(1, config.getQualityCountBase()
                + config.getQualityCountPerTag() * inliers.size());
        double consistency = 1.0 / (1.0 + uncertainty.residualInches / config.getOutlierDistanceInches());
        return clamp(meanWeight * inlierRatio * countBoost * consistency, 0, 1);
    }
}
