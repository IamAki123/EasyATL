package org.firstinspires.ftc.easyatl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

/**
 * Multi-tag AprilTag field-pose localizer.
 *
 * <p>Coordinates follow the common FTC/Pedro convention: heading zero points along field {@code +X},
 * heading is CCW-positive, and distances are inches. Tag facing headings point out of the printed
 * face toward the camera.</p>
 *
 * <p>FTC teams should usually use {@link FtcEasyATL} instead of constructing observations by hand.</p>
 */
public final class EasyATL {
    /**
     * One camera-frame tag measurement. Bearing and yaw are <strong>degrees</strong>; field headings
     * elsewhere in this API are radians.
     */
    public static final class Observation {
        /** AprilTag ID. */
        public final int id;
        /** Tag position in the camera frame, inches to the right of the lens. */
        public final double right;
        /** Tag position in the camera frame, inches in front of the lens. */
        public final double forward;
        /** Distance to the tag, inches. */
        public final double range;
        /** Horizontal bearing of the tag in the camera frame, degrees. */
        public final double bearingDegrees;
        /** Tag yaw relative to the camera, degrees. */
        public final double yawDegrees;
        /** Tag height in the camera frame, inches above the lens. {@code 0} if unused. */
        public final double z;
        /**
         * Detector score (FTC {@code decisionMargin}). Higher is better. {@code 0} or non-finite
         * means unused and does not change the weight.
         */
        public final double decisionMargin;
        /** Frame capture time, {@link System#nanoTime()} basis, or {@code 0} if unknown. */
        public final long captureNanoTime;

        /**
         * @param id AprilTag ID
         * @param right inches right of the lens
         * @param forward inches in front of the lens
         * @param range distance in inches
         * @param bearingDegrees camera-frame bearing, degrees
         * @param yawDegrees tag yaw relative to the camera, degrees
         */
        public Observation(int id, double right, double forward, double range, double bearingDegrees, double yawDegrees) {
            this(id, right, forward, range, bearingDegrees, yawDegrees, 0, 0, 0L);
        }

        /**
         * @param z inches above the lens ({@code ftcPose.z}); used with camera pitch
         * @param decisionMargin FTC decision margin, or {@code 0} if unknown
         * @param captureNanoTime capture time, or {@code 0} if unknown
         */
        public Observation(int id, double right, double forward, double range, double bearingDegrees,
                double yawDegrees, double z, double decisionMargin, long captureNanoTime) {
            this.id = id;
            this.right = right;
            this.forward = forward;
            this.range = range;
            this.bearingDegrees = bearingDegrees;
            this.yawDegrees = yawDegrees;
            this.z = z;
            this.decisionMargin = decisionMargin;
            this.captureNanoTime = captureNanoTime;
        }
    }

    /** Camera lens pose relative to robot center. */
    public static final class CameraConfig {
        /** Inches from robot center toward robot forward. Negative is behind center. */
        public final double forward;
        /** Inches from robot center toward robot right. Negative is left of center. */
        public final double right;
        /** Camera yaw relative to robot forward, radians, CCW-positive. */
        public final double yawRadians;
        /**
         * Camera pitch, radians. Positive tilts the optical axis <strong>up</strong>; negative
         * tilts it down (typical mount). {@code 0} is level with robot forward.
         */
        public final double pitchRadians;
        /** Camera roll about the optical axis, radians. {@code 0} if unused. */
        public final double rollRadians;

        /**
         * Level camera (pitch and roll {@code 0}).
         *
         * @param forward inches forward of robot center
         * @param right inches right of robot center
         * @param yawRadians CCW from robot forward; negative yaws the camera right
         */
        public CameraConfig(double forward, double right, double yawRadians) {
            this(forward, right, yawRadians, 0, 0);
        }

        /**
         * @param pitchRadians positive = camera tilted up; negative = tilted down
         */
        public CameraConfig(double forward, double right, double yawRadians, double pitchRadians) {
            this(forward, right, yawRadians, pitchRadians, 0);
        }

        /**
         * @param rollRadians rotation about the optical axis; {@code 0} for a typical mount
         */
        public CameraConfig(double forward, double right, double yawRadians, double pitchRadians, double rollRadians) {
            requireFinite("forward", forward);
            requireFinite("right", right);
            requireFinite("yawRadians", yawRadians);
            requireFinite("pitchRadians", pitchRadians);
            requireFinite("rollRadians", rollRadians);
            this.forward = forward;
            this.right = right;
            this.yawRadians = yawRadians;
            this.pitchRadians = pitchRadians;
            this.rollRadians = rollRadians;
        }
    }

