# Validation

Version 1.2.0 was built with JDK 17 and Android SDK 35.

```sh
./gradlew assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug
```

- App APK and instrumentation APK: built successfully.
- Android Lint: no errors.
- 27 passing checks on Android 12 and Android 15, including the screenshot capture on API 35.
- Checks cover per-component sizes and colors, widget isolation, custom color application, and all clock modes.
- The settings regression check changes the theme, edits text, steps a size control and confirms the same preview, input view, focus and scroll position remain in use.
- System text scale at 200% is checked for control target sizes and unclipped button labels. The color dialog is checked at that scale as well.
- Clock text colors are checked against a 4.5:1 contrast target, and hands/marks against 3:1, on several light and dark faces.
- Persian calendar, Tehran midnight, launcher/configuration routing, rendering memory and system-service permissions remain covered.

The APK uses the same signing certificate as 1.1.0 and 1.1.1. Those versions can update in place.

See docs/ACCESSIBILITY.md for the accessibility targets and limits. Actual TalkBack, manufacturer lock-screen behavior and launcher resizing still need device checks; the automated framework checks do not certify every device or configuration.
