# Glass TV KT Preview (safe side-loaded test)

Branch: codex/kt-trg01-preview. This is a **separate test app**, not the HOME launcher. The existing com.gathin.tvhome 2.3 is unaffected.

Android 14 Xiaomi MiTV-AFMU0. UI based on the previous user's HTML V3 design. Prayer zone TRG01, 42 Malay *summaries* of Imam Nawawi hadith (not verbatim matn), 5-minute rotation. JAKIM live daily data through Android native HTTPS bridge. No fabricated times if unavailable. No server needed. Wi-Fi button opens settings and app tiles try to start installed packages. Power button deliberately does not issue power commands. The Wi-Fi label is illustrative, not actual SSID.

Run GitHub Actions **Glass TV KT Preview APK**, download artifact **glass-tv-kuala-terengganu-preview-apk**, unzip then install **preview-release.apk** via Bugjaeger. Open **Glass TV KT Preview** in app list. This app has NO HOME intent filter and does not disable packages or change the default HOME.

Do not replace or remove com.gathin.tvhome. No guarantee the live JAKIM endpoint works on every network; check the status badge. QA must be completed on the actual device.

## Preview 2 fixes

- Corrected `preview=true` regression that locked the clock to 8:08 PM on 24 October 2026 and labeled real prayer readings 'CONTOH UI'. Preview 2 runs `preview=false`, using the Android TV system time converted to Asia/Kuala_Lumpur and official JAKIM prayer data (no example times).
- **New package name** `com.shafeekz.glasstv.ktpreview2` because GitHub-hosted build runners generate a fresh debug signing certificate. This means Preview 2 installs alongside Preview 1 rather than producing a `SIGNATURE_MISMATCH` error. It also continues to leave the installed HOME launcher `com.gathin.tvhome` untouched.
- In Bugjaeger open `am start -n com.shafeekz.glasstv.ktpreview2/com.shafeekz.glasstv.ktpreview.PreviewActivity` after installing the new artifact. Settings, apps, and live JAKIM status still need testing on the TV.
