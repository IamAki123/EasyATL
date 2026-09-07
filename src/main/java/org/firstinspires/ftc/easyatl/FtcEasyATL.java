package org.firstinspires.ftc.easyatl;

import org.firstinspires.ftc.vision.apriltag.AprilTagDetection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** FTC SDK adapter. Feed {@code AprilTagProcessor.getDetections()} to {@link #localize(List)} each loop. */
public final class FtcEasyATL {
    private final EasyATL delegate;

    public FtcEasyATL(EasyATL.CameraConfig camera) {
        delegate = new EasyATL(camera);
    }

    public FtcEasyATL addTag(int id, double x, double y, double facing) {
        delegate.addTag(id, x, y, facing);
        return this;
    }

    public FtcEasyATL setMaxRangeInches(double value) {
        delegate.setMaxRangeInches(value);
        return this;
    }

    public FtcEasyATL setSmoothingAlpha(double value) {
        delegate.setSmoothingAlpha(value);
        return this;
    }

    public boolean localize(List<AprilTagDetection> detections) {
        if (detections == null) return delegate.localize(Collections.<EasyATL.Observation>emptyList());
        List<EasyATL.Observation> values = new ArrayList<>();
        for (AprilTagDetection detection : detections) {
            if (detection.ftcPose == null) continue;
            values.add(new EasyATL.Observation(detection.id, detection.ftcPose.x,
                    detection.ftcPose.y, detection.ftcPose.range, detection.ftcPose.bearing,
                    detection.ftcPose.yaw));
        }
        return delegate.localize(values);
    }

    public FieldPose getPose() { return delegate.getPose(); }
    public boolean hasPose() { return delegate.hasPose(); }
    public List<Integer> getVisibleTags() { return delegate.getVisibleTags(); }
    public List<Integer> getAcceptedTags() { return delegate.getAcceptedTags(); }
    public double getQuality() { return delegate.getQuality(); }
    /** @deprecated Use {@link #getQuality()}; this is a quality score, not calibrated confidence. */
    @Deprecated public double getConfidence() { return getQuality(); }
}
