# W7 TV — Liquid Glass launcher & IDLIX Web TV

Custom launcher untuk TCL Android TV 11, dikembangkan dari [CleanYourTV](https://github.com/Gathin23/cleanyourtv). Pembaruan terakhir: 31 Oktober 2026, launcher **3.4.2-liquid-glass** (versionCode 17), package `com.gathin.tvhome`, dan pemutar **IDLIX Web TV 1.6.2** (versionCode 10), package `com.w7.idlixwebtv`.

## Update pemutar TV

- Kontrol remote dengan kapsul Liquid Glass **Kualitas / Subtitle / Lainnya**, timeline tanpa panel kaca besar.
- Kualitas dan opsi subtitle mengikuti pilihan yang tersedia pada pemutar web; unggah subtitle tidak disertakan.
- Bawah membuka pilihan; Bawah lagi membuka episode season aktif dengan fokus di episode yang diputar, atau maksimal 8 rekomendasi film dari genre yang sama.
- **Lainnya → Auto-hide pilihan → 5 / 10 / 15 detik** mengatur pilihan, daftar episode, dan rekomendasi. Default 10 detik; setiap interaksi mengulang timer. Submenu pengaturan tidak ditutup saat diedit.
- Opasitas pemutar mengikuti slider launcher melalui provider read-only dengan izin signature. Kedua APK harus memakai signature yang sama.

Source dan Gradle wrapper ada di [`idlix-tv/`](idlix-tv/), APK ada di [`releases/idlix-web-tv-1.6.2/`](releases/idlix-web-tv-1.6.2/), panduan serta hasil pemeriksaan ada di [`docs/idlix-web-tv/`](docs/idlix-web-tv/). Build dari folder `idlix-tv` dengan `./gradlew --no-daemon lintRelease assembleRelease`; gunakan JDK 17+ dan SDK 34. Instal APK memakai `adb install -r` agar data tetap tersimpan. Launcher pendamping tersedia di [`releases/3.4.2-liquid-glass/`](releases/3.4.2-liquid-glass/); rincian sinkronisasi ada di [panduan opasitas](docs/liquid-glass/OPACITY-SYNC.md).

Versi 1.6.2 lolos build/lint dan 9 pemeriksaan fixture Android dengan jam nyata/input remote untuk timer episode, pembatalan timer, pembukaan ulang dan preferensi. Fixture tidak menggunakan stream sungguhan. Pemeriksaan kualitas/subtitle pada versi 1.6 mencakup 37 tes adapter dan 24 tes DOM browser; pemeriksaan tiga durasi pada 1.6.1 mencakup 15 tes Android. APK release 1.6.2 sudah dipasang pada TV dan tidak menyertakan instrumentation atau debugging WebView. Perubahan besar struktur situs dapat memerlukan pembaruan adapter.

## Kredit, sumber aplikasi, dan asal proyek

### BitTV yang dipasang pada TV

BitTV adalah aplikasi pihak ketiga. Versi yang dipakai pada setup ini adalah **2.1.7 New Obsidian, varian mod NoAds**, package `com.live_streaming_tv.online_tv` (versionCode 60). Repo ini mencatat pemasangannya; source dan APK BitTV tidak disertakan di repo ini.

| Bagian | Kredit dan sumber |
| --- | --- |
| Aplikasi asli | **DuktekTV / tim BitTV**. Identitas DuktekTV terdapat pada resource APK dan tautan dukungan bawaannya: [Saweria DuktekTV](https://saweria.co/duktektv) dan [Buy Me a Coffee DuktekTV](https://buymeacoffee.com/duktektv). Nama pengembang juga ditulis sebagai Duktek Digital TV oleh [halaman distributor](https://bittvapk.app/home/). |
| Varian NoAds / modder | **Belum terkonfirmasi**. APK yang dipasang mengandung komponen bernama `com.gabutxmod.app.WelcomeDialog` dan `UpdateDialog`, sehingga ada indikasi keterkaitan dengan **GabutXMod**. Nama namespace tersebut belum membuktikan siapa pembuat seluruh mod atau pengunggah aslinya. |
| Sumber file yang dipasang | [APK NoAds dari bittvapk.app](https://bittvapk.app/app/BitTV%20v2.1.7-new-obsidian%20%5BNoAds%5D.apk). Ini catatan asal unduhan, bukan konfirmasi bahwa distributor adalah pembuat atau modder. |
| GitHub pembuat/modder | Belum ditemukan repo GitHub resmi yang dapat diverifikasi per 31 Oktober 2026; tidak mencantumkan akun yang hanya memiliki nama serupa. |

Kredit aplikasi asli tetap milik pembuat BitTV; mod NoAds merupakan varian terpisah dari rilis asli. Pemeriksaan atribusi memakai APK yang benar-benar dipasang, bukan nama modder dari APK BitTV versi lain.

### IDLIX Web TV dalam repo ini

**IDLIX Web TV** adalah aplikasi Android TV custom yang dikembangkan untuk setup **WSevens7** di repo ini. Aplikasi membuka [situs IDLIX yang digunakan](https://z2.idlixku.com/) melalui Android WebView dan menambahkan navigasi remote serta antarmuka pemutar Liquid Glass. Source aplikasinya ada di [`idlix-tv/`](idlix-tv/).

Kode wrapper, kontrol native TV, menu pilihan, daftar episode/rekomendasi, timer auto-hide dan sinkronisasi opasitas dikembangkan dalam proyek ini. Situs, katalog, stream, metadata, subtitle dan pilihan kualitas berasal dari IDLIX atau penyedia pemutar web yang dimuatnya. Ketersediaan pilihan mengikuti sumber web; repo ini tidak menyediakan server film atau salinan katalog/video.

Aplikasi custom ini tidak berafiliasi dengan pengelola IDLIX. Nama dan aset layanan tetap milik pemiliknya masing-masing. Repo GitHub resmi pengelola situs IDLIX belum dapat diverifikasi; repo ekstensi, downloader atau integrasi pihak lain tidak dicantumkan sebagai repo pembuat situs.

### Referensi launcher dan desain

- Launcher berawal dari **Gathin23 — [CleanYourTV](https://github.com/Gathin23/cleanyourtv)**, dengan modifikasi Liquid Glass dan integrasi pemutar pada repo ini.
- Referensi material Liquid Glass: **iyinchao — [liquid-glass-studio](https://github.com/iyinchao/liquid-glass-studio)**. Implementasi TV disesuaikan untuk perangkat Android TV.
- Ikon memakai Google Material Icons Round; lisensi Apache 2.0 dan atribusi tersedia di folder source masing-masing aplikasi. Font Outfit beserta lisensi OFL disertakan di source launcher.

## Perubahan

- Input TV, Wi-Fi, dan Settings dalam satu kapsul Liquid Glass.
- Tombol Settings remote TCL (keycode 4077) membuka Pusat Kontrol ketika berada di Home.
- Popup jaringan dan input, Pusat Kontrol, hero judul aplikasi, dan dock memakai material Liquid Glass native dengan wallpaper blur yang disimpan di cache.
- Ikon Google Material Icons Round dan teks Indonesia/Inggris mengikuti bahasa launcher/sistem.
- Header jam tanpa latar: jam, garis merah, sapaan, lalu hari dan tanggal lengkap. Font Outfit semula dipertahankan.
- **Pusat Kontrol → Tampilan** membuka halaman dengan kategori di kiri: Wallpaper, Liquid Glass, Dock & ikon, Animasi, Sistem.
- Galeri wallpaper dengan thumbnail, mode otomatis, dan interval 1/5/15/30 menit atau 1/3/6/24 jam. Wallpaper dapat ditahan dengan mematikan mode otomatis.
- Satu slider **Opasitas Liquid Glass 0–100%** mengatur hero, dock, kapsul ikon, dan popup sekaligus. Tombol kiri/kanan mengubah nilai 5%.
- Input duplikat di Pusat Kontrol dihapus; Input di kapsul kanan atas tetap tersedia.

![Pengaturan Liquid Glass](shots/liquid-glass/opacity-unified.png)

## File yang disimpan

- [`launcher/`](launcher/): source launcher lengkap dan Gradle wrapper.
- [`releases/3.4.2-liquid-glass/`](releases/3.4.2-liquid-glass/): APK launcher terbaru. Versi 3.4.1 dan APK pemulihan tampilan lama tetap disimpan.
- [`docs/liquid-glass/PANDUAN.md`](docs/liquid-glass/PANDUAN.md): cara pakai, build, dan pemulihan.
- [`docs/liquid-glass/verification.json`](docs/liquid-glass/verification.json): versi, checksum APK, hasil pemeriksaan, dan batas pengujian.
- [`shots/liquid-glass/`](shots/liquid-glass/): screenshot dari TV; beberapa screenshot merekam tahap verifikasi sebelumnya.
- [`README-w7-setup.md`](README-w7-setup.md): arsip setup debloat awal. Informasi IP, dock, dan perintah di arsip menggambarkan keadaan awal, bukan konfigurasi terbaru.

## Build

Butuh JDK 17+, Android SDK 34, serta akses untuk mengunduh dependensi Gradle.

```powershell
Set-Location launcher
.\gradlew.bat --no-daemon testReleaseUnitTest lintRelease assembleRelease
```

APK hasil build berada di `launcher/app/build/outputs/apk/release/app-release.apk`.
Android SDK lokal dikonfigurasi melalui `ANDROID_HOME` atau `launcher/local.properties` milik masing-masing komputer.
APK disediakan dengan signature yang sama dengan launcher terpasang pada TV pengguna. Build di komputer baru menggunakan debug keystore berbeda sehingga tidak dapat langsung menggantikan APK ini tanpa keystore yang cocok. Keystore tidak disimpan di repo.

## Pasang dan pulihkan

Gunakan alamat ADB TV yang sedang berlaku. Contoh PowerShell setelah `adb` tersedia:

```powershell
adb connect <IP-TV>:5555
adb -s <IP-TV>:5555 install -r releases/3.4.2-liquid-glass/Liquid-Glass-TV-3.4.2.apk
adb -s <IP-TV>:5555 shell am start -n com.gathin.tvhome/.MainActivity
```

Gunakan `install -r` untuk mempertahankan data. APK pemulihan `launcher-restore.apk` memiliki versionCode 17; setelah memasangnya, build pembaruan berikutnya harus memakai versionCode lebih tinggi. Tidak perlu menjalankan ulang debloat untuk memasang desain launcher.

Pengaturan pribadi yang disimpan di data privat aplikasi, wallpaper galeri pengguna, password, kunci ADB, serta keystore penandatanganan tidak disertakan dalam pembaruan ini. Source dan APK menyimpan implementasi perubahan; repo bukan salinan penuh data privat TV.

## Verifikasi dan lisensi

263 unit test lulus; Android lint tidak menemukan error. D-pad, slider beserta penyimpanan nilai, galeri, dan rotasi wallpaper diuji pada TV. Nilai 0% diverifikasi menghilangkan material hero, dock, dan kapsul bersama-sama; teks serta ikon tetap terlihat. Tidak ada benchmark FPS atau uji restart.

Material merupakan pendekatan native yang ringan, bukan simulasi optik penuh WebGL. Pengaturan gambar, suara, brightness, dan kredensial Wi-Fi tetap membuka halaman sistem TV. Tombol Settings dari aplikasi lain belum didukung oleh penanganan di Home ini.

Source asal berlisensi MIT ([LICENSE](LICENSE)); ikon Material berlisensi Apache 2.0 dan font Outfit berlisensi SIL OFL, masing-masing disertakan dalam folder launcher. Konsep Liquid Glass dipelajari dari [iyinchao/liquid-glass-studio](https://github.com/iyinchao/liquid-glass-studio).
