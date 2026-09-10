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

    @Test
    public void mapsDecisionMarginZAndTimestamp() {
        AprilTagDetection d = new AprilTagDetection(21, 0, 42.5f, null, null, null,
                pose(3.0, 40.0, 40.2, 4.0, -2.0), null, null, 123L);
        EasyATL.Observation o = FtcEasyATL.observationOrNull(d);
        assertEquals(0.0, o.z, 0.0);
        assertEquals(42.5, o.decisionMargin, 0.0);
        assertEquals(123L, o.captureNanoTime);
    }

    @Test
    public void addCurrentGameTagsDoesNotThrow() {
        FtcEasyATL atl = new FtcEasyATL(new EasyATL.CameraConfig(0, 0, 0));
        atl.addCurrentGameTags();
        atl.reset();
        atl.setEnabled(true);
        assertFalse(atl.hasPose());
    }

    @Test
    public void identityQuaternionFacingIsZero() {
        org.firstinspires.ftc.robotcore.external.navigation.Quaternion q =
                new org.firstinspires.ftc.robotcore.external.navigation.Quaternion(1, 0, 0, 0, 0);
        assertEquals(0, FtcEasyATL.facingRadians(q), 1e-9);
        assertEquals(0, FtcEasyATL.facingRadians(null), 0.0);
    }

    @Test
    public void headingDegreesHelper() {
        assertEquals(180, new FieldPose(0, 0, Math.PI).headingDegrees(), 1e-6);
    }

    @Test
    public void noArgConstructorAndDefaultSdkConstantsUseLatestSeason() {
        EasyATL.Config wide = new EasyATL.Config().setSmoothingAlpha(1)
                .setMaxRangeInches(200).setMaxBearingDegrees(180).setMaxTagYawDegrees(180);
        FtcEasyATL fromDefaults = DefaultSdkConstants.createLocalizer(wide);
        FtcEasyATL fromNoArg = new FtcEasyATL();
        fromNoArg.setConfig(wide);

        FieldTags.Tag blue = FieldTags.latest().tags().get(0);
        EasyATL.CameraConfig cam = DefaultSdkConstants.camera();
        FieldPose robot = new FieldPose(
                blue.x + 36 * Math.cos(blue.facingRadians),
                blue.y + 36 * Math.sin(blue.facingRadians),
                EasyATL.wrap(blue.facingRadians + Math.PI));
        EasyATL.Observation expected = KnownPoses.observation(
                blue.id, blue.x, blue.y, blue.facingRadians, cam, robot);
        AprilTagDetection d = detection(blue.id, pose(expected.right, expected.forward,
                expected.range, expected.bearingDegrees, expected.yawDegrees));

        assertTrue(fromDefaults.localize(Collections.singletonList(d)));
        assertTrue(fromNoArg.localize(Collections.singletonList(d)));
        assertEquals(robot.x, fromDefaults.getPose().x, 0.08);
        assertEquals(robot.x, fromNoArg.getPose().x, 0.08);
        assertEquals(0.0, cam.forward, 0.0);
        assertEquals("Webcam 1", DefaultSdkConstants.WEBCAM_NAME);
    }

    @Test
    public void observationOrNullRejectsNonFiniteAndNonPositiveRange() {
        assertNull(FtcEasyATL.observationOrNull(detection(21, pose(0, 36, 0, 0, 0))));
        assertNull(FtcEasyATL.observationOrNull(detection(21,
                new AprilTagPoseFtc(Double.NaN, 36, 0, 0, 0, 0, 36, 0, 0))));
    }

    @Test
    public void localizeAppliesPoseCorrectorWhenQualityPasses() {
        FtcEasyATL atl = new FtcEasyATL(new EasyATL.CameraConfig(0, 0, 0),
                new EasyATL.Config().setSmoothingAlpha(1).setMaxRangeInches(200)
                        .setMaxBearingDegrees(180).setMaxTagYawDegrees(180))
                .addTag(21, 0, 0, 0);
        RecordingCorrector corrector = new RecordingCorrector();
        EasyATL.Observation expected = KnownPoses.observation(21, 0, 0, 0,
                new EasyATL.CameraConfig(0, 0, 0), new FieldPose(36, 0, Math.PI));
        AprilTagDetection d = detection(21, pose(expected.right, expected.forward,
                expected.range, expected.bearingDegrees, expected.yawDegrees));
        assertTrue(atl.localize(Collections.singletonList(d), corrector, 0));
        assertEquals(1, corrector.calls);
        assertEquals(36, corrector.last.x, 0.08);
        assertTrue(atl.localize(Collections.singletonList(d), corrector, 2));
        assertEquals(1, corrector.calls);
    }

    private static final class RecordingCorrector implements PoseCorrector {
        int calls;
        FieldPose last;

        @Override
        public void apply(FieldPose vision) {
            calls++;
            last = vision;
        }
    }
}
