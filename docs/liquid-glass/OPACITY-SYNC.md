# Sinkronisasi Opasitas Liquid Glass

Sudah terpasang: launcher 3.4.2 (code 17) dan IDLIX Web TV 1.4 (code 5).

Atur slider dari Home → Settings → Tampilan → Liquid Glass → Opasitas Liquid Glass. Saat kembali ke IDLIX, panel player mengikuti angka 0–100% yang sama. Teks, ikon dan garis fokus remote tetap terlihat. Nilai awal pengguna 60% telah dikembalikan sesudah pengujian 0% dan 100%.

Pasang launcher APK lebih dahulu, kemudian IDLIX APK, sebagai update tanpa uninstall. Keduanya harus memiliki signature yang sama. Setelan lama tetap dipakai. Tidak perlu root atau izin tambahan dari menu TV. Jika launcher tidak tersedia, IDLIX mempertahankan nilai terakhir; instalasi IDLIX baru memakai 100%.

Provider hanya membagikan satu angka dengan izin signature dan tidak menerima penulisan. Pengujian integrasi dari IDLIX berhasil; akses shell tanpa izin ditolak. Tes adapter player: 33 lulus. Lint: launcher 0 error/135 warning; IDLIX 0 error/13 warning. Screenshot menunjukkan material hilang pada 0% sementara konten/fokus tetap ada. Diagnostik memori/frame disertakan sebagai cuplikan, bukan benchmark FPS terkontrol.

Source disalin, bukan dipindahkan. Tidak menyertakan keystore, local.properties, cache atau hasil build. Probe integrasi ada di source debug dan tidak masuk APK release. Semua folder hasil sebelumnya dipertahankan.

Cadangan APK sebelum update ada di previous-version. Source launcher sebelum update: C:\Script Buatan GPT\2026-10-30_Liquid-Glass-TV\launcher (code 16). Source IDLIX sebelum update: C:\Script Buatan GPT\2026-10-31_IDLIX-Web-TV-Liquid-Glass\source (code 4). Pemulihan tanpa menghapus data: build source lama dengan signature sama dan versionCode lebih tinggi daripada versi terpasang (launcher >17; IDLIX >5), lalu install -r. Jangan uninstall untuk sekadar mengganti tampilan. Downgrade APK release biasa dapat ditolak Android.
