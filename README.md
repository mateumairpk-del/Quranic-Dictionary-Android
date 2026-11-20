# Quranic Dictionary Android Studio project

Open this folder in Android Studio (JDK 17), allow Gradle sync, then **Run** on an Android 8+ device or emulator. Android Gradle Plugin 8.7.3 and Kotlin 2.0.21 require internet on first Gradle sync.

- **Offline:** all HTML, CSS, Arabic/Urdu fonts, icons, and 500 vocabulary entries are packaged under `app/src/main/assets`. WebView uses `file:///android_asset/index.html`; no internet is required for normal use. Web PWA service workers do not run on file URLs; the native app does not need them.
- **Reminders:** WebView reminder settings are passed through `AndroidBridge` to Android AlarmManager. Android 13+ requires notification permission. On Android 12+, exact timing may require the user to grant "Alarms & reminders" special access. Otherwise alarms are inexact. Device power management can delay alarms; force-stopped apps will not receive alarms until reopened.
- **Boot:** reboot and timezone changes reschedule reminders. Scheduled alerts work without the app open, subject to OS permission and battery policies.
- **Random alerts:** random alarm interval between 45 and 210 minutes. Test button requests a native notification.
- **Limitations:** The original web UI describes web notifications and repeat behavior; native implementation handles `daily`, `weekdays`, `every2h`, `every4h`. It uses motivational reminder messages, not exact web quote text. Exact alarm permission is not automatically granted. This source package is not a compiled APK, and actual device testing is still required.
- **Content:** 374 added words were curated in the prior version and need scholarly review before publication.

To build: Android Studio > Open > this directory > wait for sync > Build > Build APK(s).
