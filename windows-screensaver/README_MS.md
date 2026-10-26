# Glass TV Kuala Terengganu — Screensaver Windows 11
Windows `.scr` sebenar, bukan APK. UI berdasarkan mockup TV yang diuji.

Fungsi: jam Malaysia, tarikh Masihi & Hijrah (Umm al-Qura, anggaran), solat TRG01 (e-Solat JAKIM dengan sandaran Solat.my), 42 ringkasan Hadis Nawawi bertukar setiap lima minit, visual dock dan empat butang.

**Windows screensaver bukan launcher.** Butang aplikasi, Wi-Fi, Settings, Power ialah paparan hiasan; apa-apa input menutup screensaver. Label National Geographic ialah hiasan UI dan bukannya feed rasmi. Tiada data cuaca langsung.

## Cara bina
Buka Actions > Windows 11 Glass TV Screensaver pada GitHub, jalankan untuk branch `codex/windows-11-screensaver` atau tunggu workflow selepas push. Apabila hijau, muat turun artifact **GlassTV-KualaTerengganu-Windows11-SCR**, kemudian unzip kepada `GlassTV-KualaTerengganu.scr`. Tidak memerlukan Python di PC anda; Microsoft Edge WebView2 Runtime diperlukan (biasanya tersedia di Windows 11). Binaan tidak ditandatangani; gunakan hanya artifact dari repositori anda.

## Uji
Buka Command Prompt pada direktori yang mengandungi fail dan jalankan `GlassTV-KualaTerengganu.scr /s`. Gerakkan tetikus atau tekan kekunci untuk keluar. Pastikan waktu solat bukan simulasi dan pemeriksaan rangkaian berjaya.

## Install
Selepas ujian, salin SCR ke `C:\Windows\System32\` (perlukan pentadbir); buka **Settings > Personalization > Lock screen > Screen saver** atau cari **Change screen saver** pada Start. Pilih GlassTV-KualaTerengganu dan tetapkan Wait. Untuk pulihkan keadaan, pilih None atau screensaver asal. Jangan padam screensaver asal.

**Had:** Paparan /p preview disokong, panel /c menunjukkan penerangan. Perlukan rangkaian untuk solat terkini. Data hari sama boleh diambil daripada cache jika sambungan gagal dengan label jelas. Tarikh Hijrah anggaran mungkin berbeza daripada pengisytiharan rasmi tempatan.

Sumber: https://learn.microsoft.com/en-us/previous-versions/windows/desktop/ms686421(v=vs.85) ; https://learn.microsoft.com/en-us/microsoft-edge/webview2/concepts/distribution
