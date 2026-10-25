# StandbyOS 2.0 — Android StandBy-inspired display

Native Java Android application inspired by iOS StandBy. Works offline, requires no ads, account or network access.

## Features

- 6 clock faces: digital, split flip-style, analog, world clocks, minimalist, and dashboard
- Swipe horizontally between faces; tap to reveal controls; long press to configure
- Interactive dashboard widgets: clock, date/calendar, battery, stopwatch/timer, custom photo, and world time
- Optional red night mode, battery charging indicator, adjustable dimming, clock burn-in shift
- Keep display awake while viewing StandbyOS; optional charging-only behavior
- User-selected photo persisted via Android document picker; no media library permission
- Landscape mode for desk/charger use

**Android limitations:** This is a foreground app, not the system lock screen. Android does not guarantee automatic launches when the phone is placed on a charger. This project does not claim native iOS features such as iPhone MagSafe dock detection, real Apple widgets, iOS notifications, Always-On hardware control, or Live Activities.

## Build a real APK

With Android Studio, open the project and select **Build > Build APK(s)**. Or push the project to a GitHub repository and run **Actions > Build Android APK > Run workflow**, then download `StandbyOS-2.0-debug-apk` from the completed run. Requires JDK 17, Android SDK platform 35 and Gradle 8.10.2.

The generated debug build has the same applicationId `com.standbyos.android` as the uploaded example APK. Because the new build uses a different signing key, uninstall the previously installed APK before installing this build (which may reset existing settings).

Not associated with Apple Inc. Design is inspired by StandBy, not a byte-for-byte copy of proprietary Apple assets.
# standbyos
# standbyos
