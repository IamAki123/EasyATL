package org.firstinspires.ftc.easyatl;

import org.firstinspires.ftc.vision.apriltag.AprilTagDetection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * FTC adapter around {@link EasyATL}.
 *
 * <p>Feed {@code AprilTagProcessor.getDetections()} (or an equivalent list) to
 * {@link #localize(List)} each loop after the vision portal has been updated.</p>
 */
public final class FtcEasyATL {
    private final EasyATL delegate;

    /**
     * Uses {@link EasyATL.Config} defaults.
     *
     * @param camera camera lens pose relative to the robot center
     */
    public FtcEasyATL(EasyATL.CameraConfig camera) {
        this(camera, new EasyATL.Config());
    }

    /**
     * @param camera camera lens pose relative to the robot center
     * @param config pipeline thresholds; copied on construction
     */
    public FtcEasyATL(EasyATL.CameraConfig camera, EasyATL.Config config) {
        delegate = new EasyATL(camera, config);
    }

    /**
     * Registers a field AprilTag. Same as {@link EasyATL#addTag(int, double, double, double)}.
     *
     * @param facing direction out of the printed face toward the camera, radians
     * @return {@code this}
     */
    public FtcEasyATL addTag(int id, double x, double y, double facing) {
        delegate.addTag(id, x, y, facing);
        return this;
    }

    /**
     * Replaces pipeline settings. The object is copied.
     *
     * @return {@code this}
     */
    public FtcEasyATL setConfig(EasyATL.Config config) {
        delegate.setConfig(config);
        return this;
    }

    /** @return a copy of the active pipeline settings */
    public EasyATL.Config getConfig() {
        return delegate.getConfig();
    }

    /**
     * Convenience writer for {@link EasyATL.Config#setMaxRangeInches(double)}.
     *
     * @return {@code this}
     */
    public FtcEasyATL setMaxRangeInches(double value) {
        delegate.setMaxRangeInches(value);
        return this;
    }

    /**
     * Convenience writer for {@link EasyATL.Config#setSmoothingAlpha(double)}.
     *
     * @return {@code this}
     */
    public FtcEasyATL setSmoothingAlpha(double value) {
        delegate.setSmoothingAlpha(value);
        return this;
    }

    /**
     * Converts FTC detections to observations, then runs {@link EasyATL#localize(List)}.
     *
     * <p>Detections with {@code ftcPose == null} are skipped. {@code null} is treated as no
     * detections. Mapping is {@code ftcPose.x → right}, {@code ftcPose.y → forward}.</p>
     *
     * @return {@code true} if a new pose was accepted
     */
    public boolean localize(List<AprilTagDetection> detections) {
        if (detections == null) return delegate.localize(Collections.<EasyATL.Observation>emptyList());
        List<EasyATL.Observation> values = new ArrayList<>();
        for (AprilTagDetection detection : detections) {
            EasyATL.Observation observation = observationOrNull(detection);
            if (observation != null) values.add(observation);
        }
        return delegate.localize(values);
    }

    /** Package-visible mapping used by unit tests. {@code null} if {@code ftcPose} is missing. */
    static EasyATL.Observation observationOrNull(AprilTagDetection detection) {
        if (detection == null || detection.ftcPose == null) return null;
        return new EasyATL.Observation(detection.id, detection.ftcPose.x,
                detection.ftcPose.y, detection.ftcPose.range, detection.ftcPose.bearing,
                detection.ftcPose.yaw);
    }

    /** @see EasyATL#getPose() */
    public FieldPose getPose() { return delegate.getPose(); }
    /** @see EasyATL#hasPose() */
    public boolean hasPose() { return delegate.hasPose(); }
    /** @see EasyATL#getVisibleTags() */
    public List<Integer> getVisibleTags() { return delegate.getVisibleTags(); }
    /** @see EasyATL#getAcceptedTags() */
    public List<Integer> getAcceptedTags() { return delegate.getAcceptedTags(); }
    /** @see EasyATL#getQuality() */
    public double getQuality() { return delegate.getQuality(); }

    /** @deprecated Use {@link #getQuality()}; this is a quality score, not calibrated confidence. */
    @Deprecated public double getConfidence() { return getQuality(); }
}
