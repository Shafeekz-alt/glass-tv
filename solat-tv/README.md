# Waktu Solat TV — Kuala Terengganu

Native Android TV screensaver requested for Xiaomi TV Box S 3rd generation (MiTV-AFMU0), Android 14. Separate app package `my.solat.tv`, Android 8+.

## Current status

- APK successfully compiled with `testDebugUnitTest assembleRelease` on GitHub Actions. Scheduling and Malay-date parser tests passed. Nine dependency-free scheduling checks also passed locally.
- The official current-month JAKIM response for TRG01 passed a live schema/date/time check on 24 October 2026. Actual TV display, photo loading and azan playback still need device testing.
- Verified build: https://github.com/Shafeekz-alt/glass-tv/actions/runs/40092291214 (source commit `2382a9400bb3e5c13a923287475b39cf2be6b7a3`).
- The optional bundled azan must be downloaded before building (see `tools/download-audio.sh` and attribution). Without it, the UI explicitly says no audio is configured and allows choosing a local audio file.

## Behavior

- Location fixed to Kuala Terengganu, official JAKIM zone **TRG01** (Kuala Terengganu, Marang, Kuala Nerus).
- Source: https://www.e-solat.gov.my/index.php?pageId=50&siteId=24
- Monthly JSON request, made directly by the TV over HTTPS:
  `https://www.e-solat.gov.my/index.php?r=esolatApi/takwimsolat&period=month&zone=TRG01&year=2026&month=10`
  Year/month are calculated at runtime in `Asia/Kuala_Lumpur`. The current-month endpoint was live-tested; its schema is `status`, `zone`, and `prayerTime` with dated prayer entries.
- Shows Subuh, Syuruk, Zohor, Asar, Maghrib, Isyak. Next-prayer countdown and azan exclude Syuruk.
- Only the five obligatory prayer events play azan, and only when the screensaver was already active at that event's scheduled time. There is no background alarm while another app is open or the TV is off/asleep.
- At most a 30-second scheduling grace period, no late catch-up when the screensaver starts, persistent per-date/per-prayer deduplication. Button press exits the dream and stops audio. Preview does not trigger scheduled sound.
- Audio respects TV media volume/mute and audio focus. Use the clearly labeled audio-test button before relying on it. A custom recording can be selected with Android's file picker if available on the TV.
- Cache is bound to TRG01 and the actual dates; invalid zones, incomplete months, missing or unordered times are rejected. Missing current-day data shows no timetable and triggers no azan. Internet failures show an explicit cache/unavailable message. Data refreshes every six hours on success or retries every 15 minutes, plus Malaysia date change.
- Dim display with bounded slow motion. No wake lock, location permission, analytics, account, ads, launcher replacement or package disabling.
- Four real mosque photographs rotate every two minutes, with two-second crossfades and a dark overlay: Masjid Kristal (Malaysia), Sultan Ahmed Mosque (Turkey), Omar Ali Saifuddien Mosque (Brunei), and Putra Mosque (Malaysia). Photos download directly from Wikimedia on the TV, are cached in private app storage, and are downsampled before display. Prayer data/audio do not depend on photo downloads succeeding. The captions and complete author/source/license credits are included in the app.

## Build

Requires JDK 17+, Gradle 8.9, Android SDK platform 34 and build-tools 34.0.0. AGP is 8.7.3. Configure `ANDROID_HOME` to the SDK directory; accept its licensing through the normal SDK installation process.

```sh
bash tools/download-audio.sh
gradle --no-daemon testDebugUnitTest assembleRelease
```

Output: `app/build/outputs/apk/release/app-release.apk`. This personal sideload build uses Gradle's local debug signing key; preserve that key for compatible updates. It is not a publisher-signed Glass TV release. The GitHub Actions artifact contains the signed personal-build APK; artifacts expire after seven days.

In the Glass TV repository, this app lives in `solat-tv/`. The root workflow `.github/workflows/waktu-solat.yml` builds it on changes to this folder or via **Actions → Build Waktu Solat TV APK → Run workflow**. Download **Waktu-Solat-TV-APK** from a successful run and extract `app-release.apk` on the phone. The original Glass TV launcher is a separate app.

## Offline scheduling checks

```sh
mkdir -p build/core-checks
java -m jdk.compiler/com.sun.tools.javac.Main -d build/core-checks app/src/main/java/my/solat/tv/PrayerSchedule.java tools/CoreChecks.java
java -ea -cp build/core-checks CoreChecks
```

These run the real scheduling class; they are not an Android emulator or JUnit run. Standard JUnit4 tests are also included for the Gradle build.

## Later installation using the connected phone

After the APK is built and verified: download it on the phone, use Bugjaeger Packages → + → install APK on the selected MiTV-AFMU0. Open **Waktu Solat** from the TV app list, verify the day's times against JAKIM and use **Uji bunyi azan**. Use **Buka tetapan screensaver TV** and select **Waktu Solat • Kuala Terengganu**.

If the TV hides screensaver selection, first record the original value from `settings get secure screensaver_components` using Bugjaeger's shell. Only after successful preview and audio tests, set `settings put secure screensaver_components my.solat.tv/.PrayerDream` and `settings put secure screensaver_enabled 1`. Restore the recorded component to return to the previous screensaver. No TV changes have been performed by this source preparation.

## Credits

Prayer timetable: JAKIM e-Solat. This is an independent personal app, not an official JAKIM product.

Optional audio, unmodified: **The Adhan – Muslim Call to Prayer – Aaqib Azeez**, author **Atcovi**, Wikimedia Commons, **CC BY-SA 4.0**.
- https://commons.wikimedia.org/wiki/File:The_Adhan_-_Muslim_Call_to_Prayer_-_Aaqib_Azeez.mp3
- https://creativecommons.org/licenses/by-sa/4.0/

The recording was downloaded by the successful build and verified present in the APK. It has not been auditioned on the TV. Its SHA-256 is `0faa59e2028bbb2f60a670b53ec89ffcf628f3c195b43fb41d9bdbc99000c1b3`. It is a general azan recording; a separate Subuh recording can be added later if requested. No audio recording is attributed to JAKIM.

Photo catalog and attribution: `app/src/main/assets/mosques.json`. CC BY-SA licenses apply to their respective photos. Rendering crops them to fill the screen and adds a dimming layer; original cached image bytes are not altered. Live in-app download/rendering has not yet been device-tested.
