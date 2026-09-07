# Contributing

## Build and test

You need **JDK 17** (JitPack and CI use 17; the library bytecode is Java 8).

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

Public API changes should include Javadoc and a README note. Localization math should include a unit test with a known pose and generated observations (see `KnownPoses` and `EasyATLTest`).

## Bugs

Use the bug report template. Include EasyATL version, FTC SDK version, camera/tag setup, and telemetry (pose, quality, visible vs accepted tags).
