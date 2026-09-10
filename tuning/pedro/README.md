# Pedro EasyATL (copy into TeamCode)

These files are **Pedro Pathing only**, for practice, not matches. Match code still calls `FtcEasyATL.localize(...)`. Full guide: [docs/Tuning.md](../../docs/Tuning.md). Road Runner / no Pedro: [SDK tuner](../sdk/README.md).

**Copy** `PedroEasyATLConstants.java`, `PedroEasyATLTuning.java`, `PedroPoseCorrector.java`, and optionally `PedroEasyATLSample.java` into TeamCode. Keep Constants, tuner, and `PedroPoseCorrector` in the **same package** (`org.firstinspires.ftc.teamcode.easyatl`). They are **not** in the EasyATL AAR.

1. Edit **only** `PedroEasyATLConstants`. Fill in `createFollower` with **your** Pedro `Constants.createFollower`. Webcam uses FTC `VisionPortal` (`Webcam 1` unless you change `WEBCAM_NAME`).
2. Driver Station → **EasyATL Tuning**. Start with **Vision telemetry** — that screen does **not** create a follower.
3. Driving tests and **Apply vision correction** call `createFollower` the first time they run.
4. Paste tuner Config snippets into `PedroEasyATLConstants.config()`.

Pose correction uses `PoseCorrector` / `PedroPoseCorrector`, not a hard-coded `follower.setPose` in every OpMode.
