package org.firstinspires.ftc.easyatl;

import org.firstinspires.ftc.robotcore.external.matrices.VectorF;
import org.firstinspires.ftc.robotcore.external.navigation.Quaternion;
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection;
import org.firstinspires.ftc.vision.apriltag.AprilTagGameDatabase;
import org.firstinspires.ftc.vision.apriltag.AprilTagLibrary;
import org.firstinspires.ftc.vision.apriltag.AprilTagMetadata;

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
     * Library defaults: lens at robot center, {@link EasyATL.Config} defaults, newest bundled
     * season tags. Same as {@link DefaultSdkConstants#createLocalizer()}.
     */
    public FtcEasyATL() {
        this(DefaultSdkConstants.camera(), DefaultSdkConstants.config());
        DefaultSdkConstants.addTags(this);
    }

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
     * Camera, pipeline, and a season tag map in one call.
     *
     * @param tags typically {@link FieldTags#latest()} or a set from {@link FieldTags#custom(String)}
     */
    public FtcEasyATL(EasyATL.CameraConfig camera, EasyATL.Config config, FieldTags tags) {
        this(camera, config);
        addTags(tags);
    }

    /**
     * Newest bundled season ({@link FieldTags#latest()}). Same as
     * {@code new FtcEasyATL(camera, config).useLatestSeason()}.
     */
    public FtcEasyATL(EasyATL.CameraConfig camera, EasyATL.Config config, FieldTags.Season season) {
        this(camera, config);
        useSeason(season);
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
     * Registers every tag in a hardcoded season map.
     *
     * @return {@code this}
     */
    public FtcEasyATL addTags(FieldTags fieldTags) {
        delegate.addTags(fieldTags);
        return this;
    }

    /**
     * Newest bundled season (DECODE 2025–26). Replaces tags already registered.
     *
     * @return {@code this}
     */
    public FtcEasyATL useLatestSeason() {
        delegate.useLatestSeason();
        return this;
    }

    /**
     * Official table for a past or current bundled season. Replaces tags already registered.
     *
     * <p>Example: {@code useSeason(FieldTags.Season.INTO_THE_DEEP)}</p>
     *
     * @return {@code this}
     */
    public FtcEasyATL useSeason(FieldTags.Season season) {
        delegate.useSeason(season);
        return this;
    }

    /**
     * Replaces the tag map (custom practice field, or a set from {@code customAprilTags()}).
     *
     * @return {@code this}
     */
    public FtcEasyATL useFieldSet(FieldTags fieldTags) {
        delegate.setTags(fieldTags);
        return this;
    }

    /**
     * Removes every registered tag.
     *
     * @return {@code this}
     */
    public FtcEasyATL clearTags() {
        delegate.clearTags();
        return this;
    }

    /**
     * Loads official field poses from the <em>installed</em> FTC SDK
     * ({@code AprilTagGameDatabase.getCurrentGameTagLibrary()}). Skips tags that have no
     * field position (for example motif/obelisk tags with no fixed pose).
     *
     * <p>Prefer this over typing IDs by hand. Still tape-verify; SDK metadata tracks FIRST's
     * published CAD, not your specific event's zip ties.</p>
     *
     * @return {@code this}
     */
    public FtcEasyATL addCurrentGameTags() {
        return addTags(AprilTagGameDatabase.getCurrentGameTagLibrary());
    }

    /**
     * Registers tags that have a field pose in an SDK {@link AprilTagLibrary}.
     *
     * @return {@code this}
     */
    public FtcEasyATL addTags(AprilTagLibrary library) {
        if (library == null) throw new IllegalArgumentException("tag library cannot be null");
        AprilTagMetadata[] all = library.getAllTags();
        if (all == null) return this;
        for (AprilTagMetadata metadata : all) {
            if (metadata == null || metadata.fieldPosition == null) continue;
            VectorF p = metadata.fieldPosition;
            if (p.length() < 2) continue;
            addTag(metadata.id, p.get(0), p.get(1), facingRadians(metadata.fieldOrientation));
        }
        return this;
    }

    /**
     * Replaces the camera mount after construction.
     *
     * @return {@code this}
     */
    public FtcEasyATL setCameraConfig(EasyATL.CameraConfig value) {
        delegate.setCameraConfig(value);
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

    /** @see EasyATL#setEnabled(boolean) */
    public FtcEasyATL setEnabled(boolean value) {
        delegate.setEnabled(value);
        return this;
    }

    /** @see EasyATL#isEnabled() */
    public boolean isEnabled() {
        return delegate.isEnabled();
    }

    /** @see EasyATL#reset() */
    public FtcEasyATL reset() {
        delegate.reset();
        return this;
    }

    /** @see EasyATL#setPose(FieldPose) */
    public FtcEasyATL setPose(FieldPose value) {
        delegate.setPose(value);
        return this;
    }

    /**
     * Converts FTC detections to observations, then runs {@link EasyATL#localize(List)}.
     *
     * <p>Detections with {@code ftcPose == null} are skipped. {@code null} is treated as no
     * detections. Mapping is {@code ftcPose.x → right}, {@code ftcPose.y → forward}, plus
     * {@code z}, {@code decisionMargin}, and {@code frameAcquisitionNanoTime} when present.</p>
     *
     * @return {@code true} if a new pose was accepted
     */
    public boolean localize(List<AprilTagDetection> detections) {
        if (detections == null) return delegate.localize(Collections.<EasyATL.Observation>emptyList());
        List<EasyATL.Observation> values = new ArrayList<>();
        long newest = 0L;
        for (AprilTagDetection detection : detections) {
            EasyATL.Observation observation = observationOrNull(detection);
            if (observation != null) {
                values.add(observation);
                if (observation.captureNanoTime > newest) newest = observation.captureNanoTime;
            }
        }
        return delegate.localize(values, newest);
    }

    /** Package-visible mapping used by unit tests. {@code null} if {@code ftcPose} is missing. */
    static EasyATL.Observation observationOrNull(AprilTagDetection detection) {
        if (detection == null || detection.ftcPose == null) return null;
        return new EasyATL.Observation(detection.id, detection.ftcPose.x,
                detection.ftcPose.y, detection.ftcPose.range, detection.ftcPose.bearing,
                detection.ftcPose.yaw, detection.ftcPose.z, detection.decisionMargin,
                detection.frameAcquisitionNanoTime);
    }

    /**
     * Heading of the tag face normal (tag local +Z) projected onto the field XY plane.
     * Identity orientation (tag facing up) yields {@code 0}.
     */
    static double facingRadians(Quaternion orientation) {
        if (orientation == null) return 0;
        double[] n = rotate(orientation, 0, 0, 1);
        if (Math.hypot(n[0], n[1]) < 1e-6) return 0;
        return Math.atan2(n[1], n[0]);
    }

    private static double[] rotate(Quaternion q, double vx, double vy, double vz) {
        double cx = q.y * vz - q.z * vy;
        double cy = q.z * vx - q.x * vz;
        double cz = q.x * vy - q.y * vx;
        cx *= 2;
        cy *= 2;
        cz *= 2;
        return new double[] {
                vx + q.w * cx + (q.y * cz - q.z * cy),
                vy + q.w * cy + (q.z * cx - q.x * cz),
                vz + q.w * cz + (q.x * cy - q.y * cx)
        };
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
    /** @see EasyATL#getDebug() */
    public EasyATL.DebugFrame getDebug() { return delegate.getDebug(); }
    /** @see EasyATL#getUncertainty() */
    public EasyATL.Uncertainty getUncertainty() { return delegate.getUncertainty(); }

    /** @deprecated Use {@link #getQuality()}; this is a quality score, not calibrated confidence. */
    @Deprecated public double getConfidence() { return getQuality(); }
}
