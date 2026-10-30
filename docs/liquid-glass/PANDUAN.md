# Liquid Glass TV — TCL 55P735

Varian pribadi dari launcher `Gathin23/cleanyourtv`, package tetap `com.gathin.tvhome`.
Source launcher lama disalin; source asli di folder Temp tetap dipertahankan.

## Hasil

- `Liquid-Glass-TV.apk`: pembaruan launcher 3.4.1-liquid-glass, versionCode 16.
- `launcher/`: source lengkap untuk membangun ulang.
- `screenshots/`: Home, Control Center, Wi-Fi, dan Input TV dari TV asli.
- `backup/launcher-before.apk`: APK persis sebelum modifikasi.
- `backup/files/`: salinan konfigurasi dan wallpaper sebelum modifikasi.
- `backup/launcher-restore.apk`: tampilan launcher lama, dibangun dari source lama dengan versionCode 17 agar bisa dipasang sebagai pembaruan tanpa menghapus data. Belum dipasang.
- `backup/source-before/`: source untuk APK pemulihan; hanya metadata versi diubah dari versi lama.
- `lint-results.html` dan `verification.json`: hasil pemeriksaan.

## Cara pakai

Tiga ikon kanan atas berada dalam satu kapsul: Input TV, Wi-Fi, dan Settings.
Gunakan D-pad untuk pindah, OK untuk memilih, dan Back untuk menutup popup.
Tombol Settings fisik dari Home membuka Control Center; pengguna telah mengonfirmasi fungsi ini.
Clock di Control Center memilih format jam Home 12 atau 24 jam.
Tahan OK pada ikon Settings untuk menu launcher tambahan, termasuk pengaturan urutan aplikasi.

Material memakai wallpaper yang diburamkan sekali dan disimpan di cache, tint transparan,
highlight tepi, dan simulasi refraksi rim yang ringan. Ini pendekatan native Android 11,
bukan seluruh simulasi optik multipass WebGL demo Liquid Glass Studio.

Wi-Fi menampilkan koneksi serta hasil pemindaian yang tersedia. Untuk memilih jaringan,
password, dan sakelar Wi-Fi, Manage networks membuka dialog sistem TV.
Picture, Sound, TV settings, dan Brightness membuka halaman sistem TCL/Android yang sesuai.
Brightness adalah tautan pengaturan gambar TV, bukan slider dengan angka buatan.
Tema launcher tidak mengubah tampilan halaman sistem TCL atau aplikasi streaming.

Tidak ada izin lokasi baru, WRITE_SECURE_SETTINGS, atau layanan aksesibilitas yang diaktifkan.
Tombol Settings saat aplikasi lain sedang terbuka belum didukung/diuji oleh varian ini;
penanganan di Home tidak membutuhkan helper global.

## Build ulang (PowerShell)

Butuh JDK 17+, Android SDK 34, serta debug keystore yang sama untuk update di TV ini.
Jangan membagikan keystore pribadi.

```powershell
$env:JAVA_HOME = 'C:\Program Files\Zulu\zulu-21'
$env:ANDROID_HOME = 'C:\Users\lenov\AppData\Local\Android\Sdk'
Set-Location 'C:\Script Buatan GPT\2026-10-30_Liquid-Glass-TV\launcher'
.\gradlew.bat --no-daemon testReleaseUnitTest lintRelease assembleRelease
```

APK ada di `launcher/app/build/outputs/apk/release/app-release.apk`.
Versi final lolos 263 unit test; lint nol error. Peringatan/informasi lint dicatat dalam laporan.
Uji visual dan navigasi dilakukan pada TV 1920×1080, Android 11. Tidak ada klaim FPS atau
perbandingan performa dengan launcher lama karena benchmark A/B belum dilakukan.

## Pasang ulang / pulihkan

Gunakan IP TV saat ini; contoh berikut memakai `<IP-TV>:5555` sebagai placeholder.

```powershell
$adb = 'C:\Users\lenov\AppData\Local\Android\Sdk\platform-tools\adb.exe'
& $adb connect '<IP-TV>:5555'
& $adb -s '<IP-TV>:5555' install -r 'C:\Script Buatan GPT\2026-10-30_Liquid-Glass-TV\Liquid-Glass-TV.apk'
& $adb -s '<IP-TV>:5555' shell am start -n com.gathin.tvhome/.MainActivity
```