    /**
     * Observations from one camera, for {@link EasyATL#localizeFromCameras(List)}.
     */
    public static final class CameraObservations {
        public final CameraConfig camera;
        public final List<Observation> observations;

        public CameraObservations(CameraConfig camera, List<Observation> observations) {
            this.camera = requireCamera(camera);
            this.observations = observations == null
                    ? Collections.<Observation>emptyList()
                    : observations;
        }
    }

    /**
     * Per-tag outcome from the latest {@link EasyATL#localize(List)} call.
     */
    public static final class TagDebug {
        public final int id;
        public final double weight;
        public final String rejectReason;
        public final FieldPose pose;

        TagDebug(int id, double weight, String rejectReason, FieldPose pose) {
            this.id = id;
            this.weight = weight;
            this.rejectReason = rejectReason;
            this.pose = pose;
        }

        public boolean accepted() {
            return rejectReason == null;
        }
    }

    /**
     * Rough pose uncertainty from the latest accepted frame. Diagonal only; not a calibrated
     * covariance. {@code null} fields are not used — every field is always set when this object
     * exists.
     */
    public static final class Uncertainty {
        public final double sigmaXInches;
        public final double sigmaYInches;
        public final double sigmaHeadingRadians;
        public final double residualInches;
        public final double residualHeadingRadians;

        Uncertainty(double sigmaXInches, double sigmaYInches, double sigmaHeadingRadians,
                double residualInches, double residualHeadingRadians) {
            this.sigmaXInches = sigmaXInches;
            this.sigmaYInches = sigmaYInches;
            this.sigmaHeadingRadians = sigmaHeadingRadians;
            this.residualInches = residualInches;
            this.residualHeadingRadians = residualHeadingRadians;
        }
    }

    /**
     * Snapshot of the latest fusion attempt: residuals, per-tag weights, and reject reasons.
     */
    public static final class DebugFrame {
        public final boolean accepted;
        public final FieldPose rawMeasurement;
        public final Uncertainty uncertainty;
        public final List<TagDebug> tags;

        DebugFrame(boolean accepted, FieldPose rawMeasurement, Uncertainty uncertainty, List<TagDebug> tags) {
            this.accepted = accepted;
            this.rawMeasurement = rawMeasurement;
            this.uncertainty = uncertainty;
            this.tags = tags;
        }
    }

    /**
     * Tunable localization pipeline. Defaults match the original EasyATL behavior.
     *
     * <p>Copied when passed into a localizer, so later edits to this object do not apply until
     * {@link EasyATL#setConfig(Config)}.</p>
     */
    public static final class Config {
        public static final double DEFAULT_MAX_RANGE_INCHES = 96;
        public static final double DEFAULT_MAX_BEARING_DEGREES = 55;
        public static final double DEFAULT_MAX_TAG_YAW_DEGREES = 45;
        public static final double DEFAULT_OUTLIER_DISTANCE_INCHES = 12;
        public static final double DEFAULT_OUTLIER_HEADING_DEGREES = 25;
        public static final double DEFAULT_SMOOTHING_ALPHA = 0.65;
        public static final double DEFAULT_QUALITY_DECAY_RATE = 0.8;
        public static final double DEFAULT_WEIGHT_RANGE_SCALE_INCHES = 36;
        public static final double DEFAULT_DECISION_MARGIN_SCALE = 50;
        public static final double DEFAULT_MIN_WEIGHT = 0.05;

        private double maxRangeInches = DEFAULT_MAX_RANGE_INCHES;
        private double maxBearingDegrees = DEFAULT_MAX_BEARING_DEGREES;
        private double maxTagYawDegrees = DEFAULT_MAX_TAG_YAW_DEGREES;
        private double outlierDistanceInches = DEFAULT_OUTLIER_DISTANCE_INCHES;
        private double outlierHeadingDegrees = DEFAULT_OUTLIER_HEADING_DEGREES;
        private double smoothingAlpha = DEFAULT_SMOOTHING_ALPHA;
        private double qualityDecayRate = DEFAULT_QUALITY_DECAY_RATE;
        private double weightRangeScaleInches = DEFAULT_WEIGHT_RANGE_SCALE_INCHES;
        private double decisionMarginScale = DEFAULT_DECISION_MARGIN_SCALE;
        private double minWeight = DEFAULT_MIN_WEIGHT;
        private long maxObservationAgeMs;
        private double maxStepInches;
        private double maxStepDegrees;

