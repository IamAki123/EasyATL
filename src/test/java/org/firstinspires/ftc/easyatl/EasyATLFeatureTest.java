package org.firstinspires.ftc.easyatl;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class EasyATLFeatureTest {
    private static final double INCH = 0.08;
    private static final double RAD = Math.toRadians(0.25);
    private static final EasyATL.CameraConfig ZERO = new EasyATL.CameraConfig(0, 0, 0);

    private static EasyATL open(EasyATL.CameraConfig camera) {
        return new EasyATL(camera, new EasyATL.Config().setSmoothingAlpha(1)
                .setMaxRangeInches(200)
                .setMaxBearingDegrees(180)
                .setMaxTagYawDegrees(180))
                .addTag(21, 0, 0, 0);
    }

    private static void assertPose(FieldPose expected, FieldPose actual) {
        assertNotNull(actual);
        assertEquals("x", expected.x, actual.x, INCH);
        assertEquals("y", expected.y, actual.y, INCH);
        assertEquals("heading", 0, KnownPoses.wrap(actual.heading - expected.heading), RAD);
    }

    @Test
    public void cameraPitchRoundtrip() {
        EasyATL.CameraConfig camera = new EasyATL.CameraConfig(2, -1, Math.toRadians(8), Math.toRadians(-12));
        FieldPose robot = new FieldPose(40, 6, Math.toRadians(170));
        EasyATL atl = open(camera);
        assertTrue(atl.localize(Collections.singletonList(
                KnownPoses.observation(21, 0, 0, 0, camera, robot))));
        assertPose(robot, atl.getPose());
    }

    @Test
    public void ignoringPitchBiasesRangeWhenCameraLooksDown() {
        EasyATL.CameraConfig pitched = new EasyATL.CameraConfig(0, 0, 0, Math.toRadians(-20));
        FieldPose robot = new FieldPose(48, 0, Math.PI);
        EasyATL.Observation o = KnownPoses.observation(21, 0, 0, 0, pitched, robot);
        EasyATL withPitch = open(pitched);
        EasyATL withoutPitch = open(ZERO);
        assertTrue(withPitch.localize(Collections.singletonList(o)));
        assertTrue(withoutPitch.localize(Collections.singletonList(o)));
        assertPose(robot, withPitch.getPose());
        assertTrue(Math.abs(withoutPitch.getPose().x - robot.x) > 1.0);
    }

    @Test
    public void randomPlanarRoundtrips() {
        Random rng = new Random(20260909);
        for (int i = 0; i < 40; i++) {
            EasyATL.CameraConfig camera = new EasyATL.CameraConfig(
                    rng.nextGaussian() * 4,
                    rng.nextGaussian() * 4,
                    rng.nextGaussian() * 0.3,
                    rng.nextGaussian() * 0.2);
            FieldPose robot = new FieldPose(
                    rng.nextGaussian() * 40 + 36,
                    rng.nextGaussian() * 40,
                    rng.nextGaussian() * Math.PI);
            EasyATL atl = open(camera);
            assertTrue("i=" + i, atl.localize(Collections.singletonList(
                    KnownPoses.observation(21, 0, 0, 0, camera, robot))));
            assertPose(robot, atl.getPose());
        }
    }

    @Test
    public void multiTagHeadingWrapDoesNotAverageAcrossPi() {
        FieldPose robot = new FieldPose(36, 0, Math.PI);
        EasyATL atl = new EasyATL(ZERO, new EasyATL.Config().setSmoothingAlpha(1)
                .setMaxRangeInches(200).setMaxBearingDegrees(180).setMaxTagYawDegrees(180)
                .setOutlierHeadingDegrees(25))
                .addTag(21, 0, 0, 0)
                .addTag(22, 0, 8, 0);
        EasyATL.Observation a = KnownPoses.observation(21, 0, 0, 0, ZERO, robot);
        EasyATL.Observation b = KnownPoses.observation(22, 0, 8, 0, ZERO, robot);
        a = KnownPoses.withYaw(a, a.yawDegrees + 1);
        b = KnownPoses.withYaw(b, b.yawDegrees - 1);
        assertTrue(atl.localize(Arrays.asList(a, b)));
        double err = Math.abs(KnownPoses.wrap(atl.getPose().heading - Math.PI));
        assertTrue("heading should stay near ±π, was " + atl.getPose().heading, err < Math.toRadians(5));
        assertEquals(2, atl.getAcceptedTags().size());
    }

    @Test
    public void qualityDecayUsesInjectedClock() {
        AtomicLong nanos = new AtomicLong(1_000_000_000L);
        EasyATL atl = open(ZERO);
        atl.setNanoTimeSource(EasyATL.advancingNanoTime(nanos));
        assertTrue(atl.localize(Collections.singletonList(
                KnownPoses.observation(21, 0, 0, 0, ZERO, new FieldPose(36, 0, Math.PI)))));
        double before = atl.getQuality();
        nanos.addAndGet(1_000_000_000L);
        assertFalse(atl.localize(Collections.<EasyATL.Observation>emptyList()));
        double after = atl.getQuality();
        double expected = before * Math.exp(-0.8);
        assertEquals(expected, after, 0.02);
        assertTrue(after < before);
    }

    @Test
    public void resetClearsPoseAndQuality() {
        EasyATL atl = open(ZERO);
        assertTrue(atl.localize(Collections.singletonList(
                KnownPoses.observation(21, 0, 0, 0, ZERO, new FieldPose(36, 0, Math.PI)))));
        atl.reset();
        assertFalse(atl.hasPose());
        assertNull(atl.getPose());
        assertEquals(0, atl.getQuality(), 0.0);
        assertTrue(atl.getAcceptedTags().isEmpty());
    }

    @Test
    public void setPoseSeedsEstimate() {
        EasyATL atl = open(ZERO);
        atl.setPose(new FieldPose(10, 20, 0.5));
        assertTrue(atl.hasPose());
        assertEquals(10, atl.getPose().x, 0.0);
        assertTrue(atl.getQuality() > 0.99);
    }

    @Test
    public void disabledDoesNotAccept() {
        EasyATL atl = open(ZERO).setEnabled(false);
        assertFalse(atl.localize(Collections.singletonList(
                KnownPoses.observation(21, 0, 0, 0, ZERO, new FieldPose(36, 0, Math.PI)))));
        assertFalse(atl.hasPose());
        assertEquals("disabled", atl.getDebug().tags.get(0).rejectReason);
        atl.setEnabled(true);
        assertTrue(atl.localize(Collections.singletonList(
                KnownPoses.observation(21, 0, 0, 0, ZERO, new FieldPose(36, 0, Math.PI)))));
    }

    @Test
    public void decisionMarginIncreasesWeightVersusZero() {
        EasyATL.Observation base = KnownPoses.observation(21, 0, 0, 0, ZERO, new FieldPose(60, 0, Math.PI));
        EasyATL low = open(ZERO);
        EasyATL high = open(ZERO);
        EasyATL.Observation weak = new EasyATL.Observation(base.id, base.right, base.forward, base.range,
                base.bearingDegrees, base.yawDegrees, 0, 5, 0L);
        EasyATL.Observation strong = new EasyATL.Observation(base.id, base.right, base.forward, base.range,
                base.bearingDegrees, base.yawDegrees, 0, 80, 0L);
        assertTrue(low.localize(Collections.singletonList(weak)));
        assertTrue(high.localize(Collections.singletonList(strong)));
        assertTrue(high.getQuality() > low.getQuality());
        assertTrue(high.getDebug().tags.get(0).weight > low.getDebug().tags.get(0).weight);
    }

    @Test
    public void smallerRangeScalePrefersCloserTags() {
        EasyATL farScale = new EasyATL(ZERO, new EasyATL.Config().setSmoothingAlpha(1)
                .setMaxRangeInches(200).setMaxBearingDegrees(180).setMaxTagYawDegrees(180)
                .setWeightRangeScaleInches(80))
                .addTag(21, 0, 0, 0);
        EasyATL nearScale = new EasyATL(ZERO, new EasyATL.Config().setSmoothingAlpha(1)
                .setMaxRangeInches(200).setMaxBearingDegrees(180).setMaxTagYawDegrees(180)
                .setWeightRangeScaleInches(12))
                .addTag(21, 0, 0, 0);
        EasyATL.Observation far = KnownPoses.observation(21, 0, 0, 0, ZERO, new FieldPose(72, 0, Math.PI));
        assertTrue(farScale.localize(Collections.singletonList(far)));
        assertTrue(nearScale.localize(Collections.singletonList(far)));
        assertTrue(farScale.getDebug().tags.get(0).weight > nearScale.getDebug().tags.get(0).weight);
    }

    @Test
    public void maxStepLimitsJump() {
        EasyATL atl = new EasyATL(ZERO, new EasyATL.Config().setSmoothingAlpha(1)
                .setMaxRangeInches(200).setMaxBearingDegrees(180).setMaxTagYawDegrees(180)
                .setMaxStepInches(2))
                .addTag(21, 0, 0, 0);
        assertTrue(atl.localize(Collections.singletonList(
                KnownPoses.observation(21, 0, 0, 0, ZERO, new FieldPose(36, 0, Math.PI)))));
        assertTrue(atl.localize(Collections.singletonList(
                KnownPoses.observation(21, 0, 0, 0, ZERO, new FieldPose(48, 0, Math.PI)))));
        assertEquals(38, atl.getPose().x, INCH);
    }

    @Test
    public void staleFrameIsRejected() {
        AtomicLong nanos = new AtomicLong(10_000_000_000L);
        EasyATL atl = new EasyATL(ZERO, new EasyATL.Config().setSmoothingAlpha(1)
                .setMaxRangeInches(200).setMaxBearingDegrees(180).setMaxTagYawDegrees(180)
                .setMaxObservationAgeMs(50))
                .addTag(21, 0, 0, 0);
        atl.setNanoTimeSource(EasyATL.advancingNanoTime(nanos));
        EasyATL.Observation o = KnownPoses.observation(21, 0, 0, 0, ZERO, new FieldPose(36, 0, Math.PI));
        assertFalse(atl.localize(Collections.singletonList(o), 1_000_000_000L));
        assertFalse(atl.hasPose());
        assertEquals("stale", atl.getDebug().tags.get(0).rejectReason);
        assertTrue(atl.localize(Collections.singletonList(o), nanos.get()));
    }

    @Test
    public void localizeFromCamerasFusesBoth() {
        EasyATL.CameraConfig front = new EasyATL.CameraConfig(4, 0, 0);
        EasyATL.CameraConfig rear = new EasyATL.CameraConfig(-4, 0, Math.PI);
        FieldPose robot = new FieldPose(36, 0, Math.PI);
        EasyATL atl = new EasyATL(front, new EasyATL.Config().setSmoothingAlpha(1)
                .setMaxRangeInches(200).setMaxBearingDegrees(180).setMaxTagYawDegrees(180))
                .addTag(21, 0, 0, 0)
                .addTag(22, 72, 0, Math.PI);
        EasyATL.Observation a = KnownPoses.observation(21, 0, 0, 0, front, robot);
        EasyATL.Observation b = KnownPoses.observation(22, 72, 0, Math.PI, rear, robot);
        List<EasyATL.CameraObservations> frames = Arrays.asList(
                new EasyATL.CameraObservations(front, Collections.singletonList(a)),
                new EasyATL.CameraObservations(rear, Collections.singletonList(b)));
        assertTrue(atl.localizeFromCameras(frames));
        assertPose(robot, atl.getPose());
        assertEquals(2, atl.getAcceptedTags().size());
    }

    @Test
    public void fieldTagsDecodeRegistersGoalTags() {
        EasyATL atl = new EasyATL(ZERO, new EasyATL.Config().setSmoothingAlpha(1)
                .setMaxRangeInches(200).setMaxBearingDegrees(180).setMaxTagYawDegrees(180))
                .addTags(FieldTags.decode());
        FieldTags.Tag blue = FieldTags.decode().tags().get(0);
        FieldPose robot = new FieldPose(blue.x + 36 * Math.cos(blue.facingRadians),
                blue.y + 36 * Math.sin(blue.facingRadians),
                EasyATL.wrap(blue.facingRadians + Math.PI));
        EasyATL.Observation o = KnownPoses.observation(blue.id, blue.x, blue.y, blue.facingRadians, ZERO, robot);
        assertTrue(atl.localize(Collections.singletonList(o)));
        assertPose(robot, atl.getPose());
        assertEquals("DECODE", FieldTags.decode().seasonName());
        assertEquals(2, FieldTags.decode().tags().size());
        assertEquals(6, FieldTags.intoTheDeep().tags().size());
        assertEquals(FieldTags.Season.DECODE, FieldTags.Season.latest());
        assertEquals(2, FieldTags.latest().tags().size());
        assertEquals(6, FieldTags.season(FieldTags.Season.INTO_THE_DEEP).tags().size());
    }

    @Test
    public void customAprilTagsRoundtrip() {
        FieldTags set = FieldTags.custom("Practice field")
                .add(21, 8, 8, Math.toRadians(45), "left wall")
                .build();
        EasyATL atl = new EasyATL(ZERO, new EasyATL.Config().setSmoothingAlpha(1)
                .setMaxRangeInches(200).setMaxBearingDegrees(180).setMaxTagYawDegrees(180))
                .setTags(set);
        FieldPose robot = new FieldPose(
                8 + 36 * Math.cos(Math.toRadians(45)),
                8 + 36 * Math.sin(Math.toRadians(45)),
                EasyATL.wrap(Math.toRadians(45) + Math.PI));
        EasyATL.Observation o = KnownPoses.observation(21, 8, 8, Math.toRadians(45), ZERO, robot);
        assertTrue(atl.localize(Collections.singletonList(o)));
        assertPose(robot, atl.getPose());
    }

    @Test
    public void useLatestSeasonReplacesOldIds() {
        EasyATL atl = open(ZERO);
        atl.useSeason(FieldTags.Season.INTO_THE_DEEP);
        atl.useLatestSeason();
        EasyATL.Observation unknown = new EasyATL.Observation(11, 0, 36, 36, 0, 0);
        assertFalse(atl.localize(Collections.singletonList(unknown)));
        assertTrue(atl.getVisibleTags().isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void centerStageSeasonRequiresCustomOrSdk() {
        FieldTags.season(FieldTags.Season.CENTERSTAGE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyCustomSetRejected() {
        FieldTags.custom("empty").build();
    }

    @Test
    public void debugAndUncertaintyPopulatedOnAccept() {
        EasyATL atl = open(ZERO);
        assertTrue(atl.localize(Collections.singletonList(
                KnownPoses.observation(21, 0, 0, 0, ZERO, new FieldPose(36, 0, Math.PI)))));
        assertTrue(atl.getDebug().accepted);
        assertNotNull(atl.getDebug().rawMeasurement);
        assertNotNull(atl.getUncertainty());
        assertTrue(atl.getUncertainty().sigmaXInches > 0);
        assertEquals(21, atl.getDebug().tags.get(0).id);
        assertTrue(atl.getDebug().tags.get(0).accepted());
    }

    @Test
    public void stressMixedGoodAndBadDetections() {
        EasyATL atl = new EasyATL(ZERO, new EasyATL.Config().setSmoothingAlpha(1)
                .setMaxRangeInches(80).setMaxBearingDegrees(60).setMaxTagYawDegrees(45)
                .setOutlierDistanceInches(12))
                .addTag(21, 0, 0, 0)
                .addTag(22, 0, 10, 0)
                .addTag(23, 0, -10, 0)
                .addTag(24, 8, 0, 0);
        FieldPose good = new FieldPose(36, 0, Math.PI);
        FieldPose bad = new FieldPose(90, 40, 0);
        List<EasyATL.Observation> obs = new ArrayList<>();
        obs.add(KnownPoses.observation(21, 0, 0, 0, ZERO, good));
        obs.add(KnownPoses.observation(22, 0, 10, 0, ZERO, good));
        obs.add(KnownPoses.observation(23, 0, -10, 0, ZERO, bad));
        obs.add(KnownPoses.withRange(KnownPoses.observation(24, 8, 0, 0, ZERO, good), 200));
        obs.add(new EasyATL.Observation(99, 0, 10, 10, 0, 0));
        obs.add(new EasyATL.Observation(21, 0, 36, Double.NaN, 0, 0));
        assertTrue(atl.localize(obs));
        assertPose(good, atl.getPose());
        assertTrue(atl.getAcceptedTags().size() >= 2);
        assertFalse(atl.getAcceptedTags().contains(Integer.valueOf(23)));
    }

    @Test
    public void localizeIsCheap() {
        EasyATL atl = new EasyATL(ZERO, new EasyATL.Config().setSmoothingAlpha(1)
                .setMaxRangeInches(200).setMaxBearingDegrees(180).setMaxTagYawDegrees(180))
                .addTag(21, 0, 0, 0)
                .addTag(22, 0, 8, 0)
                .addTag(23, 0, -8, 0);
        FieldPose robot = new FieldPose(36, 0, Math.PI);
        List<EasyATL.Observation> obs = Arrays.asList(
                KnownPoses.observation(21, 0, 0, 0, ZERO, robot),
                KnownPoses.observation(22, 0, 8, 0, ZERO, robot),
                KnownPoses.observation(23, 0, -8, 0, ZERO, robot));
        long start = System.nanoTime();
        int n = 4000;
        for (int i = 0; i < n; i++) {
            assertTrue(atl.localize(obs));
        }
        double ms = (System.nanoTime() - start) / 1e6;
        assertTrue("4000 localize calls took " + ms + " ms", ms < 500);
    }

    @Test
    public void polarObservationMatchesCartesian() {
        EasyATL.Observation polar = EasyATLObservations.fromPolar(21, 40, 0, 0);
        assertEquals(40, polar.forward, 1e-9);
        assertEquals(0, polar.right, 1e-9);
        EasyATL atl = open(ZERO);
        assertTrue(atl.localize(Collections.singletonList(polar)));
        assertEquals(40, atl.getPose().x, INCH);
    }
}
