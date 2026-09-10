# EasyATL SDK tuner (copy into TeamCode)

These files are for **practice**, not matches. They do **not** use Pedro Pathing. Match code still calls `FtcEasyATL.localize(...)`. Full guide: [docs/Tuning.md](../../docs/Tuning.md) (including [AAR defaults](../../docs/Tuning.md#aar-defaults-no-constants-file)).

## AAR defaults (you can skip copying Constants)

The tuner and sample use **`DefaultSdkConstants`**, which **is** in the EasyATL AAR. They compile even if you never copy a constants file.

| What | Default |
| --- | --- |
| Webcam | `Webcam 1` |
| Camera | Lens at robot center, pointed forward, level |
| Config | Library `new EasyATL.Config()` |
| Tags | Latest bundled season (`useLatestSeason()`, DECODE 20 and 24) |

**Copy** `EasyATLSdkTuner.java` and optionally `EasyATLSdkSample.java` into TeamCode. Those OpModes are **not** in the AAR.

Copy `EasyATLSdkConstants.java` only when you need different lens numbers, webcam name, tags, or a tighter `config()`. Put it in package `org.firstinspires.ftc.teamcode.easyatl`. Then in **`init()`** of the tuner (and sample, if you copied it) replace every `DefaultSdkConstants` with `EasyATLSdkConstants`. Exact lines: [Tuning → Where to switch](../../docs/Tuning.md#where-to-switch-to-easyatlsdkconstants). Do not edit `DefaultSdkConstants` itself.

1. Hardware config webcam name must match (`Webcam 1` unless you override). Tape the **lens** before trusting pose. Past seasons or `customAprilTags()`: [FieldTagSets](../../docs/FieldTagSets.md).
2. Driver Station → **EasyATL Tuner**. In **init**, D-pad up/down picks a test. Press **Play**.
3. After Play, D-pad up/down = small step, left/right = large step on that knob. Telemetry shows PASS-style OK/REJECT per tag, residual, and a Config snippet (paste into `EasyATLSdkConstants.config()` once you have copied that file).
4. There is no drivetrain here — push the robot or run your own TeleOp beside it.

Pedro teams can keep using [`../pedro/PedroEasyATLTuning.java`](../pedro/PedroEasyATLTuning.java) instead.
