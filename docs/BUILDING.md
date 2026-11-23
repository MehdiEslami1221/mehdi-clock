# Building Mehdi Clock

## Toolchain

| Tool | Version |
| --- | --- |
| Java | 17 |
| Android compile/target SDK | 35 |
| Minimum Android SDK | 31 |
| Android Gradle Plugin | 8.9.2 |
| Gradle | 8.11.1, supplied through the wrapper |

Open the repository root in Android Studio and let Gradle sync. From a terminal:

```sh
./gradlew assembleDebug testDebugUnitTest lintDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Signing

Local debug builds use Android Studio's debug key unless signing variables are set. Such a build can have a different certificate from the APK under `downloads/`.

To create a compatible development update with the project's private key, set:

- `CLOCK_KEYSTORE_FILE`: absolute path to the private keystore.
- `CLOCK_KEYSTORE_PASSWORD`: its password. The key alias is `clock`.

The keystore and credentials are maintained separately. They are not part of this repository. Configure a release signing block before publishing a store build; `assembleRelease` is otherwise unsigned.

## Regenerate screenshots

```sh
./gradlew testDebugUnitTest --tests ir.mehdieslami.clock.ScreenshotCaptureTest
```

The test opens `MainActivity` and `FullScreenClockActivity` and captures their Activity views with native Android graphics on API 35. Files are written to `app/build/reports/screenshots/`. The checked-in copies live under `docs/screenshots/`.

## Device checks

To run the instrumentation checks on a connected Android 12+ device:

```sh
./gradlew assembleDebug assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w ir.mehdieslami.clock.test/ir.mehdieslami.clock.SmokeInstrumentation
```

Also check widget resizing in the target launcher, screensaver activation while charging, and the wallpaper picker. These behaviors depend on the device and are not covered by the framework tests.

## Publishing a version

The `downloads/` directory contains the signed APK intended for users and its checksum. The release workflow verifies that checksum and publishes that existing file to GitHub Releases. It does not rebuild or re-sign it.

For a new version, update the version in `app/build.gradle`, build with the retained signing key, replace the download and checksum, update the release notes, and push. Existing GitHub releases are left in place.
