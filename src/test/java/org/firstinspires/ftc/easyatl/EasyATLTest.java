package org.firstinspires.ftc.easyatl;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class EasyATLTest {
    private static final double INCH = 0.05;
    private static final double RAD = Math.toRadians(0.15);
    private static final int TAG = 21;
    private static final double TX = 0;
    private static final double TY = 0;
    private static final double TFACING = 0;
    private static final EasyATL.CameraConfig ZERO_CAM = new EasyATL.CameraConfig(0, 0, 0);

    private static EasyATL localizer(EasyATL.CameraConfig camera) {
        return new EasyATL(camera, new EasyATL.Config().setSmoothingAlpha(1)
                .setMaxRangeInches(200)
                .setMaxBearingDegrees(180)
                .setMaxTagYawDegrees(180))
                .addTag(TAG, TX, TY, TFACING);
    }

    private static EasyATL.Observation obs(EasyATL.CameraConfig camera, FieldPose robot) {
        return KnownPoses.observation(TAG, TX, TY, TFACING, camera, robot);
    }

    private static void assertPose(FieldPose expected, FieldPose actual) {
        assertTrue("missing pose", actual != null);
        assertEquals("x", expected.x, actual.x, INCH);
        assertEquals("y", expected.y, actual.y, INCH);
        assertEquals("heading", 0, KnownPoses.wrap(actual.heading - expected.heading), RAD);
    }

    @Test
    public void singleTagHeadOn() {
        FieldPose robot = new FieldPose(36, 0, Math.PI);
        EasyATL atl = localizer(ZERO_CAM);
        assertTrue(atl.localize(Collections.singletonList(obs(ZERO_CAM, robot))));
        assertPose(robot, atl.getPose());
        assertEquals(Collections.singletonList(Integer.valueOf(TAG)), atl.getAcceptedTags());
        assertEquals(Collections.singletonList(Integer.valueOf(TAG)), atl.getVisibleTags());
    }

    @Test
    public void scenarioRobotAt24_36_heading0() {
        FieldPose robot = new FieldPose(24, 36, 0);
        EasyATL atl = localizer(ZERO_CAM);
        assertTrue(atl.localize(Collections.singletonList(obs(ZERO_CAM, robot))));
        assertPose(robot, atl.getPose());
    }

    @Test
    public void scenarioRobotAt48_24_heading90() {
        FieldPose robot = new FieldPose(48, 24, Math.PI / 2);
        EasyATL atl = localizer(ZERO_CAM);
        assertTrue(atl.localize(Collections.singletonList(obs(ZERO_CAM, robot))));
        assertPose(robot, atl.getPose());
    }

    @Test
    public void scenarioRobotAt72_72_heading180() {
        FieldPose robot = new FieldPose(72, 72, Math.PI);
        EasyATL atl = localizer(ZERO_CAM);
        assertTrue(atl.localize(Collections.singletonList(obs(ZERO_CAM, robot))));
        assertPose(robot, atl.getPose());
    }

    @Test
    public void cameraForwardOffset() {
        EasyATL.CameraConfig camera = new EasyATL.CameraConfig(4, 0, 0);
        FieldPose robot = new FieldPose(40, 0, Math.PI);
        EasyATL atl = localizer(camera);
        assertTrue(atl.localize(Collections.singletonList(obs(camera, robot))));
        assertPose(robot, atl.getPose());
    }

    @Test
    public void cameraRightOffset() {
        EasyATL.CameraConfig camera = new EasyATL.CameraConfig(0, 3, 0);
        FieldPose robot = new FieldPose(36, 8, Math.PI);
        EasyATL atl = localizer(camera);
        assertTrue(atl.localize(Collections.singletonList(obs(camera, robot))));
        assertPose(robot, atl.getPose());
    }

    @Test
    public void cameraYaw() {
        EasyATL.CameraConfig camera = new EasyATL.CameraConfig(0, 0, Math.toRadians(-10));
        FieldPose robot = new FieldPose(36, 6, Math.PI);
        EasyATL atl = localizer(camera);
        assertTrue(atl.localize(Collections.singletonList(obs(camera, robot))));
        assertPose(robot, atl.getPose());
    }

    @Test
    public void largeCameraOffset() {
        EasyATL.CameraConfig camera = new EasyATL.CameraConfig(-8, 6, Math.toRadians(15));
        FieldPose robot = new FieldPose(30, -12, Math.toRadians(170));
        EasyATL atl = localizer(camera);
        assertTrue(atl.localize(Collections.singletonList(obs(camera, robot))));
        assertPose(robot, atl.getPose());
    }

    @Test
    public void tagFacingHeading() {
        double facing = Math.PI / 2;
        FieldPose robot = new FieldPose(0, 36, -Math.PI / 2);
        EasyATL atl = new EasyATL(ZERO_CAM, new EasyATL.Config().setSmoothingAlpha(1)
                .setMaxRangeInches(200).setMaxBearingDegrees(90).setMaxTagYawDegrees(90))
                .addTag(TAG, TX, TY, facing);
        EasyATL.Observation o = KnownPoses.observation(TAG, TX, TY, facing, ZERO_CAM, robot);
        assertTrue(atl.localize(Collections.singletonList(o)));
        assertPose(robot, atl.getPose());
    }

    @Test
    public void multipleAgreeingTags() {
        FieldPose robot = new FieldPose(36, 0, Math.PI);
        EasyATL atl = new EasyATL(ZERO_CAM, new EasyATL.Config().setSmoothingAlpha(1)
                .setMaxRangeInches(200).setMaxBearingDegrees(90).setMaxTagYawDegrees(90))
                .addTag(21, 0, 0, 0)
                .addTag(22, 0, 12, 0);
        EasyATL.Observation a = KnownPoses.observation(21, 0, 0, 0, ZERO_CAM, robot);
        EasyATL.Observation b = KnownPoses.observation(22, 0, 12, 0, ZERO_CAM, robot);
        assertTrue(atl.localize(Arrays.asList(a, b)));
        assertPose(robot, atl.getPose());
        assertEquals(2, atl.getAcceptedTags().size());
        assertEquals(2, atl.getVisibleTags().size());
    }

    @Test
    public void outlierTagIsRejected() {
        FieldPose good = new FieldPose(36, 0, Math.PI);
        FieldPose bad = new FieldPose(80, 0, Math.PI);
        EasyATL atl = new EasyATL(ZERO_CAM, new EasyATL.Config().setSmoothingAlpha(1)
                .setMaxRangeInches(200).setMaxBearingDegrees(90).setMaxTagYawDegrees(90)
                .setOutlierDistanceInches(12).setOutlierHeadingDegrees(25))
                .addTag(21, 0, 0, 0)
                .addTag(22, 0, 8, 0)
                .addTag(23, 0, -8, 0);
        EasyATL.Observation a = KnownPoses.observation(21, 0, 0, 0, ZERO_CAM, good);
        EasyATL.Observation b = KnownPoses.observation(22, 0, 8, 0, ZERO_CAM, good);
        EasyATL.Observation c = KnownPoses.observation(23, 0, -8, 0, ZERO_CAM, bad);
        assertTrue(atl.localize(Arrays.asList(a, b, c)));
        assertPose(good, atl.getPose());
        assertFalse(atl.getAcceptedTags().contains(Integer.valueOf(23)));
        assertTrue(atl.getVisibleTags().contains(Integer.valueOf(23)));
        assertEquals(3, atl.getVisibleTags().size());
        assertEquals(2, atl.getAcceptedTags().size());
    }

    @Test
    public void emptyAndNullDetections() {
        EasyATL atl = localizer(ZERO_CAM);
        assertFalse(atl.localize(Collections.<EasyATL.Observation>emptyList()));
        assertFalse(atl.hasPose());
        assertNull(atl.getPose());
        assertTrue(atl.getVisibleTags().isEmpty());
        assertTrue(atl.getAcceptedTags().isEmpty());
        assertFalse(atl.localize(null));
    }

    @Test
    public void unknownTagIdIsIgnored() {
        EasyATL atl = localizer(ZERO_CAM);
        EasyATL.Observation o = new EasyATL.Observation(99, 0, 36, 36, 0, 0);
        assertFalse(atl.localize(Collections.singletonList(o)));
        assertTrue(atl.getVisibleTags().isEmpty());
        assertFalse(atl.hasPose());
    }

    @Test
    public void nonFiniteObservationRejected() {
        EasyATL atl = localizer(ZERO_CAM);
        EasyATL.Observation o = new EasyATL.Observation(TAG, 0, 36, Double.NaN, 0, 0);
        assertFalse(atl.localize(Collections.singletonList(o)));
        assertEquals(Collections.singletonList(Integer.valueOf(TAG)), atl.getVisibleTags());
        assertTrue(atl.getAcceptedTags().isEmpty());
    }

    @Test
    public void rangeFilterInclusiveBoundary() {
        EasyATL atl = new EasyATL(ZERO_CAM, new EasyATL.Config().setSmoothingAlpha(1)
                .setMaxRangeInches(72).setMaxBearingDegrees(90).setMaxTagYawDegrees(90))
                .addTag(TAG, TX, TY, TFACING);
        FieldPose robot = new FieldPose(36, 0, Math.PI);
        EasyATL.Observation inside = KnownPoses.withRange(obs(ZERO_CAM, robot), 72);
        EasyATL.Observation outside = KnownPoses.withRange(obs(ZERO_CAM, robot), 72.01);
        assertTrue(atl.localize(Collections.singletonList(inside)));
        atl = new EasyATL(ZERO_CAM, new EasyATL.Config().setSmoothingAlpha(1)
                .setMaxRangeInches(72).setMaxBearingDegrees(90).setMaxTagYawDegrees(90))
                .addTag(TAG, TX, TY, TFACING);
        assertFalse(atl.localize(Collections.singletonList(outside)));
    }

    @Test
    public void bearingFilterInclusiveBoundary() {
        EasyATL atl = new EasyATL(ZERO_CAM, new EasyATL.Config().setSmoothingAlpha(1)
                .setMaxRangeInches(200).setMaxBearingDegrees(55).setMaxTagYawDegrees(90))
                .addTag(TAG, TX, TY, TFACING);
        FieldPose robot = new FieldPose(36, 0, Math.PI);
        EasyATL.Observation atLimit = KnownPoses.withBearing(obs(ZERO_CAM, robot), 55);
        EasyATL.Observation outside = KnownPoses.withBearing(obs(ZERO_CAM, robot), 55.01);
        assertTrue(atl.localize(Collections.singletonList(atLimit)));
        atl = new EasyATL(ZERO_CAM, new EasyATL.Config().setSmoothingAlpha(1)
                .setMaxRangeInches(200).setMaxBearingDegrees(55).setMaxTagYawDegrees(90))
                .addTag(TAG, TX, TY, TFACING);
        assertFalse(atl.localize(Collections.singletonList(outside)));
    }

    @Test
    public void tagYawFilterInclusiveBoundary() {
        EasyATL atl = new EasyATL(ZERO_CAM, new EasyATL.Config().setSmoothingAlpha(1)
                .setMaxRangeInches(200).setMaxBearingDegrees(90).setMaxTagYawDegrees(45))
                .addTag(TAG, TX, TY, TFACING);
        FieldPose robot = new FieldPose(36, 0, Math.PI);
        EasyATL.Observation atLimit = KnownPoses.withYaw(obs(ZERO_CAM, robot), -45);
        EasyATL.Observation outside = KnownPoses.withYaw(obs(ZERO_CAM, robot), -45.01);
        assertTrue(atl.localize(Collections.singletonList(atLimit)));
        atl = new EasyATL(ZERO_CAM, new EasyATL.Config().setSmoothingAlpha(1)
                .setMaxRangeInches(200).setMaxBearingDegrees(90).setMaxTagYawDegrees(45))
                .addTag(TAG, TX, TY, TFACING);
        assertFalse(atl.localize(Collections.singletonList(outside)));
    }

    @Test
    public void headingWrapNearZero() {
        FieldPose robot = new FieldPose(36, 0, Math.toRadians(179));
        EasyATL atl = localizer(ZERO_CAM);
        assertTrue(atl.localize(Collections.singletonList(obs(ZERO_CAM, robot))));
        assertPose(robot, atl.getPose());
        FieldPose crossed = new FieldPose(36, 0, Math.toRadians(-179));
        atl.setSmoothingAlpha(1);
        assertTrue(atl.localize(Collections.singletonList(obs(ZERO_CAM, crossed))));
        assertPose(crossed, atl.getPose());
    }

    @Test
    public void negativeAndLargeHeadingsWrap() {
        FieldPose robot = new FieldPose(40, 10, Math.toRadians(400));
        EasyATL atl = localizer(ZERO_CAM);
        assertTrue(atl.localize(Collections.singletonList(obs(ZERO_CAM, robot))));
        FieldPose wrapped = new FieldPose(robot.x, robot.y, KnownPoses.wrap(robot.heading));
        assertPose(wrapped, atl.getPose());
        robot = new FieldPose(40, 10, Math.toRadians(-200));
        atl = localizer(ZERO_CAM);
        assertTrue(atl.localize(Collections.singletonList(obs(ZERO_CAM, robot))));
        assertPose(new FieldPose(robot.x, robot.y, KnownPoses.wrap(robot.heading)), atl.getPose());
    }

    @Test
    public void smoothingBlendsTowardNewMeasurement() {
        EasyATL atl = new EasyATL(ZERO_CAM, new EasyATL.Config().setSmoothingAlpha(0.5)
                .setMaxRangeInches(200).setMaxBearingDegrees(90).setMaxTagYawDegrees(90))
                .addTag(TAG, TX, TY, TFACING);
        FieldPose first = new FieldPose(36, 0, Math.PI);
        FieldPose second = new FieldPose(40, 0, Math.PI);
        assertTrue(atl.localize(Collections.singletonList(obs(ZERO_CAM, first))));
        assertPose(first, atl.getPose());
        assertTrue(atl.localize(Collections.singletonList(obs(ZERO_CAM, second))));
        assertEquals(38, atl.getPose().x, INCH);
        assertEquals(0, atl.getPose().y, INCH);
    }

    @Test
    public void consecutiveIdenticalFramesStayPut() {
        FieldPose robot = new FieldPose(36, 0, Math.PI);
        EasyATL atl = localizer(ZERO_CAM);
        EasyATL.Observation o = obs(ZERO_CAM, robot);
        assertTrue(atl.localize(Collections.singletonList(o)));
        assertTrue(atl.localize(Collections.singletonList(o)));
        assertPose(robot, atl.getPose());
    }

    @Test
    public void failedFrameKeepsLastPose() {
        FieldPose robot = new FieldPose(36, 0, Math.PI);
        EasyATL atl = localizer(ZERO_CAM);
        assertTrue(atl.localize(Collections.singletonList(obs(ZERO_CAM, robot))));
        assertFalse(atl.localize(Collections.<EasyATL.Observation>emptyList()));
        assertTrue(atl.hasPose());
        assertPose(robot, atl.getPose());
        assertTrue(atl.getAcceptedTags().isEmpty());
    }

    @Test
    public void qualityIsInUnitIntervalAndHigherWhenCloser() {
        EasyATL far = localizer(ZERO_CAM);
        EasyATL near = localizer(ZERO_CAM);
        assertTrue(far.localize(Collections.singletonList(obs(ZERO_CAM, new FieldPose(80, 0, Math.PI)))));
        assertTrue(near.localize(Collections.singletonList(obs(ZERO_CAM, new FieldPose(24, 0, Math.PI)))));
        double qFar = far.getQuality();
        double qNear = near.getQuality();
        assertTrue(qFar > 0 && qFar <= 1);
        assertTrue(qNear > 0 && qNear <= 1);
        assertTrue(qNear > qFar);
    }

    @Test
    public void qualityDecaysAfterTagsAreLost() throws Exception {
        EasyATL atl = localizer(ZERO_CAM);
        assertTrue(atl.localize(Collections.singletonList(obs(ZERO_CAM, new FieldPose(36, 0, Math.PI)))));
        double before = atl.getQuality();
        Thread.sleep(400);
        assertFalse(atl.localize(Collections.<EasyATL.Observation>emptyList()));
        double after = atl.getQuality();
        assertTrue("quality should decay, before=" + before + " after=" + after, after < before * 0.9);
    }
}
