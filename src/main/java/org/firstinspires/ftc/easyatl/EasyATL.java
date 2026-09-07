package org.firstinspires.ftc.easyatl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Multi-tag AprilTag field-pose localizer. Coordinates follow the common FTC/Pedro convention:
 * heading zero points forward along +X, right points -Y, and heading is CCW-positive. All
 * distances are inches. Tag facing headings point out of the printed face toward the camera.
 */
public final class EasyATL {
    /** Camera-frame tag observation: right, forward, range, bearing, and yaw. Angles are degrees. */
    public static final class Observation {
        public final int id; public final double right, forward, range, bearingDegrees, yawDegrees;
        public Observation(int id, double right, double forward, double range, double bearingDegrees, double yawDegrees) {
            this.id=id; this.right=right; this.forward=forward; this.range=range;
            this.bearingDegrees=bearingDegrees; this.yawDegrees=yawDegrees;
        }
    }
    /** Camera lens pose relative to robot center; right/forward in inches and yaw in radians. */
    public static final class CameraConfig {
        public final double forward, right, yawRadians;
        public CameraConfig(double forward, double right, double yawRadians) {
            requireFinite("forward", forward); requireFinite("right", right); requireFinite("yawRadians", yawRadians);
            this.forward=forward; this.right=right; this.yawRadians=yawRadians;
        }
    }
    private static final class Tag { final double x,y,facing; Tag(double x,double y,double facing){this.x=x;this.y=y;this.facing=facing;} }
    private static final class Estimate { final FieldPose pose; final double weight; final int id; Estimate(FieldPose p,double w,int i){pose=p;weight=w;id=i;} }

    private final Map<Integer, Tag> tags = new HashMap<>();
    private CameraConfig camera;
    private FieldPose pose;
    private List<Integer> visibleTags=Collections.emptyList(), acceptedTags=Collections.emptyList();
    private double confidence;
    private long lastConfidenceTime;
    private double maxRangeInches=96, maxBearingDegrees=55, maxYawDegrees=45, outlierDistanceInches=12;
    private double outlierHeadingRadians=Math.toRadians(25), smoothingAlpha=.65, confidenceDecayPerSecond=.8;

    public EasyATL(CameraConfig camera) { this.camera=requireCamera(camera); }
    public EasyATL addTag(int id,double x,double y,double facingHeadingRadians) {
        requireFinite("field x", x); requireFinite("field y", y); requireFinite("facing heading", facingHeadingRadians);
        tags.put(id,new Tag(x,y,facingHeadingRadians)); return this;
    }
    public EasyATL setCameraConfig(CameraConfig value) { camera=requireCamera(value); return this; }
    public EasyATL setMaxRangeInches(double value) {
        if (!Double.isFinite(value) || value <= 0) throw new IllegalArgumentException("max range must be finite and positive");
        maxRangeInches=value; return this;
    }
    /** 1.0 disables temporal smoothing; lower values are steadier but lag more. */
    public EasyATL setSmoothingAlpha(double value) {
        requireFinite("smoothing alpha", value); smoothingAlpha=clamp(value,0,1); return this;
    }

    /** Combines all usable configured tags in the frame. True means a new pose was accepted. */
    public boolean localize(List<Observation> observations) {
        long now=System.nanoTime(); decayConfidence(now);
        if(observations==null) observations=Collections.emptyList();
        List<Integer> visible=new ArrayList<>(); List<Estimate> estimates=new ArrayList<>();
        for(Observation o:observations) { Tag tag=tags.get(o.id); if(tag==null) continue; visible.add(o.id); if(reliable(o)) estimates.add(new Estimate(toPose(tag,o),weight(o),o.id)); }
        visibleTags=Collections.unmodifiableList(visible);
        if(estimates.isEmpty()) { acceptedTags=Collections.emptyList(); return false; }
        // A medoid is robust: one bad tag cannot pull the initial consensus toward itself.
        FieldPose center=robustCenter(estimates); List<Estimate> inliers=new ArrayList<>();
        for(Estimate e:estimates) if(Math.hypot(e.pose.x-center.x,e.pose.y-center.y)<=outlierDistanceInches && Math.abs(wrap(e.pose.heading-center.heading))<=outlierHeadingRadians) inliers.add(e);
        if(inliers.isEmpty()) { acceptedTags=Collections.emptyList(); return false; }
        FieldPose measurement=mean(inliers); pose=pose==null?measurement:blend(pose,measurement,smoothingAlpha);
        List<Integer> ids=new ArrayList<>(); double quality=0; for(Estimate e:inliers){ids.add(e.id);quality+=e.weight;}
        acceptedTags=Collections.unmodifiableList(ids); quality/=inliers.size();
        confidence=clamp(quality*((double)inliers.size()/estimates.size())*Math.min(1,.75+.125*inliers.size()),0,1); lastConfidenceTime=now;
        return true;
    }
    public FieldPose getPose(){return pose;}
    public boolean hasPose(){return pose!=null;}
    /** Configured tags seen in the latest frame, even if rejected by quality checks. */
    public List<Integer> getVisibleTags(){return visibleTags;}
    public List<Integer> getAcceptedTags(){return acceptedTags;}
    /** Heuristic 0..1 measurement quality; it is not a probability and decays while tags are lost. */
    public double getQuality(){decayConfidence(System.nanoTime());return confidence;}
    /** @deprecated Use {@link #getQuality()}; this is a quality score, not calibrated confidence. */
    @Deprecated public double getConfidence(){return getQuality();}

