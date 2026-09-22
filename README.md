# LibreShot

iOS-style screenshots for stock Android: capture, peek in the corner, mark up, copy or
save - and nothing ever touches your gallery unless you say so.

Built for GrapheneOS/Pixel (Quick Tap) and tested on Samsung One UI (Quick Settings
tile). See `DEVIATIONS.md` for where this MVP differs from the original product
spec (an internal document, not included in this repo).

## How it works

- Capture uses `AccessibilityService.takeScreenshot()` - no MediaProjection prompt, no
  system shutter UI, and **no file is written** at capture time. The bitmap lives in
  memory until you Save / Copy / Share.
- A transparent trampoline activity (`CaptureActivity`) is what Quick Tap launches on
  Pixel. The Quick Settings tile collapses the shade and triggers the same path.
- The corner thumbnail is a `TYPE_ACCESSIBILITY_OVERLAY` Compose view drawn by the
  service - no overlay permission prompt.
- The editor is fullscreen Compose markup: pen / marker / pencil / eraser, colours,
  undo/redo, pinch zoom, three-finger undo/redo. Done opens the iOS-style sheet:
  Save to Photos / Copy / Copy and Delete / Delete Screenshot.

## Privacy

- **No `INTERNET` permission.** Verify: App Info → Permissions, or
  `aapt dump permissions app-release.apk`.
- The accessibility service requests only `canTakeScreenshot` (and
  `canRequestFilterKeyEvents` for the optional volume chord). It cannot read window
  content.
- The optional back-tap trigger reads the accelerometer only while the screen is
  on and the toggle is enabled. No motion data is stored or leaves the process.
- No analytics, no crash SDKs, no Play Services.

## Setup

1. Install the app, open it once - onboarding walks through it.
2. Enable **LibreShot capture** under Settings → Accessibility → Downloaded/Installed
   apps.
3. Trigger:
   - **Any device:** enable **Back-tap capture** in LibreShot settings and
     double-tap the back of the phone.
   - **Stock Pixel:** Settings → System → Gestures → Quick Tap → Open app →
     LibreShot (GrapheneOS/AOSP builds don't ship Quick Tap - use back-tap).
   - **Others (e.g. Samsung):** add the LibreShot tile to Quick Settings and tap it.
   - Optional: volume-down double-press chord (Settings inside the app).
   - Tapping the LibreShot icon opens Settings; long-press the icon for a
     Capture shortcut.

## Build

```
./gradlew assembleDebug          # debug APK
./gradlew testDebugUnitTest      # unit tests
./gradlew installDebug           # install on a connected device
```

`minSdk 33`, `targetSdk 36`. For a release build, copy `signing.properties.example` to
`signing.properties` and fill it in.

## Dev shortcuts

```
adb shell settings put secure enabled_accessibility_services app.libreshot/.capture.CaptureService
adb shell am start app.libreshot/.capture.CaptureActivity     # == Quick Tap trigger
adb shell service call clipboard 2                            # inspect clipboard
```

## License

MIT (final license pending).