        /**
         * Rejects detections farther than this range.
         *
         * @param value inches; must be finite and {@code > 0}
         * @return {@code this}
         */
        public Config setMaxRangeInches(double value) {
            maxRangeInches = requirePositive("max range", value);
            return this;
        }

        /**
         * Rejects detections whose absolute bearing exceeds this limit.
         *
         * @param value degrees; must be finite and {@code >= 0}
         * @return {@code this}
         */
        public Config setMaxBearingDegrees(double value) {
            maxBearingDegrees = requireNonNegative("max bearing", value);
            return this;
        }

        /**
         * Rejects detections whose absolute tag yaw exceeds this limit.
         *
         * @param value degrees; must be finite and {@code >= 0}
         * @return {@code this}
         */
        public Config setMaxTagYawDegrees(double value) {
            maxTagYawDegrees = requireNonNegative("max tag yaw", value);
            return this;
        }

        /**
         * Drops multi-tag estimates farther than this from the robust consensus.
         *
         * @param value inches; must be finite and {@code > 0}
         * @return {@code this}
         */
        public Config setOutlierDistanceInches(double value) {
            outlierDistanceInches = requirePositive("outlier distance", value);
            return this;
        }

        /**
         * Drops multi-tag estimates whose heading disagrees with the consensus by more than this.
         *
         * @param value degrees; must be finite and {@code > 0}
         * @return {@code this}
         */
        public Config setOutlierHeadingDegrees(double value) {
            outlierHeadingDegrees = requirePositive("outlier heading", value);
            return this;
        }

        /**
         * Blend of a new accepted pose versus the previous pose. {@code 1.0} disables smoothing.
         *
         * @param value clamped to {@code [0, 1]}
         * @return {@code this}
         */
        public Config setSmoothingAlpha(double value) {
            requireFinite("smoothing alpha", value);
            smoothingAlpha = clamp(value, 0, 1);
            return this;
        }

        /**
         * Exponential quality decay per second while no new pose is accepted. {@code 0} freezes quality.
         *
         * @param value must be finite and {@code >= 0}
         * @return {@code this}
         */
        public Config setQualityDecayRate(double value) {
            qualityDecayRate = requireNonNegative("quality decay rate", value);
            return this;
        }

        /**
         * Range scale in the weight {@code 1 / (1 + (range / scale)²)}. Default {@code 36} in.
         * Larger scale keeps far tags heavier (wide-angle / good calibration); smaller scale
         * prefers close tags.
         *
         * @param value inches; must be finite and {@code > 0}
         * @return {@code this}
         */
        public Config setWeightRangeScaleInches(double value) {
            weightRangeScaleInches = requirePositive("weight range scale", value);
            return this;
        }

        /**
         * Decision-margin value treated as full weight. Margin {@code 0} or non-finite is ignored.
         * Weight is multiplied by {@code clamp(margin / scale, 0, 1)}.
         *
         * @param value must be finite and {@code > 0}
         * @return {@code this}
         */
        public Config setDecisionMarginScale(double value) {
            decisionMarginScale = requirePositive("decision margin scale", value);
            return this;
        }

        /**
         * Floor on per-tag weight after range, angle, and margin terms.
         *
         * @param value clamped to {@code [0, 1]}
         * @return {@code this}
         */
        public Config setMinWeight(double value) {
            requireFinite("min weight", value);
            minWeight = clamp(value, 0, 1);
            return this;
        }

        /**
         * Ignore a frame if its capture timestamp is older than this. {@code 0} (default) disables
         * the check. Requires a non-zero {@code captureNanoTime} on observations or the
         * {@link EasyATL#localize(List, long)} argument.
         *
         * @param value milliseconds; must be finite and {@code >= 0}
         * @return {@code this}
         */
        public Config setMaxObservationAgeMs(double value) {
            maxObservationAgeMs = (long) requireNonNegative("max observation age", value);
            return this;
        }

