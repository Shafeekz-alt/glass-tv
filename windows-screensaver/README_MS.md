# Glass TV Islamic Screensaver — Windows 11

Screensaver **Windows .scr** (bukan APK Android TV), versi tanpa ikon aplikasi atau butang kawalan.

## Paparan

- Jam dan tarikh Masihi secara langsung dalam zon Asia/Kuala_Lumpur
- Tarikh Hijrah berdasarkan kalendar Umm al-Qura (ditanda anggaran)
- Kuala Terengganu, Terengganu — waktu solat e-Solat JAKIM `TRG01`
- Enam waktu: Subuh, Syuruk, Zohor, Asar, Maghrib dan Isyak
- Kiraan waktu solat seterusnya dan status kemas kini data
- Kad **42 ringkasan pengajaran Hadis 40 Imam Nawawi**, bertukar setiap 5 minit
- Latar lembut dengan kad lutsinar; ruang bawah kosong dan nota untuk keluar
- **Tiada YouTube, Disney+, Apple TV, BrowseHere, Main, Wi-Fi, Settings atau Power**

Semua butang dan ikon aplikasi Android TV telah dibuang daripada paparan. Screensaver tidak boleh mengubah aplikasi utama Windows; klik, pergerakan tetikus dan kekunci menamatkan paparan.

## Cara membina fail .scr

1. Buka [GitHub Actions](https://github.com/Shafeekz-alt/glass-tv/actions).
2. Pilih workflow **Windows 11 Glass TV Screensaver**.
3. Pilih **Run workflow** dan branch `codex/windows-11-screensaver` (atau buka run terkini bagi branch ini).
4. Jika status **Success** (hijau), muat turun artifact **GlassTV-KualaTerengganu-Windows11-SCR**.
5. Ekstrak ZIP — fail `GlassTV-KualaTerengganu.scr`.

Sumber di branch ini ialah projek `.NET 8 Windows Forms + Microsoft Edge WebView2`. Binaan x64 di GitHub Actions menghasilkan fail tunggal `.scr` (tidak ditandatangani), jadi muat turun hanya daripada repositori sendiri. Windows memerlukan **Microsoft Edge WebView2 Runtime**. Tidak memerlukan Python atau Android SDK pada komputer.

## Uji sebelum pasang

Buka Command Prompt/Terminal dalam folder fail screensaver, kemudian jalankan:

```bat
GlassTV-KualaTerengganu.scr /s
```

Gerakkan tetikus atau tekan sebarang kekunci untuk keluar.

**Semak:** Masa sekarang bukan 8:08 simulasi, zon `TRG01`, tarikh Hijrah bukan nama bulan Gregorian, dan status waktu solat bukan data rekaan. Jika JAKIM tidak dapat dihubungi, screensaver akan cuba sumber sandaran; jika masih gagal, waktu solat ditandakan tidak tersedia atau menggunakan cache **hari yang sama**, bukan masa rekaan.

## Pasang pada Windows 11

1. Selesai diuji, klik kanan `GlassTV-KualaTerengganu.scr` dan pilih **Install** jika menu tersebut muncul.
2. Jika tidak tersedia, salin ke `C:\Windows\System32\` dengan kebenaran pentadbir.
3. Cari **Change screen saver** dari Start.
4. Pilih **GlassTV-KualaTerengganu**, tetapkan **Wait** (cth. 5 minit) dan tekan **Preview** untuk uji; kemudian Apply.
5. Untuk kembali seperti asal: pilih None atau screensaver sebelumnya. Jangan padam fail sistem asal.

## Nota

- Tarikh Hijrah ialah anggaran astronomi dan mungkin tidak sama dengan pengisytiharan rasmi Malaysia.
- `/p` ialah paparan kecil dalam tetapan screensaver; `/c` menunjukkan maklumat.
- Menyokong berbilang monitor melalui tetingkap skrin penuh berasingan.
- Paparan tidak mengutip data peribadi; jam sistem komputer digunakan.
- Screensaver ini **bukan ciri keselamatan**. Untuk kunci komputer selepas waktu tidak aktif, aktifkan pilihan **On resume, display logon screen** dalam Windows.
- Kod dan screenshot UI masih perlu diuji pada mesin Windows sebenar sebelum dianggap siap sepenuhnya.

Dokumentasi Windows: https://learn.microsoft.com/en-us/previous-versions/windows/desktop/ms686421(v=vs.85)
