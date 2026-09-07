package org.firstinspires.ftc.easyatl;

import org.firstinspires.ftc.vision.apriltag.AprilTagDetection;
import org.firstinspires.ftc.vision.apriltag.AprilTagPoseFtc;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class FtcEasyATLTest {
    private static AprilTagPoseFtc pose(double x, double y, double range, double bearing, double yaw) {
        return new AprilTagPoseFtc(x, y, 0, yaw, 0, 0, range, bearing, 0);
    }

    private static AprilTagDetection detection(int id, AprilTagPoseFtc ftcPose) {
        return new AprilTagDetection(id, 0, 0, null, null, null, ftcPose, null, null, 0L);
    }

    @Test
    public void mapsFtcPoseAxesToObservation() {
        AprilTagDetection d = detection(21, pose(3.0, 40.0, 40.2, 4.0, -2.0));
        EasyATL.Observation o = FtcEasyATL.observationOrNull(d);
        assertEquals(21, o.id);
        assertEquals(3.0, o.right, 0.0);
        assertEquals(40.0, o.forward, 0.0);
        assertEquals(40.2, o.range, 0.0);
        assertEquals(4.0, o.bearingDegrees, 0.0);
        assertEquals(-2.0, o.yawDegrees, 0.0);
    }

    @Test
    public void skipsNullFtcPose() {
        assertNull(FtcEasyATL.observationOrNull(detection(21, null)));
        assertNull(FtcEasyATL.observationOrNull(null));
    }

    @Test
    public void localizeNullAndUnknownIds() {
        FtcEasyATL atl = new FtcEasyATL(new EasyATL.CameraConfig(0, 0, 0))
                .addTag(21, 0, 0, 0)
                .setSmoothingAlpha(1);
        assertFalse(atl.localize(null));
        assertFalse(atl.localize(Collections.singletonList(
                detection(99, pose(0.0, 36.0, 36.0, 0.0, 0.0)))));
        assertFalse(atl.hasPose());
    }

    @Test
    public void multipleDetectionsIgnoreNullPose() {
        FtcEasyATL atl = new FtcEasyATL(new EasyATL.CameraConfig(0, 0, 0),
                new EasyATL.Config().setSmoothingAlpha(1)
                        .setMaxRangeInches(200)
                        .setMaxBearingDegrees(90)
                        .setMaxTagYawDegrees(90))
                .addTag(21, 0, 0, 0);
        FieldPose robot = new FieldPose(36, 0, Math.PI);
        EasyATL.Observation expected = KnownPoses.observation(21, 0, 0, 0, new EasyATL.CameraConfig(0, 0, 0), robot);
        AprilTagDetection good = detection(21, pose(expected.right, expected.forward, expected.range,
                expected.bearingDegrees, expected.yawDegrees));
        AprilTagDetection missing = detection(21, null);
        assertTrue(atl.localize(Arrays.asList(missing, good)));
        assertEquals(36, atl.getPose().x, 0.05);
        assertEquals(1, atl.getAcceptedTags().size());
    }
}