        /**
         * Limits how far one accepted frame may move XY from the previous pose. {@code 0}
         * (default) is unlimited. Applied before smoothing. First pose is never clamped.
         *
         * @param value inches; must be finite and {@code >= 0}
         * @return {@code this}
         */
        public Config setMaxStepInches(double value) {
            maxStepInches = requireNonNegative("max step inches", value);
            return this;
        }

        /**
         * Limits how far one accepted frame may change heading from the previous pose. {@code 0}
         * (default) is unlimited. Applied before smoothing. First pose is never clamped.
         *
         * @param value degrees; must be finite and {@code >= 0}
         * @return {@code this}
         */
        public Config setMaxStepDegrees(double value) {
            maxStepDegrees = requireNonNegative("max step degrees", value);
            return this;
        }

        /** @return maximum accepted range, inches */
        public double getMaxRangeInches() { return maxRangeInches; }
        /** @return maximum accepted absolute bearing, degrees */
        public double getMaxBearingDegrees() { return maxBearingDegrees; }
        /** @return maximum accepted absolute tag yaw, degrees */
        public double getMaxTagYawDegrees() { return maxTagYawDegrees; }
        /** @return XY outlier threshold, inches */
        public double getOutlierDistanceInches() { return outlierDistanceInches; }
        /** @return heading outlier threshold, degrees */
        public double getOutlierHeadingDegrees() { return outlierHeadingDegrees; }
        /** @return pose blend factor in {@code [0, 1]} */
        public double getSmoothingAlpha() { return smoothingAlpha; }
        /** @return quality decay rate per second */
        public double getQualityDecayRate() { return qualityDecayRate; }
        /** @return range scale used in per-tag weights, inches */
        public double getWeightRangeScaleInches() { return weightRangeScaleInches; }
        /** @return decision-margin value treated as full weight */
        public double getDecisionMarginScale() { return decisionMarginScale; }
        /** @return minimum per-tag weight */
        public double getMinWeight() { return minWeight; }
        /** @return max observation age, milliseconds; {@code 0} disables */
        public double getMaxObservationAgeMs() { return maxObservationAgeMs; }
        /** @return max XY step per frame, inches; {@code 0} unlimited */
        public double getMaxStepInches() { return maxStepInches; }
        /** @return max heading step per frame, degrees; {@code 0} unlimited */
        public double getMaxStepDegrees() { return maxStepDegrees; }

        /** @return an independent copy of these settings */
        public Config copy() {
            return new Config()
                    .setMaxRangeInches(maxRangeInches)
                    .setMaxBearingDegrees(maxBearingDegrees)
                    .setMaxTagYawDegrees(maxTagYawDegrees)
                    .setOutlierDistanceInches(outlierDistanceInches)
                    .setOutlierHeadingDegrees(outlierHeadingDegrees)
                    .setSmoothingAlpha(smoothingAlpha)
                    .setQualityDecayRate(qualityDecayRate)
                    .setWeightRangeScaleInches(weightRangeScaleInches)
                    .setDecisionMarginScale(decisionMarginScale)
                    .setMinWeight(minWeight)
                    .setMaxObservationAgeMs(maxObservationAgeMs)
                    .setMaxStepInches(maxStepInches)
                    .setMaxStepDegrees(maxStepDegrees);
        }
    }

    private static final class Tag { final double x,y,facing; Tag(double x,double y,double facing){this.x=x;this.y=y;this.facing=facing;} }
    private static final class Estimate { final FieldPose pose; final double weight; final int id; Estimate(FieldPose p,double w,int i){pose=p;weight=w;id=i;} }

    private final Map<Integer, Tag> tags = new HashMap<>();
    private CameraConfig camera;
    private Config config;
    private FieldPose pose;
    private List<Integer> visibleTags=Collections.emptyList(), acceptedTags=Collections.emptyList();
    private double confidence;
    private long lastConfidenceTime;
    private boolean enabled = true;
    private DebugFrame lastDebug = new DebugFrame(false, null, null, Collections.<TagDebug>emptyList());
    private Uncertainty lastUncertainty;
    private LongSupplier nanoTime = new LongSupplier() {
        @Override public long getAsLong() { return System.nanoTime(); }
    };

