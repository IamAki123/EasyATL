# EasyATL Tuning (copy into TeamCode)

These two files are for **practice**, not matches. Match code still calls `FtcEasyATL.localize(...)`. Full guide: [docs/Tuning.md](../docs/Tuning.md). First-time setup: [README](../README.md).

**Copy** `EasyATLConstants.java` and `EasyATLTuning.java` into TeamCode (**same package**). They are **not** in the EasyATL AAR — do not put them under this repo’s `src/main`.

1. Edit **only** `EasyATLConstants` (AprilTags, camera mount, `Config`, webcam, Pedro follower). Replace the Super Sigma placeholder imports. Keep other robot settings in your existing Pedro `Constants`.
2. Driver Station → **EasyATL Tuning**. **Before Play**, use the D-pad on the list:
   - **Up / down** — move
   - **Right** — select that test
   - **Left** — go back  
   Start with **Vision telemetry**.
3. Press **Play**. Drive with sticks and triggers. On a filter test, D-pad now changes that setting (up/down small, left/right large). Range/bearing/yaw screens show PASS/FAIL. Filter tests print a snippet to paste into `EasyATLConstants.config()`.
4. Match TeleOp should call `createFollower` / `createWebcam` / `createLocalizer` from Constants **in `init()`**.

Requires Pedro Pathing (`SelectableOpMode`).
