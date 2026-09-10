# Tuning EasyATL

[README](../README.md) · [Pedro files](../tuning/README.md) · [SDK files](../tuning/sdk/README.md) · [Sample OpMode](SampleOpMode.md)

`EasyATLTuning` (Pedro) and `EasyATLSdkTuner` (plain FTC SDK) are for the practice field or pit. Do not select them during a match. Match TeleOp and auto should call `FtcEasyATL.localize(...)`.

> **Geometry first.** Filtering cannot fix a wrong camera mount, camera yaw/**pitch**, or AprilTag field pose. Confirm IDs, tag X/Y/facing, and **lens** forward/right/yaw/pitch with a tape (and **Vision telemetry**) before turning knobs. The housing is not the lens.

**On this page:** [Copy these files](#copy-these-files) · [AAR defaults](#aar-defaults-no-constants-file) · [Where to switch](#where-to-switch-to-easyatlsdkconstants) · [Pick a test](#pick-a-test-before-play) · [After Play](#after-play-driving-and-knobs) · [Library Config defaults](#library-config-defaults) · [Field procedure](#field-procedure) · [Measuring accuracy](#measuring-accuracy)

## Copy these files

**Pedro:** copy [`EasyATLConstants.java`](../tuning/EasyATLConstants.java) and [`EasyATLTuning.java`](../tuning/EasyATLTuning.java) into TeamCode in the **same package**. Pedro still needs that constants file (webcam helper and follower live there).

**Road Runner / no Pedro:** copy [`EasyATLSdkTuner.java`](../tuning/sdk/EasyATLSdkTuner.java) (and optionally the [SDK sample](SampleOpModeSdk.md)). You do **not** have to copy a constants file — see [AAR defaults](#aar-defaults-no-constants-file). No drivetrain in the tuner — D-pad picks a knob in init, then changes that value after Play.

The tuner OpModes themselves are not in the JitPack AAR. `DefaultSdkConstants` **is** in the AAR.

Requires Pedro Pathing (`SelectableOpMode`) only for the Pedro tuner. Keep drivetrain PID and motor names in your existing Pedro `Constants`.

```text
DefaultSdkConstants (AAR)          SDK tuner / sample
  or EasyATLSdkConstants      →    pick test, paste snippet
  or EasyATLConstants (Pedro)      Match TeleOp: localize(...)
```

`config()` is a method on the constants class you are using, not a separate file. After the tuner prints a snippet, paste it into that method.

**Pedro** match OpModes should already call:

```java
follower = EasyATLConstants.createFollower(hardwareMap);
webcam = EasyATLConstants.createWebcam(hardwareMap, telemetry);
localizer = EasyATLConstants.createLocalizer(EasyATLConstants.config());
```

**SDK** match OpModes can start with the AAR (`DefaultSdkConstants` / `new FtcEasyATL()`). After you copy Constants, change those calls in `init()` — exact lines: [Where to switch](#where-to-switch-to-easyatlsdkconstants).

## AAR defaults (no constants file)

SDK / Road Runner code compiles and runs without copying `EasyATLSdkConstants`. The library ships [`DefaultSdkConstants`](LibraryFiles.md#defaultsdkconstantsjava) inside the AAR. `new FtcEasyATL()` is the same localizer as `DefaultSdkConstants.createLocalizer()`.


| What               | AAR default (`DefaultSdkConstants`)                                              |
| ------------------ | -------------------------------------------------------------------------------- |
| Webcam name        | `Webcam 1` (must match your hardware config)                                     |
| Camera             | Lens at robot center: forward `0`, right `0`, yaw `0`, pitch `0`                 |
| Pipeline           | `new EasyATL.Config()` — see [Library Config defaults](#library-config-defaults) |
| Tags               | `useLatestSeason()` (DECODE goal tags 20 and 24)                                 |
| Processor / portal | Stock `AprilTagProcessor` + `VisionPortal` on that webcam                        |


The SDK tuner and sample already call this class. Copy only the OpMode if you want to try vision on the field tonight.

Copy [`EasyATLSdkConstants.java`](../tuning/sdk/EasyATLSdkConstants.java) into TeamCode when **any** of these is true:

- The lens is not at robot center (tape forward/right/yaw/**pitch**)
- The USB webcam is not named `Webcam 1`
- You need a past season, homemade tags, or `addCurrentGameTags()`
- You want a tighter starting `config()` than the library defaults (the copy-in sample uses 72 in range and 0.70 smoothing)



### Where to switch to EasyATLSdkConstants

You do **not** edit `DefaultSdkConstants` (that class is inside the library). You copy `EasyATLSdkConstants`, put your lens/webcam/tags/`config()` **in that file**, then point each OpMode’s `init()` at it.

Do this in every SDK OpMode you copied or wrote:


| File                                                           | Package                                                      | What to change                                                        |
| -------------------------------------------------------------- | ------------------------------------------------------------ | --------------------------------------------------------------------- |
| [`EasyATLSdkTuner.java`](../tuning/sdk/EasyATLSdkTuner.java) | `org.firstinspires.ftc.teamcode.easyatl` (same as Constants) | The four `DefaultSdkConstants` lines in `init()` |
| [`EasyATLSdkSample.java`](../tuning/sdk/EasyATLSdkSample.java) | `org.firstinspires.ftc.teamcode` | The three `DefaultSdkConstants` lines in `init()`, plus the import |
| Your match TeleOp / auto                                       | whatever you used                                            | The same `createProcessor` / `createPortal` / `createLocalizer` calls |


Keep `EasyATLSdkConstants` in `org.firstinspires.ftc.teamcode.easyatl` (the package already on that file).

**Tuner** — same package as Constants. Delete the `DefaultSdkConstants` import (you do not need a new import). Then in `init()`:

```java
processor = EasyATLSdkConstants.createProcessor();
portal = EasyATLSdkConstants.createPortal(hardwareMap, processor, telemetry);
config = EasyATLSdkConstants.config();
localizer = EasyATLSdkConstants.createLocalizer(config);
```

**Sample / TeleOp / auto** — different package, so add this import (and drop `DefaultSdkConstants`):

```java
import org.firstinspires.ftc.teamcode.easyatl.EasyATLSdkConstants;
```

Then in `init()`:

```java
processor = EasyATLSdkConstants.createProcessor();
portal = EasyATLSdkConstants.createPortal(hardwareMap, processor, telemetry);
localizer = EasyATLSdkConstants.createLocalizer(EasyATLSdkConstants.config());
```

Processor and portal helpers in the copy-in file still delegate to the AAR. Camera, webcam name, tags, and `config()` are what you override in `EasyATLSdkConstants`.

Pedro teams cannot skip constants: `EasyATLConstants` still holds your webcam helper and Pedro follower. That file is not in the AAR.

Tag-map choices: [AprilTag field sets](FieldTagSets.md). Method list: [API](API.md#defaultsdkconstants).

## Pick a test (before Play)

Open **EasyATL Tuning** on the Driver Station. You get one list (no folders). Highlight a row, then select it. **Do not press Play until a test is selected.**


| D-pad     | Before Play (the list) |
| --------- | ---------------------- |
| Up / down | Move the highlight     |
| Right     | Select that test       |
| Left      | Go back                |


Pedro’s on-screen hint may also mention bumpers. You only need the D-pad.

The tests themselves are unchanged. Start with **Vision telemetry**.


| Test                    | What it does                                                                                                                              |
| ----------------------- | ----------------------------------------------------------------------------------------------------------------------------------------- |
| Vision telemetry        | Pose, quality, tags. Does **not** set Pedro pose.                                                                                         |
| Apply vision correction | Writes Pedro pose when `localize()` returns true and quality ≥ **0.20** (sample gate, not a library default). Use only after tape checks. |
| Max range (in)          | Live range filter. PASS/FAIL for each tag.                                                                                                |
| Max bearing (deg)       | Live bearing filter. PASS/FAIL for each tag.                                                                                              |
| Max tag yaw (deg)       | Live tag-yaw filter. PASS/FAIL for each tag.                                                                                              |
| XY outlier (in)         | Multi-tag XY outlier limit.                                                                                                               |
| Heading outlier (deg)   | Multi-tag heading outlier limit.                                                                                                          |
| Smoothing alpha         | Live pose blend (`1.0` = no smoothing).                                                                                                   |
| Quality decay rate      | How fast quality falls when no pose is accepted.                                                                                          |


The SDK tuner also exposes **Weight range scale**. Per-tag OK/REJECT reasons (`range`, `bearing`, `yaw`, `outlier`) and XY residual print on both tuners’ vision telemetry (Pedro: Vision telemetry screen; SDK: every screen).

## After Play (driving and knobs)

Drive: left stick Y, triggers strafe, right stick X.

On filter tests, the D-pad **no longer** means select/back. It changes the current setting:


| D-pad        | After Play (filter tests) |
| ------------ | ------------------------- |
| Up / down    | Small step                |
| Left / right | Large step                |


Range, bearing, yaw, outlier, smoothing, and quality tests print a Config snippet to paste into `EasyATLConstants.config()` (Pedro) or `EasyATLSdkConstants.config()` (SDK override). Vision telemetry does not. If you are still on AAR defaults, copy `EasyATLSdkConstants` before pasting.

## Library Config defaults

Filter knobs when you call `new EasyATL.Config()`, `DefaultSdkConstants.config()`, or `new FtcEasyATL()` (no custom config). The copy-in `EasyATLConstants.config()` / `EasyATLSdkConstants.config()` samples use 72 in range and 0.70 smoothing instead:


| Setting               | Default         |
| --------------------- | --------------- |
| Max range             | 96 in           |
| Max bearing           | 55°             |
| Max tag yaw           | 45°             |
| XY outlier limit      | 12 in           |
| Heading outlier limit | 25°             |
| Smoothing alpha       | 0.65            |
| Quality decay         | 0.8/s           |
| Weight range scale    | 36 in           |
| Decision-margin scale | 50              |
| Max observation age   | off (`0`)       |
| Max step XY / heading | unlimited (`0`) |


There is no universally optimal set. Tune **your** camera and field. Change **one** parameter at a time.


| Parameter                | Increase when…                             | Decrease when…                |
| ------------------------ | ------------------------------------------ | ----------------------------- |
| `maxRangeInches`         | Tags drop too early at useful distance     | Far tags are noisy            |
| `maxBearingDegrees`      | Edge-of-frame tags are still stable        | Edge detections jump          |
| `maxTagYawDegrees`       | Steep angles are still accurate            | Angled tags are bad           |
| `outlierDistanceInches`  | Good multi-tag estimates disagree a little | One bad tag yanks XY          |
| `outlierHeadingDegrees`  | Headings differ slightly                   | Heading jumps                 |
| `smoothingAlpha`         | Need faster response (`1.0` = off)         | Pose is jittery               |
| `qualityDecayRate`       | Want stale vision untrusted faster         | Want quality to linger        |
| `weightRangeScaleInches` | Far tags are still trustworthy             | Far tags dominate a close tag |


If a detection is geometrically valid but noisy, tighten range / bearing / yaw **before** outliers or smoothing. Weights are configurable; formulas: [Math](Math.md).

## Field procedure

Work on a practice field (or a taped-off area) with real AprilTags. Use **EasyATL Tuning → Vision telemetry**, or the [sample TeleOp](SampleOpMode.md) with `APPLY_VISION_CORRECTION = false`. You are comparing **tape** to **Vision pose** on the Driver Station. Do not write vision into Pedro yet.

Mark a few **stations** on the floor with tape: spots where you will park the robot, facing one tag, at known distances. Distances are **inches from the robot to that one tag**, measured with a tape measure along the floor (or along the camera’s view if that is easier to repeat). Suggested stations: **24 in, 36 in, 48 in, and 72 in**. You do not type those numbers into code. You drive or roll the robot to each mark.

### 1. Geometry check (do this first)

Park at 24 in, one configured tag in view. On telemetry, the tag ID must match `addTag(...)` or `addCurrentGameTags()`. Vision X/Y/heading should be close to what you measured on the field. If the pose is flipped, rotated, or off by a **constant** amount at every station, fix camera offsets, camera yaw/**pitch**, and tag X/Y/facing. Do not tune filters for that. Error that grows with distance is often pitch or lens-vs-housing.

### 2. One tag, four distances

Stay on **one** tag. Repeat at 24, 36, 48, then 72 in.

At each station write down: Vision pose, whether a **new** measurement was accepted this loop (`New vision measurement` / `localize()` true), visible IDs, accepted IDs, and quality.

Watch for two problems:

- **Dropped too early** — at a distance you still want to use in auto, accepted stays false or the tag fails range/bearing/yaw. Raise `maxRangeInches` (or bearing/yaw) a little.
- **Noisy far away** — accepted is true but X/Y jumps or disagrees with tape. Lower `maxRangeInches` so those far frames are ignored.

You now know roughly how far your camera is trustworthy.

### 3. Tag near the edge of the camera image

Stay at a medium distance (about 36 in). Turn in place until the tag is near the **side** of the preview, not centered.

If Vision pose stays close to tape, edge detections are usable. If it jumps, lower `maxBearingDegrees` and/or `maxTagYawDegrees` until those frames fail (FAIL on the range/bearing/yaw tests) instead of pulling the pose.

### 4. Two tags at once

Add a second tag in `addTags` and stand where **both** are in view and both IDs are configured.

`Accepted` should list **both** when they agree. If one bad detection yanks X/Y or heading, lower `outlierDistanceInches` / `outlierHeadingDegrees`. If a good tag is dropped while both look stable, raise those limits slightly.

### 5. Junk should fail, not fuse

Cover a tag, aim very steep, or stand well past the range you chose. Those frames should **not** become a new accepted pose. If they still fuse, tighten range / bearing / yaw before outliers or smoothing.

### 6. Change one knob, then re-check the same marks

Use the filter tests (Max range, Max bearing, and so on). Change **one** setting. Drive back to the **same** 24 / 36 / 48 / 72 marks and the edge-of-frame / two-tag spots. If the new value is worse, revert it.

Smoothing last: lower `smoothingAlpha` if the pose jitters after filters are reasonable; raise it (toward `1.0`) if it lags too much.

### 7. Decide you are done

Stop when:

- Tape and vision agree closely enough for your auto at the distances you will actually use
- Tags you care about still get accepted
- Bad views are rejected
- You are not still chasing a constant offset (that is still geometry)

Then paste the tuner’s Config snippet into `EasyATLConstants.config()` (Pedro) or `EasyATLSdkConstants.config()` (SDK override). If you never copied a constants file, copy `EasyATLSdkConstants` now so you have a place to paste, or call `setConfig(...)` on the localizer in `init()`.

## Measuring accuracy

EasyATL does **not** publish a universal “average error in inches.” Lighting, mount, calibration, and your tag map all change the result. Measure **your** robot.

This is **not** a snippet to paste into an OpMode. The sample TeleOp and Vision telemetry already print Vision pose. You stand at a taped station, read the Driver Station, and do the arithmetic in a notebook, spreadsheet, or the table below.

1. Keep **vision correction off** (`APPLY_VISION_CORRECTION = false`, or use Vision telemetry). Otherwise Pedro’s pose is mixed with vision and you cannot tell which is wrong.
2. Park on a mark. Measure the robot’s field pose with a tape (and a heading reference you trust). Those are **Actual** X, Y, heading.
3. Wait until telemetry shows a **new accepted** measurement for that park (`accepted` / `New vision measurement` true). If it is false, skip the trial — there is no new vision pose to score.
4. Copy **Vision pose** (EasyATL X, Y, heading) from telemetry. Also copy visible IDs, accepted IDs, and quality.
5. Compute errors yourself (calculator or spreadsheet). `hypot` is the straight-line distance between tape XY and vision XY. Heading error is the smallest angle between the two headings, in degrees:

```text
positionError = hypot(vision.x - tape.x, vision.y - tape.y)
headingErrorDeg = abs(toDegrees(wrap(vision.heading - tape.heading)))
```

**Example** (one park, 36 in from tag 21). Telemetry heading is often shown in degrees; convert to radians only if you subtract in radians. Using degrees for heading here:

```text
tape    = (36.0 in,  0.0 in, 180°)
vision  = (37.2 in,  1.1 in, 176°)

positionError   = hypot(37.2 - 36.0, 1.1 - 0.0)
                = hypot(1.2, 1.1)
                ≈ 1.6 in

headingErrorDeg = abs(176 - 180)
                = 4°
```

`wrap` only matters when headings cross ±180° (for example tape 179° and vision −179° is about 2°, not 358°). A spreadsheet `MOD(angle + 180, 360) - 180` (or the same idea in radians) is enough.

Filled-in row for that trial:


| Station       | Tape X | Tape Y | Tape heading | Vision X | Vision Y | Vision heading | Position error | Heading error | Visible | Accepted | Quality |
| ------------- | ------ | ------ | ------------ | -------- | -------- | -------------- | -------------- | ------------- | ------- | -------- | ------- |
| 36 in, tag 21 | 36.0   | 0.0    | 180°         | 37.2     | 1.1      | 176°           | 1.6 in         | 4°            | [21]    | [21]     | 0.72    |


1. Repeat at each station (one-tag at 24 / 36 / 48 / 72 in, then a two-tag spot if you use more than one tag).
2. Optional: run the same stations once with library defaults (`DefaultSdkConstants.config()` / `new EasyATL.Config()`) and once with your tuned `config()`. Keep whichever set is more accurate **without** dropping the frames you need.

Copy the table into Google Sheets, Excel, or a team notebook. It is a **record of that practice session**, not something that goes into Git or the robot. Use it to pick a config and to see if a later camera remount made things worse.


| Station (e.g. 36 in, tag 21) | Tape X | Tape Y | Tape heading | Vision X | Vision Y | Vision heading | Position error | Heading error | Visible | Accepted | Quality |
| ---------------------------- | ------ | ------ | ------------ | -------- | -------- | -------------- | -------------- | ------------- | ------- | -------- | ------- |
|                              |        |        |              |          |          |                |                |               |         |          |         |


---

[README](../README.md) · [Sample OpMode](SampleOpMode.md) · [API](API.md) · [Troubleshooting](Troubleshooting.md)