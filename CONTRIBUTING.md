# Contributing

Using EasyATL on a robot? Start at the [README](README.md), not this page.

## Build and test

You need **JDK 17+** and an **Android SDK**. GitHub Actions and JitPack use JDK 17; the library bytecode is Java 8. Android Gradle Plugin 8.7 will not run on a Java 8 `JAVA_HOME` (a common default on Windows). Point `JAVA_HOME` at Temurin 17 or Android Studio’s JBR, and set `ANDROID_HOME` (or `local.properties` `sdk.dir`) to your SDK.

```bash
git clone https://github.com/IamAki123/EasyATL.git
cd EasyATL
./gradlew test
./gradlew build
```

On Windows use `gradlew.bat`. Android SDK is required because this is an Android library module; Android Studio's Embedded JDK and SDK are enough.

## Pull requests

1. Fork the repository.
2. Create a branch from `main`.
3. Change the code. If you fix a bug, add a regression test.
4. Run `./gradlew test`.
5. Open a pull request. Describe what changed and why.

Public API changes should include Javadoc and a note in [docs/API.md](docs/API.md). Localization math should include a unit test with a known pose and generated observations (see `KnownPoses` and `EasyATLTest`) and a short note in [docs/Math.md](docs/Math.md) if the formula changed.

## Bugs

Use the bug report template. Include EasyATL version, FTC SDK version, camera/tag setup, and telemetry (pose, quality, visible vs accepted tags).
