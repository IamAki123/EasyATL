# EasyATL docs

Start with the [README](../README.md) if this is your first time. Use this folder when you need more than the Quick Start.

| I want to… | Open |
| --- | --- |
| Check webcam / detections first | [Prerequisites](Prerequisites.md) · [AprilTag detections](AprilTagDetections.md) |
| Add the library to an FTC project | [Install](Install.md) |
| Copy a working TeleOp | [Pedro sample](SampleOpMode.md) · [SDK sample (no Pedro)](SampleOpModeSdk.md) |
| Tune filters on the field | [Tuning](Tuning.md) · [AAR defaults](Tuning.md#aar-defaults-no-constants-file) · [Pedro files](../tuning/pedro/README.md) · [SDK files](../tuning/sdk/README.md) |
| Look up a class or method | [API](API.md) · [What each file does](LibraryFiles.md) |
| Pick DECODE / old season / custom tags | [AprilTag field sets](FieldTagSets.md) |
| Understand how localization works | [Simple explanation](MathButDumbed.md) · [Math / formulas](Math.md) |
| Fix a wrong or missing pose | [Troubleshooting](Troubleshooting.md) |

Core runtime is `FtcEasyATL.localize(...)`. SDK teams can use `DefaultSdkConstants` from the AAR without copying a constants file. Practice tools live under [`tuning/`](../tuning/) (Pedro) and [`tuning/sdk/`](../tuning/sdk/) (no Pedro). The tuners themselves are not in the JitPack AAR.
