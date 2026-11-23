# Validation

Version 1.1.1 was built with JDK 17 and Android SDK 35.

```sh
./gradlew assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug
```

- Debug APK and instrumentation APK: built successfully.
- Android Lint: no errors.
- Framework checks: 14 passing checks on Android 12 and Android 15 with native graphics.
- Screenshot capture: one passing check that opens and captures the settings and full-screen Activities on API 35.
- Calendar example: Monday, 2 Azar 1405, including the next midnight in Tehran.
- Launcher/configuration check: a normal app launch shows the main controls; a valid widget ID opens widget configuration.
- Settings saved by early builds under widget ID zero are migrated to app defaults.
- Widget views, themes, clock modes, high-resolution rendering and service permissions are covered by the checks.

The published APK uses the same signing certificate as 1.1.0. Users of 1.1.0 can update in place. The earlier 1.0.0 build has a different certificate.

Launcher resizing, screensaver activation, keyguard behavior and lock-screen wallpaper support should also be checked on the intended phone. Framework tests do not reproduce a manufacturer's lock screen or Always On Display.