    /**
     * Fuses AprilTag observations into a field-relative robot pose.
     *
     * <p>Supports multiple simultaneous tags, robust outlier rejection, pose smoothing, and
     * heuristic quality scoring. Uses {@link Config} defaults.</p>
     *
     * @param camera camera lens pose relative to the robot center
     */
    public EasyATL(CameraConfig camera) { this(camera, new Config()); }

    /**
     * @param camera camera lens pose relative to the robot center
     * @param config pipeline thresholds; copied on construction
     */
    public EasyATL(CameraConfig camera, Config config) {
        this.camera=requireCamera(camera);
        this.config=requireConfig(config).copy();
    }

    /**
     * Registers a field AprilTag. Re-adding the same ID overwrites the previous pose.
     *
     * @param id AprilTag ID
     * @param x tag center field X, inches
     * @param y tag center field Y, inches
     * @param facingHeadingRadians direction out of the printed face toward the camera, radians, CCW-positive
     * @return {@code this}
     */
    public EasyATL addTag(int id,double x,double y,double facingHeadingRadians) {
        requireFinite("field x", x); requireFinite("field y", y); requireFinite("facing heading", facingHeadingRadians);
        tags.put(id,new Tag(x,y,facingHeadingRadians)); return this;
    }

    /**
     * Registers every tag in a field set (merges; same ID overwrites).
     *
     * @return {@code this}
     */
    public EasyATL addTags(FieldTags fieldTags) {
        if (fieldTags == null) throw new IllegalArgumentException("field tags cannot be null");
        fieldTags.apply(this);
        return this;
    }

    /**
     * Newest bundled season ({@link FieldTags#latest()}). Replaces any tags already registered.
     *
     * @return {@code this}
     */
    public EasyATL useLatestSeason() {
        return setTags(FieldTags.latest());
    }

    /**
     * Official table for a bundled season. Replaces any tags already registered.
     *
     * @return {@code this}
     */
    public EasyATL useSeason(FieldTags.Season season) {
        return setTags(FieldTags.season(season));
    }

    /**
     * Replaces the tag map with this set.
     *
     * @return {@code this}
     */
    public EasyATL setTags(FieldTags fieldTags) {
        clearTags();
        return addTags(fieldTags);
    }

    /**
     * Removes every registered tag. Camera and config are kept.
     *
     * @return {@code this}
     */
    public EasyATL clearTags() {
        tags.clear();
        return this;
    }

    /**
     * Replaces the camera mount after construction.
     *
     * @param value new lens pose
     * @return {@code this}
     */
    public EasyATL setCameraConfig(CameraConfig value) { camera=requireCamera(value); return this; }

    /**
     * Replaces pipeline settings. The object is copied.
     *
     * @param value new config
     * @return {@code this}
     */
    public EasyATL setConfig(Config value) { config=requireConfig(value).copy(); return this; }

    /** @return a copy of the active pipeline settings */
    public Config getConfig() { return config.copy(); }

    /**
     * Convenience writer for {@link Config#setMaxRangeInches(double)}.
     *
     * @return {@code this}
     */
    public EasyATL setMaxRangeInches(double value) { config.setMaxRangeInches(value); return this; }

    /**
     * Convenience writer for {@link Config#setSmoothingAlpha(double)}.
     *
     * @return {@code this}
     */
    public EasyATL setSmoothingAlpha(double value) { config.setSmoothingAlpha(value); return this; }

    /**
     * When {@code false}, {@code localize} will not accept a new pose. Visible-tag telemetry is
     * still updated. Quality continues to decay.
     *
     * @return {@code this}
     */
    public EasyATL setEnabled(boolean value) {
        enabled = value;
        return this;
    }

    /** @return {@code false} if acceptance is frozen */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Clears the last pose, quality, and debug snapshot. Tag configuration is kept.
     *
     * @return {@code this}
     */
    public EasyATL reset() {
        pose = null;
        confidence = 0;
        lastConfidenceTime = 0;
        visibleTags = Collections.emptyList();
        acceptedTags = Collections.emptyList();
        lastDebug = new DebugFrame(false, null, null, Collections.<TagDebug>emptyList());
        lastUncertainty = null;
        return this;
    }

    /**
     * Seeds the smoothed pose (for example after a known start). Quality is set to {@code 1}.
     *
     * @return {@code this}
     */
    public EasyATL setPose(FieldPose value) {
        if (value == null) throw new IllegalArgumentException("pose cannot be null");
        requireFinite("pose x", value.x);
        requireFinite("pose y", value.y);
        requireFinite("pose heading", value.heading);
        pose = value;
        confidence = 1;
        lastConfidenceTime = nanoTime.getAsLong();
        return this;
    }

