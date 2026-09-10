package org.firstinspires.ftc.teamcode.easyatl;

import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.Pose;

import org.firstinspires.ftc.easyatl.FieldPose;
import org.firstinspires.ftc.easyatl.PoseCorrector;

/**
 * Pedro {@link PoseCorrector}. Copy into TeamCode with the other Pedro EasyATL files.
 * Not in the EasyATL AAR.
 */
public final class PedroPoseCorrector implements PoseCorrector {
    private final Follower follower;

    public PedroPoseCorrector(Follower follower) {
        if (follower == null) throw new IllegalArgumentException("follower cannot be null");
        this.follower = follower;
    }

    @Override
    public void apply(FieldPose vision) {
        if (vision == null) return;
        follower.setPose(new Pose(vision.x, vision.y, vision.heading));
    }
}