Untuk kembali ke tampilan lama, ganti path APK di perintah install dengan
`backup/launcher-restore.apk`. Tetap gunakan `install -r`; jangan uninstall agar data tersimpan.
APK pemulihan memiliki versi 17. Setelah memakainya, versi pembaruan berikutnya harus lebih
tinggi dari 17 sebelum dibangun; APK liquid versi 16 tidak dapat dipasang di atasnya sebagai
upgrade biasa. APK mentah `launcher-before.apk` tetap disimpan untuk arsip, tetapi firmware
bisa menolak downgrade langsung ke versi 9.

Launcher Google TV bawaan tetap dinonaktifkan sesuai keadaan yang dipilih pengguna.
Jangan menjalankan ulang daftar debloat untuk memasang desain ini.

## Pembaruan ikon dan bahasa

Versi 3.2 mengganti ikon yang digambar lewat Canvas dengan Google Material Icons Round dari Android SDK. Bentuk vektor aslinya dipertahankan; ukuran dan tint disesuaikan. Kapsul dan panel menggunakan set yang sama. Lisensi dan atribusi ada di folder launcher.

Semua teks panel liquid glass tersedia dalam resource Inggris dan Indonesia, mengikuti bahasa launcher/TV. Nama aplikasi, merek, nama jaringan, dan satuan standar tetap ditampilkan sebagaimana mestinya. Snapshot versi 3.1 sebelum perbaikan disimpan di backup/3.1-before-icons; sumber lama disalin, tidak dipindah.

## Header jam versi 3.3

Header jam kini tanpa latar kaca: jam di kiri, garis merah vertikal di tengah, serta sapaan dan hari, tanggal bulan tahun di kanan. Tanggal dan sapaan tetap mengikuti bahasa launcher, format jam 12/24 jam tetap berfungsi. Material kaca pada dock, hero, kapsul ikon, dan popup dipertahankan.

Sapaan dan tanggal memakai warna yang sama (#E6FFFFFF), termasuk setelah pembaruan menit. Format tanggal: EEEE, d MMMM yyyy, misalnya Jumat, 30 Oktober 2026.

Tipografi header memakai font Outfit lama (Light untuk jam, Medium untuk sapaan, Regular untuk tanggal), termasuk ukuran dan jarak huruf semula. Susunan baru dan nama hari pada tanggal dipertahankan.

## Tampilan versi 3.4

Buka Settings pada remote saat di Home → Tampilan. Menu penuh memiliki kategori di kiri
serta halaman terpisah di kanan: Wallpaper, Liquid Glass, Dock & ikon, Animasi, Sistem.
Atas/bawah memilih kategori; kanan atau OK masuk ke halaman. Back mengembalikan fokus
ke kategori; Back berikutnya menutup menu.

Wallpaper memiliki galeri thumbnail, sakelar otomatis, serta interval mati, 1/5/15/30 menit,
1/3/6/24 jam. Foto yang dipilih diterapkan ketika kembali ke Home. Mode otomatis melanjutkan
rotasi koleksi setelah interval, sedangkan mode mati menahan gambar pilihan.

Liquid Glass memiliki satu slider horizontal 0–100% untuk bar hero, dock, kapsul ikon, dan popup sekaligus.
Kiri/kanan mengubah nilai 5%. Preview langsung mengikuti nilai; teks dan ikon tetap terlihat.
Pada 0% material menghilang, dengan penanda fokus tetap tersedia untuk remote.
Pengaturan disimpan. Input duplikat di Pusat Kontrol dihapus; Input kanan atas tetap tersedia.

Verifikasi TV: navigasi D-pad, slider dan penyimpanan nilai, galeri thumbnail, serta rotasi
otomatis interval 1 menit. Setelah uji, interval dikembalikan menjadi 5 menit.
Sumber terbaru disalin ke folder hasil, bukan dipindahkan; cadangan versi sebelumnya tetap ada.

Versi 3.4.1 menyatukan opacity. Pengaturan hero terpisah versi lama tidak dipakai lagi; nilai opacity umum yang sudah tersimpan tetap digunakan. Istilah material memakai Liquid Glass pada kategori, label, dan preview.
