# Glass TV KT Preview (safe side-loaded test)

Branch: codex/kt-trg01-preview. This is a **separate test app**, not the HOME launcher. The existing com.gathin.tvhome 2.3 is unaffected.

Android 14 Xiaomi MiTV-AFMU0. UI based on the previous user's HTML V3 design. Prayer zone TRG01, 42 Malay *summaries* of Imam Nawawi hadith (not verbatim matn), 5-minute rotation. JAKIM live daily data through Android native HTTPS bridge. No fabricated times if unavailable. No server needed. Wi-Fi button opens settings and app tiles try to start installed packages. Power button deliberately does not issue power commands. The Wi-Fi label is illustrative, not actual SSID.

Run GitHub Actions **Glass TV KT Preview APK**, download artifact **glass-tv-kuala-terengganu-preview-apk**, unzip then install **preview-release.apk** via Bugjaeger. Open **Glass TV KT Preview** in app list. This app has NO HOME intent filter and does not disable packages or change the default HOME.

Do not replace or remove com.gathin.tvhome. No guarantee the live JAKIM endpoint works on every network; check the status badge. QA must be completed on the actual device.
