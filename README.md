# Study Focus (Android)

A study-mode app blocker. Pick the apps you want to block, choose a duration, press **Start focus mode**. Until the timer ends, those apps can't be opened and there is no cancel button.

## How to build and run

1. Install **Android Studio** (latest stable) on your laptop.
2. Unzip this project. In Android Studio choose **File > Open** and select the `StudyFocus` folder.
3. Wait for the Gradle sync to finish. If it offers to upgrade the Android Gradle Plugin, you can accept.
   Android Studio may also ask to download Android SDK 34. Accept that too.
4. Connect your phone with a USB cable:
   - Phone: Settings > About phone > tap **Build number** 7 times > back > Developer options > turn on **USB debugging**.
   - Pick your phone at the top of Android Studio and press the green **Run** button.
5. To get an APK file to share: **Build > Build Bundle(s) / APK(s) > Build APK(s)**.

## First run on the phone

1. Open **Study Focus** and tap **Enable blocking permission**. Read the notice, continue, find *Study Focus* in Accessibility settings and turn it on.
   - If the switch is greyed out ("restricted setting"): Settings > Apps > Study Focus > three dots (top right) > **Allow restricted settings**, then try again.
2. Tap **Choose apps to block**. Common apps like Instagram and YouTube are ticked by default if installed.
3. (Optional) Turn on **extra protection** to make uninstalling harder during a session.
4. Pick a duration and press **Start focus mode**.

## What the app does

- Blocks the ticked apps for the whole session. Opening one sends you home and shows a "Stay focused" screen with the time left.
- During a session it also blocks **Settings, Play Store and the package installer**, so the blocker can't be switched off or uninstalled.
- The app list and duration can't be changed during a session.
- The end time is saved on the phone. Closing the app, force-stopping or rebooting does not end the session.
- Time is measured with the phone's internal clock, so changing the date/time does not shorten a session.
- Phone/dialer, emergency apps, the keyboard and the home screen are never blocked.

## Testing tip (important)

There is deliberately no cancel button, so while testing use the shortest session (5 minutes), or end a test session from your laptop with USB debugging:

    adb shell pm clear com.studyfocus.app

(After this you must turn the Accessibility permission on again.)

## Honest limits

- It can't be 100% unbreakable. Someone determined can still boot into safe mode, use an accessibility shortcut or quick tile on some phones, or factory reset. This makes quitting hard, not impossible.
- Some phone brands (Xiaomi, Oppo, Vivo, Realme, OnePlus, Samsung) kill background services aggressively. If blocking stops working, allow **Autostart** and set battery to **Unrestricted** for Study Focus.
- Android only. iPhones don't allow this kind of blocking.

## Before publishing on the Play Store

- Apps using the Accessibility Service must fill in Google's **Accessibility declaration** and have a **privacy policy** page. The in-app disclosure already exists in `MainActivity.kt`.
- Device admin use must also be explained in the Play listing.
- Raise `targetSdk` / `compileSdk` to the level Google currently requires (in `app/build.gradle.kts`). On Android 15+ you will also need to handle edge-to-edge layouts.
- Change `applicationId` (`com.studyfocus.app`) to your own unique ID and add a proper app icon.

## Project map

| File | Purpose |
|---|---|
| `MainActivity.kt` | Home screen: permission status, duration, start, locked countdown |
| `AppPickerActivity.kt` | Choose which apps to block (locked during a session) |
| `BlockerAccessibilityService.kt` | Detects the opened app and blocks it |
| `BlockedActivity.kt` | "Stay focused" screen |
| `FocusPrefs.kt` | Saves session time and the block list |
| `BlockRules.kt` | Always-blocked and never-blocked app rules |
| `FocusAdminReceiver.kt` | Optional device admin (uninstall protection) |
