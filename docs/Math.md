# EasyATL math

[README](../README.md) · [API](API.md) · [Plain English](MathButDumbed.md)

This is what `EasyATL.localize()` does each loop. It is a **heuristic**, not a Kalman filter. Defaults below are `new EasyATL.Config()`.

You do not need this page to use the library. For a no-formula walkthrough, use [How EasyATL works (simple version)](MathButDumbed.md). Read this page when you want to know *why* a tag was dropped or how quality is computed.

## Coordinates

| Quantity | Units / convention |
| --- | --- |
| Field X/Y, camera forward/right, tag X/Y | Inches |
| Robot heading, tag facing, camera yaw / pitch / roll | Radians, yaw CCW-positive, `0` along field `+X` |
| Tag facing | Out of the printed face **toward the camera** |
| `CameraConfig` | The **lens**, not the housing |
| Camera pitch | Positive = optical axis tilted **up**; negative = tilted **down** |
| Detection bearing and tag yaw | Degrees (converted internally) |

`ftcPose.x` is camera-right; `ftcPose.y` is camera-forward. EasyATL never opens the camera.

## Pipeline

Each `localize()` call:

### 1. Decay quality

If no new pose is accepted, quality fades over time:

```text
q  ←  q * e^(-λ * Δt)
```

- `λ` is `qualityDecayRate` (default **0.8 per second**)
- `Δt` is seconds since the last update
- `λ = 0` freezes quality

### 2. Ignore unknown IDs

Tags you never registered with `addTag()` / `addCurrentGameTags()` are skipped.

### 3. Hard filters

A detection is kept only if **all** of these hold:

- `right`, `forward`, `range`, bearing, yaw, and `z` are finite numbers
- `0 < range ≤ maxRangeInches` (default **96 in**)
- `|bearing| ≤ maxBearingDegrees` (default **55°**)
- `|yaw| ≤ maxTagYawDegrees` (default **45°**)

### 4. Per-tag pose

Camera-frame measurement → robot frame (yaw, pitch, roll) → field pose using that tag’s map.

### 5. Per-tag weight

Closer, more head-on tags count more. Defaults match EasyATL 1.1.x:

```text
rangeTerm  =  1 / (1 + (range / scale)²)
angleTerm  =  max(0, cos(bearing) * cos(yaw))
marginTerm =  1                                          if decisionMargin ≤ 0
           =  clamp(decisionMargin / M, 0, 1)            otherwise

weight     =  max(minWeight, rangeTerm * angleTerm * marginTerm)
```

| Symbol | Meaning | Default |
| --- | --- | --- |
| `scale` | `weightRangeScaleInches` | 36 in |
| `M` | `decisionMarginScale` | 50 |
| `minWeight` | floor so a tag never goes to weight 0 | 0.05 |

`decisionMargin` is the FTC detector score. `0` means “unused” and does not change the weight.

### 6. Robust center (medoid)

Pick the estimate that disagrees *least* with the others (capped XY + heading distances). One bad tag cannot drag this center toward itself.

### 7. Outlier cut

Drop estimates farther than `outlierDistanceInches` (**12 in**) or `outlierHeadingDegrees` (**25°**) from that center.

### 8. Weighted mean

Average the survivors, weighted by step 5. Heading uses `atan2` of weighted sines and cosines so values near ±π (about ±180°) do **not** average to 0.

Inliers already inside the cut get Huber weight 1, so this matches the old mean unless you loosen the outlier limits.

### 9. Optional max step

If `maxStepInches` or `maxStepDegrees` is **> 0**, clamp how far one frame may jump from the previous pose. The **first** pose is never clamped. `0` (default) means unlimited.

### 10. Exponential smoothing

The first accepted pose is used as-is. After that:

```text
pose  ←  pose + α * (measurement - pose)
```

Heading is wrapped the short way. `α` is `smoothingAlpha`. **`α = 1`** means no smoothing (default **0.65**).

### 11. Quality (not a probability)

```text
q  =  clamp( meanWeight * (nIn / nEst) * countBoost * consistency,  0,  1 )

countBoost    =  min(1,  0.75 + 0.125 * nIn)
consistency   =  1 / (1 + residual / d)
```

| Symbol | Meaning |
| --- | --- |
| `meanWeight` | Average inlier weight from step 5 |
| `nIn` | Number of inliers (tags that survived the outlier cut) |
| `nEst` | Number of estimates that passed the hard filters |
| `residual` | Mean distance of inliers from the fused pose |
| `d` | `outlierDistanceInches` |

One tag has residual 0, so `consistency = 1` (same as 1.1.x for a single tag).

### 12. Uncertainty (rough, not calibrated)

`σ_xy` (sigma XY) **grows** with residual and **falls** with `√nIn` (square root of the inlier count). Use it as a *relative* gate versus odometry (“is vision tighter than wheels?”), not as inches of truth.

On failure the last pose is **kept**, accepted IDs are empty, and quality starts decaying. `getDebug()` lists per-tag weights and reject reasons: `range`, `bearing`, `yaw`, `outlier`, `stale`, `disabled`, `non-finite`.

## Camera pitch

Ignoring pitch treats optical-axis distance as floor distance. Error **grows with range**.

If the camera is pitched down by angle `θ` and the tag is near camera height, leaving pitch at 0 overstates forward distance by about:

```text
trueFloorDistance  ≈  opticalRange * cos(θ)
error              ≈  opticalRange * (1 - cos(θ))
```

Measure the **lens** pitch. Typical mounts are a few degrees down (`pitchRadians` negative). Roll is optional and defaults to 0.

## Latency

`Config.setMaxObservationAgeMs` (default **0** = off) drops a frame when `frameAcquisitionNanoTime` (or the `localize(..., timestamp)` argument) is older than that. FTC detections already carry a capture timestamp.

## Cost

`localize` runs on a handful of tags. The medoid step is O(n²) in the tag count (tiny for FTC). Unit tests run thousands of calls in well under one teleop loop. Do not skip it to “save” 20 ms.

---

[Plain English](MathButDumbed.md) · [API](API.md) · [Tuning](Tuning.md) · [Troubleshooting](Troubleshooting.md)
