# Troubleshooting

[README](../README.md) · [Install](Install.md) · [Tuning](Tuning.md)

Check **geometry in `EasyATLConstants`** (tag map and lens) before filter knobs. Do not write vision into Pedro until taped poses match. Correct only when `localize()` returns true and quality is high enough for *your* robot. A tag on screen is not an accepted estimate.

## Symptom → check

| Symptom | Check |
| --- | --- |
| `easyatl` cannot resolve / class not found | JitPack repository **and** `implementation` line, or `include ':EasyATL'`. Then Gradle sync. [Install](Install.md) |
| Sync: repositories may only be declared in `settings.gradle` | Add JitPack under `dependencyResolutionManagement` in root `settings.gradle` ([Install](Install.md)) |
| Raw tags = 0 | Webcam init, lighting, tag size, lens focus. EasyATL never opens the camera. |
| Raw tags > 0 but `ftcPose` is “no FTC pose” | SDK has an ID without a pose. EasyATL skips those detections. |
| Visible empty, tags on screen | Tag IDs not passed to `addTag()`. Unconfigured IDs are ignored. |
| Pose mirrored or rotated | Tag **facing** and camera yaw before changing filters |
| Constant offset (same error everywhere) | Lens vs robot center, and tag field X/Y |
| Error grows with distance | Camera **pitch**, lens vs housing, or calibration — not a missing range filter |
| Pose frozen after tags disappear | Expected. Last vision pose is kept; let odometry run until a tag is accepted again |

## Conventions

- Inches and radians. Heading `0` along field `+X`, CCW-positive.
- `CameraConfig` is the **lens**, not the housing. Negative forward = behind center; negative right = left of center; negative yaw = camera pointed right; negative pitch = tilted down.
- `getQuality()` is a heuristic in `[0, 1]`, not a probability. `getConfidence()` is a deprecated alias (removal planned for a future major version).
- EasyATL does not publish a universal inch-error; measure your robot. See [Math](Math.md) for weights and quality.

Still stuck? Open a [bug report](https://github.com/IamAki123/EasyATL/issues/new?template=bug_report.md) with EasyATL version, FTC SDK version, and telemetry (pose, quality, visible vs accepted tags).

---

[README](../README.md) · [Install](Install.md) · [Sample OpMode](SampleOpMode.md) · [API](API.md)