    /**
     * Fuses configured, in-range, consistent tags from this frame using the constructor camera.
     *
     * <p>{@code null} is treated as an empty list. Unconfigured IDs are ignored. On failure the last
     * pose is kept and {@link #getQuality()} begins decaying.</p>
     *
     * @param observations camera-frame detections for this frame
     * @return {@code true} if a new pose was accepted
     */
    public boolean localize(List<Observation> observations) {
        return localize(observations, 0L);
    }

    /**
     * Same as {@link #localize(List)} with an optional frame timestamp for
     * {@link Config#setMaxObservationAgeMs(double)}.
     *
     * @param frameCaptureNanoTime {@link System#nanoTime()} capture time, or {@code 0} to use
     *        per-observation timestamps when present
     */
    public boolean localize(List<Observation> observations, long frameCaptureNanoTime) {
        List<CameraObservations> frames = new ArrayList<>();
        frames.add(new CameraObservations(camera, observations));
        return localizeFromCameras(frames, frameCaptureNanoTime);
    }

    /**
     * Fuses observations from more than one camera in a single call. Each group is transformed
     * with its own {@link CameraConfig}, then combined with the same medoid / outlier / mean
     * pipeline as {@link #localize(List)}.
     */
    public boolean localizeFromCameras(List<CameraObservations> frames) {
        return localizeFromCameras(frames, 0L);
    }

    /**
     * @param frameCaptureNanoTime shared capture time, or {@code 0} to use observation timestamps
     */
    public boolean localizeFromCameras(List<CameraObservations> frames, long frameCaptureNanoTime) {
        long now = nanoTime.getAsLong();
        decayConfidence(now);
        List<TagDebug> reports = new ArrayList<>();
        List<Integer> visible = new ArrayList<>();
        List<Estimate> estimates = new ArrayList<>();

        if (frames == null) frames = Collections.emptyList();
        long newestCapture = frameCaptureNanoTime;
        for (CameraObservations frame : frames) {
            if (frame == null) continue;
            List<Observation> observations = frame.observations;
            if (observations == null) continue;
            for (Observation o : observations) {
                if (o.captureNanoTime > newestCapture) newestCapture = o.captureNanoTime;
                Tag tag = tags.get(o.id);
                if (tag == null) continue;
                visible.add(o.id);
                String reason = rejectReason(o);
                if (reason != null) {
                    reports.add(new TagDebug(o.id, 0, reason, null));
                    continue;
                }
                FieldPose tagPose = toPose(frame.camera, tag, o);
                double w = weight(o);
                estimates.add(new Estimate(tagPose, w, o.id));
                reports.add(new TagDebug(o.id, w, null, tagPose));
            }
        }
        visibleTags = Collections.unmodifiableList(visible);

        if (!enabled) {
            acceptedTags = Collections.emptyList();
            lastDebug = new DebugFrame(false, null, lastUncertainty, freezeReasons(reports, "disabled"));
            return false;
        }
        if (stale(newestCapture, now)) {
            acceptedTags = Collections.emptyList();
            lastDebug = new DebugFrame(false, null, lastUncertainty, freezeReasons(reports, "stale"));
            return false;
        }
        if (estimates.isEmpty()) {
            acceptedTags = Collections.emptyList();
            lastDebug = new DebugFrame(false, null, lastUncertainty, Collections.unmodifiableList(reports));
            return false;
        }

        FieldPose center = robustCenter(estimates);
        List<Estimate> inliers = new ArrayList<>();
        double outlierHeadingRadians = Math.toRadians(config.getOutlierHeadingDegrees());
        for (Estimate e : estimates) {
            boolean in = Math.hypot(e.pose.x - center.x, e.pose.y - center.y) <= config.getOutlierDistanceInches()
                    && Math.abs(wrap(e.pose.heading - center.heading)) <= outlierHeadingRadians;
            if (in) {
                inliers.add(e);
            } else {
                markRejected(reports, e.id, "outlier");
            }
        }
        if (inliers.isEmpty()) {
            acceptedTags = Collections.emptyList();
            lastDebug = new DebugFrame(false, null, lastUncertainty, Collections.unmodifiableList(reports));
            return false;
        }

        FieldPose measurement = mean(inliers, center);
        measurement = limitStep(pose, measurement);
        pose = pose == null ? measurement : blend(pose, measurement, config.getSmoothingAlpha());
        List<Integer> ids = new ArrayList<>();
        double quality = 0;
        for (Estimate e : inliers) {
            ids.add(e.id);
            quality += e.weight;
        }
        acceptedTags = Collections.unmodifiableList(ids);
        quality /= inliers.size();
        Uncertainty uncertainty = uncertainty(inliers, measurement);
        lastUncertainty = uncertainty;
        double consistency = 1.0 / (1.0 + uncertainty.residualInches / config.getOutlierDistanceInches());
        confidence = clamp(quality * ((double) inliers.size() / estimates.size())
                * Math.min(1, 0.75 + 0.125 * inliers.size()) * consistency, 0, 1);
        lastConfidenceTime = now;
        lastDebug = new DebugFrame(true, measurement, uncertainty, Collections.unmodifiableList(reports));
        return true;
    }

