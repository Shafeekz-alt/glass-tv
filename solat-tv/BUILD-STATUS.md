# Verified build — 24 October 2026

Source commit: `2382a9400bb3e5c13a923287475b39cf2be6b7a3`.
Run: https://github.com/Shafeekz-alt/glass-tv/actions/runs/40092291214

- GitHub Actions `testDebugUnitTest assembleRelease`: passed.
- Live official JAKIM TRG01 current-month zone, dates, completeness and ordered prayer times: passed. JAKIM returns Malay month names; parser now supports these, English abbreviations and ISO dates, with regression tests.
- Licensed azan downloaded successfully and verified inside the APK.
- APK extracted from the downloaded artifact; archive digest verified against GitHub.
- Nine local scheduling checks passed; ten production Java files passed syntax parsing.

APK SHA-256: `8c7da229ecbd91c0619b3cce65c0801181ede5f6b3576b776ff1b29d3a9cc9eb`.

Still requires device validation: Android TV installation, dream selection/exit, live photos, audio playback/focus, and comparison with the user's daily JAKIM timetable. Azan only operates while the screensaver is active; it does not wake a sleeping/off TV or interrupt another app. No changes have been sent to the user's TV.

The APK uses a temporary debug signing key from the CI runner. Future compatible updates require persisting a signing key, otherwise uninstall/reinstall is needed.