    private boolean reliable(Observation o){return Double.isFinite(o.right)&&Double.isFinite(o.forward)&&Double.isFinite(o.range)&&Double.isFinite(o.bearingDegrees)&&Double.isFinite(o.yawDegrees)&&o.range>0&&o.range<=maxRangeInches&&Math.abs(o.bearingDegrees)<=maxBearingDegrees&&Math.abs(o.yawDegrees)<=maxYawDegrees;}
    private FieldPose toPose(Tag tag,Observation o){double c=Math.cos(camera.yawRadians),s=Math.sin(camera.yawRadians); double f=o.forward*c+o.right*s+camera.forward,r=-o.forward*s+o.right*c+camera.right; double h=wrap(tag.facing+Math.PI-Math.toRadians(o.yawDegrees)-camera.yawRadians); return new FieldPose(tag.x-(f*Math.cos(h)+r*Math.sin(h)),tag.y-(f*Math.sin(h)-r*Math.cos(h)),h);}
    private double weight(Observation o){double range=1/(1+Math.pow(o.range/36,2));double angle=Math.cos(Math.toRadians(o.bearingDegrees))*Math.cos(Math.toRadians(o.yawDegrees));return Math.max(.05,range*Math.max(0,angle));}
    private static FieldPose mean(List<Estimate> values){double x=0,y=0,s=0,c=0,total=0;for(Estimate e:values){x+=e.weight*e.pose.x;y+=e.weight*e.pose.y;s+=e.weight*Math.sin(e.pose.heading);c+=e.weight*Math.cos(e.pose.heading);total+=e.weight;}return new FieldPose(x/total,y/total,Math.atan2(s,c));}
    private FieldPose robustCenter(List<Estimate> values){
        Estimate best=null; double bestCost=Double.POSITIVE_INFINITY;
        for(Estimate candidate:values){
            double cost=0;
            for(Estimate other:values) cost+=Math.min(1,Math.hypot(candidate.pose.x-other.pose.x,candidate.pose.y-other.pose.y)/outlierDistanceInches)+Math.min(1,Math.abs(wrap(candidate.pose.heading-other.pose.heading))/outlierHeadingRadians);
            if(cost<bestCost || (cost==bestCost && (best==null || candidate.weight>best.weight))){best=candidate;bestCost=cost;}
        }
        return best.pose;
    }
    private static FieldPose blend(FieldPose oldPose,FieldPose newPose,double a){return new FieldPose(oldPose.x+a*(newPose.x-oldPose.x),oldPose.y+a*(newPose.y-oldPose.y),wrap(oldPose.heading+a*wrap(newPose.heading-oldPose.heading)));}
    private void decayConfidence(long now){if(lastConfidenceTime==0)return;confidence*=Math.exp(-confidenceDecayPerSecond*Math.max(0,now-lastConfidenceTime)/1e9);lastConfidenceTime=now;}
    private static double wrap(double a){while(a>Math.PI)a-=2*Math.PI;while(a<=-Math.PI)a+=2*Math.PI;return a;}
    private static double clamp(double v,double min,double max){return Math.max(min,Math.min(max,v));}
    private static CameraConfig requireCamera(CameraConfig value){if(value==null)throw new IllegalArgumentException("camera config cannot be null");return value;}
    private static void requireFinite(String name,double value){if(!Double.isFinite(value))throw new IllegalArgumentException(name+" must be finite");}
}