    /**
     * Latest smoothed vision pose, or {@code null} if none has been accepted yet.
     * Frozen after tags are lost until the next accepted frame.
     */
    public FieldPose getPose(){return pose;}

    /** @return {@code true} after at least one pose has been accepted */
    public boolean hasPose(){return pose!=null;}

    /**
     * Configured tag IDs seen in the latest {@link #localize(List)} call, including those later
     * rejected by range, angle, or outlier checks.
     */
    public List<Integer> getVisibleTags(){return visibleTags;}

    /**
     * Tag IDs that contributed to the pose on the latest {@link #localize(List)} call.
     * Empty when that call returned {@code false}.
     */
    public List<Integer> getAcceptedTags(){return acceptedTags;}

    /**
     * Heuristic measurement quality in {@code [0, 1]}. Not a probability; decays while no new pose
     * is accepted. Read after {@link #localize(List)}.
     */
    public double getQuality(){decayConfidence(nanoTime.getAsLong());return confidence;}

    /** @deprecated Use {@link #getQuality()}; this is a quality score, not calibrated confidence. */
    @Deprecated public double getConfidence(){return getQuality();}

    /**
     * Latest fusion debug snapshot (per-tag weights, reject reasons, residual). Always non-null;
     * {@link DebugFrame#rawMeasurement} is null until a pose has been accepted on some call.
     */
    public DebugFrame getDebug() {
        return lastDebug;
    }

    /**
     * Rough XY/heading uncertainty from the last accepted frame, or {@code null} if none.
     */
    public Uncertainty getUncertainty() {
        return lastUncertainty;
    }

    /** Test hook: replace {@link System#nanoTime()} so quality decay can be simulated. */
    void setNanoTimeSource(LongSupplier source) {
        if (source == null) throw new IllegalArgumentException("nano time source cannot be null");
        nanoTime = source;
    }

    /** Test helper that advances a mutable clock. */
    static LongSupplier advancingNanoTime(final AtomicLong nanos) {
        return new LongSupplier() {
            @Override public long getAsLong() { return nanos.get(); }
        };
    }

    private String rejectReason(Observation o) {
        if (!Double.isFinite(o.right) || !Double.isFinite(o.forward) || !Double.isFinite(o.range)
                || !Double.isFinite(o.bearingDegrees) || !Double.isFinite(o.yawDegrees)
                || !Double.isFinite(o.z)) {
            return "non-finite";
        }
        if (o.range <= 0 || o.range > config.getMaxRangeInches()) return "range";
        if (Math.abs(o.bearingDegrees) > config.getMaxBearingDegrees()) return "bearing";
        if (Math.abs(o.yawDegrees) > config.getMaxTagYawDegrees()) return "yaw";
        return null;
    }

    private boolean stale(long captureNanoTime, long now) {
        if (config.getMaxObservationAgeMs() <= 0 || captureNanoTime <= 0) return false;
        long ageMs = (now - captureNanoTime) / 1_000_000L;
        return ageMs > (long) config.getMaxObservationAgeMs();
    }

    private FieldPose toPose(CameraConfig cam, Tag tag, Observation o) {
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
        double h = wrap(tag.facing + Math.PI - Math.toRadians(o.yawDegrees) - cam.yawRadians);
        return new FieldPose(tag.x - (f * Math.cos(h) + r * Math.sin(h)),
                tag.y - (f * Math.sin(h) - r * Math.cos(h)), h);
    }

