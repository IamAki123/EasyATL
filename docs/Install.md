# Install

[README](../README.md) · [Sample OpMode](SampleOpMode.md) · [Troubleshooting](Troubleshooting.md)

Current JitPack version: **1.2.1** (must match a [git tag](https://github.com/IamAki123/EasyATL/releases)).

## What you need

- An FTC SDK **11.1.0+** Android Studio project (`FtcEasyATL` uses Vision + RobotCore)
- Android Studio’s Embedded JDK for the Gradle JVM (11+). EasyATL’s bytecode is Java 8
- Core `EasyATL` has no Pedro or Road Runner dependency. Copy-in tuners are **not** in the JitPack AAR. Pedro tuner/sample vs [SDK tuner](../tuning/sdk/README.md).

Building *this* GitHub repo from the command line is different (JDK 17+, Android SDK): [Contributing](../CONTRIBUTING.md).

## Typical path: JitPack

Gradle will not resolve `com.github.IamAki123:EasyATL` until JitPack is a repository **and** the `implementation` line is in a `dependencies` block that TeamCode applies.

### 1. Repository

In the FTC project **root** `build.dependencies.gradle`, add JitPack to the existing `repositories` block:

```gradle
repositories {
    mavenCentral()
    google()
    maven { url = 'https://jitpack.io' }
}
```

If sync fails because `RepositoriesMode.FAIL_ON_PROJECT_REPOS` is set, add JitPack in the **root** `settings.gradle` instead:

```gradle
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = 'https://jitpack.io' }
    }
}
```

Stock FTC SDK 11.1 `settings.gradle` usually does **not** set `FAIL_ON_PROJECT_REPOS`; `build.dependencies.gradle` is the usual place.

### 2. Dependency

In `TeamCode/build.gradle`:

```gradle
dependencies {
    implementation project(':FtcRobotController')
    implementation 'com.github.IamAki123:EasyATL:1.2.1'
}
```

If `build.dependencies.gradle` already holds Pedro or other libraries, you can put the EasyATL `implementation` line in that file’s `dependencies` block instead.

### 3. Sync

File → Sync Project with Gradle Files.

**You are done when** the project syncs and Android Studio can autocomplete `org.firstinspires.ftc.easyatl.FtcEasyATL`.

Next: SDK / Road Runner can call `new FtcEasyATL()` or `DefaultSdkConstants.createLocalizer()` immediately. Pedro teams copy [`PedroEasyATLConstants`](../tuning/pedro/PedroEasyATLConstants.java). Continue in the [README](../README.md) (**2. Configure the robot once**).

## Optional: local module

Use this if you want to edit EasyATL source next to TeamCode instead of JitPack. This is the supported non-JitPack path (Maven Central is not published).

1. Clone or copy this repository so it sits next to `TeamCode` (for example `YourFtcProject/EasyATL/` with this project’s `build.gradle` and `src/`).
2. `include ':EasyATL'` in the FTC project’s root `settings.gradle`.
3. `implementation project(':EasyATL')` in `TeamCode/build.gradle` (no JitPack coordinate needed for EasyATL).
4. Sync with Android Studio’s Embedded JDK. The module name must match `include`; do not `implementation project(':easyatl')` unless you included that name.

---

[README](../README.md) · [Sample OpMode](SampleOpMode.md) · [API](API.md) · [Troubleshooting](Troubleshooting.md)
