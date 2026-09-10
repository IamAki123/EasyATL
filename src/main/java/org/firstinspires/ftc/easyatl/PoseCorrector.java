package org.firstinspires.ftc.easyatl;

/**
 * Applies an EasyATL field pose to a drivetrain localizer. The library has no Pedro or Road Runner
 * dependency; copy-in samples provide those implementations.
 *
 * <p>Call this only after {@code localize()} accepts a pose you trust (and after any quality gate).
 * EasyATL does not replace odometry between tags.</p>
 */
public interface PoseCorrector {
    /**
     * Writes {@code vision} into the drive localizer.
     *
     * @param vision latest accepted EasyATL pose; implementations should no-op on {@code null}
     */
    void apply(FieldPose vision);
}