    private double weight(Observation o) {
        double scale = config.getWeightRangeScaleInches();
        double range = 1 / (1 + Math.pow(o.range / scale, 2));
        double angle = Math.cos(Math.toRadians(o.bearingDegrees)) * Math.cos(Math.toRadians(o.yawDegrees));
        double margin = 1;
        if (Double.isFinite(o.decisionMargin) && o.decisionMargin > 0) {
            margin = clamp(o.decisionMargin / config.getDecisionMarginScale(), 0, 1);
        }
        return Math.max(config.getMinWeight(), range * Math.max(0, angle) * margin);
    }

    private FieldPose mean(List<Estimate> values, FieldPose center) {
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

    private static double huber(double residual, double scale) {
        if (scale <= 0) return 1;
        double u = residual / scale;
        if (u <= 1) return 1;
        return 1 / u;
    }

    private FieldPose robustCenter(List<Estimate> values){
        Estimate best=null; double bestCost=Double.POSITIVE_INFINITY;
        double outlierHeadingRadians=Math.toRadians(config.getOutlierHeadingDegrees());
        for(Estimate candidate:values){
            double cost=0;
            for(Estimate other:values) cost+=Math.min(1,Math.hypot(candidate.pose.x-other.pose.x,candidate.pose.y-other.pose.y)/config.getOutlierDistanceInches())+Math.min(1,Math.abs(wrap(candidate.pose.heading-other.pose.heading))/outlierHeadingRadians);
            if(cost<bestCost || (cost==bestCost && (best==null || candidate.weight>best.weight))){best=candidate;bestCost=cost;}
        }
        return best.pose;
    }

    private FieldPose limitStep(FieldPose oldPose, FieldPose measurement) {
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

    private static Uncertainty uncertainty(List<Estimate> inliers, FieldPose measurement) {
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
        return new Uncertainty(sigmaXY, sigmaXY, sigmaH, residualInches, residualHeading);
    }

    private static void markRejected(List<TagDebug> reports, int id, String reason) {
        for (int i = 0; i < reports.size(); i++) {
            TagDebug t = reports.get(i);
            if (t.id == id && t.rejectReason == null) {
                reports.set(i, new TagDebug(t.id, t.weight, reason, t.pose));
                return;
            }
        }
    }

    private static List<TagDebug> freezeReasons(List<TagDebug> reports, String reason) {
        List<TagDebug> out = new ArrayList<>();
        for (TagDebug t : reports) {
            if (t.rejectReason == null) out.add(new TagDebug(t.id, t.weight, reason, t.pose));
            else out.add(t);
        }
        return Collections.unmodifiableList(out);
    }

    private static FieldPose blend(FieldPose oldPose,FieldPose newPose,double a){return new FieldPose(oldPose.x+a*(newPose.x-oldPose.x),oldPose.y+a*(newPose.y-oldPose.y),wrap(oldPose.heading+a*wrap(newPose.heading-oldPose.heading)));}
    private void decayConfidence(long now){if(lastConfidenceTime==0)return;confidence*=Math.exp(-config.getQualityDecayRate()*Math.max(0,now-lastConfidenceTime)/1e9);lastConfidenceTime=now;}
    static double wrap(double a){while(a>Math.PI)a-=2*Math.PI;while(a<=-Math.PI)a+=2*Math.PI;return a;}
    private static double clamp(double v,double min,double max){return Math.max(min,Math.min(max,v));}
    private static CameraConfig requireCamera(CameraConfig value){if(value==null)throw new IllegalArgumentException("camera config cannot be null");return value;}
    private static Config requireConfig(Config value){if(value==null)throw new IllegalArgumentException("config cannot be null");return value;}
    private static void requireFinite(String name,double value){if(!Double.isFinite(value))throw new IllegalArgumentException(name+" must be finite");}
    private static double requirePositive(String name,double value){
        if(!Double.isFinite(value)||value<=0) throw new IllegalArgumentException(name+" must be finite and positive");
        return value;
    }
    private static double requireNonNegative(String name,double value){
        if(!Double.isFinite(value)||value<0) throw new IllegalArgumentException(name+" must be finite and non-negative");
        return value;
    }
}
