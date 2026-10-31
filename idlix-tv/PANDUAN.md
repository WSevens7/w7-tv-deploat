# IDLIX Web TV Player 1.6.2 — Liquid Glass

## Auto-hide episode dan rekomendasi (1.6.2)

Daftar episode dan rekomendasi film juga memakai durasi **Lainnya → Auto-hide pilihan → 5 / 10 / 15 detik** yang sama. Default tetap 10 detik. Tombol untuk berpindah kartu mengulang hitungan; perubahan metadata/thumbnail tidak dihitung sebagai interaksi pengguna. Jika tidak dipakai sampai batas waktu, daftar dan kontrol disembunyikan, lalu mode kembali ke menonton. Bawah dua kali membuka daftar lagi. Memilih episode atau kembali secara manual membatalkan timer lama.

Probe cepat khusus episode (build debug): `adb shell am instrument -w -e episodesOnly true com.w7.idlixwebtv/.SelectionTimeoutProbe`. Menggunakan jam Android dan input remote nyata dengan fixture tiga episode, tanpa stream sungguhan. Pengujian mengembalikan durasi ke nilai tersimpan sebelumnya. Probe tidak masuk APK release.

APK sebelumnya ada di previous-version/IDLIX-Web-TV-1.6.1.apk; source sebelumnya tetap di C:\Script Buatan GPT\2026-10-31_IDLIX-Web-TV-Auto-Hide\source. Untuk pemulihan sebagai update, gunakan source lama dengan signature sama dan versionCode lebih tinggi dari 10. Semua source hasil disalin, bukan dipindahkan.

## Auto-hide selection box (1.6.1)

Selection box otomatis hilang setelah tidak ada interaksi selama **10 detik** secara default. Ubah dari **Lainnya → Auto-hide pilihan → 5 / 10 / 15 detik**. Pilihan disimpan pada aplikasi dan tetap dipakai setelah dibuka ulang.

Setiap tombol pada selection box mengulang hitungan. Saat waktunya habis, kontrol disembunyikan dan mode kembali ke menonton: kiri/kanan kembali menjadi seek, OK kembali menjadi putar/jeda; Bawah membuka pilihan lagi. Timer berlaku saat video berjalan maupun dijeda. Submenu kualitas/subtitle/Lainnya tidak ditutup oleh timer ini; daftar episode dan rekomendasi mengikuti timer yang sama. Saat aplikasi di latar belakang timer dihentikan, lalu dihitung ulang ketika kembali.

Tes Android dengan jam nyata tersedia hanya dalam build debug: `adb shell am instrument -w com.w7.idlixwebtv/.SelectionTimeoutProbe`. Fixture memakai keadaan media native terpisah, bukan stream sungguhan, dan menguji ketiga durasi lewat remote, penyimpanan preferensi, interaksi yang mengulang timer, keadaan jeda, serta submenu yang tetap terbuka. Probe tidak masuk APK release. Pengujian mengembalikan pengaturan ke 10 detik.

Cadangan versi sebelum fitur ini: previous-version/IDLIX-Web-TV-1.6.apk. Source 1.6 tetap di C:\Script Buatan GPT\2026-10-31_IDLIX-Web-TV-Web-Controls\source. Pemulihan tanpa uninstall memerlukan signature sama dan versionCode lebih tinggi dari 9. Source hasil disalin, bukan dipindahkan.

## Kontrol mengikuti web (1.6)

Daftar Kualitas kini mencerminkan pilihan di menu Resolusi web dan menekan tombol kualitas asli ketika dipilih. Tidak ada daftar resolusi tetap: bila sumber menawarkan 360/480/720/1080p, semuanya ditampilkan; bila hanya sebagian, hanya pilihan itu yang muncul. Opsi yang tersembunyi/tidak aktif tidak ditawarkan. Pilihan baru, label dan tanda terpilih ikut diperbarui saat menu terbuka. Jika menu web belum terbentuk, API track Shaka menjadi fallback, tanpa mengarang resolusi.

Subtitle membuka Bahasa subtitle, Ukuran subtitle, Posisi subtitle, dan Gaya subtitle sesuai fitur yang tersedia di web. Gaya mengikuti kontrol sumber untuk warna latar, opasitas latar, blur, jeda waktu dan Atur ulang. Upload/unggah subtitle tidak ditawarkan. Semua opsi/angka/minimum/maksimum/step dibaca dari web. Ukuran dan posisi menggunakan pilihan asli; opasitas dan blur memakai slider horizontal yang diatur kiri/kanan remote. Bahasa dan Nonaktif mengikuti seluruh track yang tersedia.

Pilihan menjalankan kontrol HTML asli (atau API track sebagai fallback), sehingga pengaturan diterapkan pada renderer subtitle web dan tetap sama ketika kembali ke kontrol web. Tidak ada JavaScript bridge Android, unggah file, pengambilan stream URL, atau izin baru. Menjalankan kontrol native tidak melewati iklan/akses situs. Material Liquid Glass tetap mengikuti slider launcher, terpisah dari opasitas latar subtitle.

Struktur menu dan tombol web adalah kontrak sumber yang diperiksa pada versi ini; perubahan markup besar dari situs dapat memerlukan penyesuaian adapter. Daftar yang berubah dalam struktur yang didukung mengikuti web otomatis. Source ini disalin, bukan dipindahkan. Source/APK 1.5.1 tetap di C:\Script Buatan GPT\2026-10-31_IDLIX-Web-TV-Quality-Fix. Pemulihan tanpa uninstall: build source lama dengan signature sama dan versionCode lebih tinggi dari 8.

Tes browser tambahan: jalankan tests/web-menu-browser-fixture.js melalui inspect-player.mjs pada build debug. Fixture memakai DOM tersembunyi terpisah; memeriksa daftar kualitas lengkap/dinamis, pilihan dengan label sama, bahasa/ukuran/posisi/gaya, event input/change, validasi rentang dan pengecualian unggah tanpa mengubah preferensi pemutar asli. APK release tidak menyertakan endpoint debugging atau probe pengujian.

## Daftar kualitas dinamis (1.5.1)

Popover Kualitas dan Subtitle kini memperbarui daftar serta tanda terpilih ketika metadata sumber berubah, termasuk saat menu sedang terbuka. Fokus tetap mengikuti ID track yang sama selama masih tersedia. Daftar dirender ulang hanya jika opsi atau statusnya berubah, bukan setiap frame.

Resolusi nyata dari dimensi video didahulukan daripada label umum yang bisa dipakai ulang pada beberapa track. Varian yang dimensinya belum diketahui tidak disatukan hanya karena bitrate atau teks labelnya sama; duplikasi bahasa pada video yang sama tetap digabungkan. Tidak ada resolusi buatan: ketika sumber belum memberikan dimensi/label resolusi, bitrate asli ditampilkan sementara dan diperbarui setelah metadata muncul.

Empat tes regresi tambahan mencakup label 720p yang dipakai ulang pada track 360p, video berbeda dengan bitrate sama, duplikasi bahasa pada video yang belum diprobe, dan metadata yang muncul belakangan. Total 37 pemeriksaan adapter lulus. Cadangan sebelum perbaikan ini: previous-version/IDLIX-Web-TV-1.5.apk; source tetap di C:\Script Buatan GPT\2026-10-31_IDLIX-Web-TV-Compact-Player\source. Pemulihan sebagai update memerlukan signature sama dan versionCode lebih tinggi dari 7.

## Player ringkas 1.5

Panel Liquid Glass besar di belakang timeline dihilangkan. Waktu dan seek bar memakai latar transparan dengan gradasi gelap halus untuk keterbacaan; judul dan tombol putar/seek juga tanpa material kaca. Satu kapsul kecil di kanan bawah berisi tepat tiga pilihan: **Kualitas · Subtitle · Lainnya**. Sorotan Liquid Glass bergeser mengikuti fokus kiri/kanan selama 180 ms; tidak ada animasi terus-menerus saat diam. Seluruh material tetap mengikuti slider opasitas launcher.

Tekan Bawah untuk memfokuskan kapsul, kiri/kanan untuk memilih, OK untuk membuka popover kecil di atasnya. Lainnya berisi Kecepatan, Gunakan kontrol web dan Tutup pemutar. Back dari Kecepatan kembali ke Lainnya; Back dari popover lain kembali ke kapsul. Daftar panjang dibatasi tinggi dan dapat digulir. Fokus tetap berada di daftar sampai kembali dengan Back.

Dalam mode pilihan, kiri/kanan dan OK ditangani langsung oleh player: tidak diteruskan menjadi seek atau putar/jeda ke WebView. Tombol media play/pause, rewind/fast-forward, next/previous dan spasi juga tidak mengubah video selama berada pada pilihan atau submenu. Kembali ke mode menonton dengan Atas/Back untuk memakai kontrol seek dan putar/jeda lagi.

Bawah lagi dari kapsul membuka episode serial atau maksimal delapan rekomendasi film, sama seperti sebelumnya. Atas/Back kembali satu tingkat. Saat menonton, OK tetap putar/jeda dan kiri/kanan tetap seek 10 detik. Kontrol hilang otomatis saat video berjalan.

APK sebelum perubahan (1.4/code 5) disimpan di previous-version, dengan source asli tetap di C:\Script Buatan GPT\2026-10-31_IDLIX-Launcher-Opacity-Sync\source\idlix. Untuk pemulihan tanpa uninstall, build source lama memakai signature yang sama dan versionCode lebih tinggi dari 6, lalu install -r. Source hasil ini disalin, bukan dipindahkan; keystore dan local.properties tidak disertakan.

## Opasitas mengikuti launcher (1.4)

Atur slider **Opasitas Liquid Glass** pada launcher → Tampilan → Liquid Glass. Player IDLIX otomatis membaca angka yang sama (0–100%) saat dibuka atau dilanjutkan; perubahan saat player aktif diterima melalui notifikasi setelan tanpa polling tambahan. Hanya material panel, blur, tint dan kilau yang mengikuti angka tersebut. Teks, ikon, gambar kartu dan garis fokus remote tetap terlihat, termasuk pada 0%.

Launcher pendamping minimal versi 3.4.2-liquid-glass (versionCode 17), package com.gathin.tvhome, dengan signature yang sama. Setelan dibaca melalui provider lokal read-only content://com.gathin.tvhome.appearance/opacity yang dilindungi izin signature. Tidak ada akses ke pengaturan lain, file privat, URL video atau JavaScript bridge. Bila launcher tidak tersedia, IDLIX mempertahankan nilai terakhir yang berhasil dibaca; instalasi baru memakai 100%. APK release tidak menyertakan probe pengujian.

Tes integrasi perangkat tersedia hanya pada build debug: `adb shell am instrument -w com.w7.idlixwebtv/.AppearanceProbe`. Tes memeriksa pembacaan lintas aplikasi, rentang nilai, penolakan path lain serta insert/update/delete. Jangan menyerahkan APK debug sebagai hasil final.

## Tambahan versi 1.3

Tekan Bawah sekali untuk memilih Kualitas, Subtitle atau Lainnya; tekan Bawah lagi untuk konten terkait. Serial tetap menampilkan episode, dengan fokus awal pada episode yang sedang diputar. Film menampilkan maksimal **8 rekomendasi** dari genre pertama yang tercantum pada halaman film. Judul saat ini, serial dan duplikasi disaring. Data berasal dari katalog publik situs, maksimal dua halaman katalog; tidak ada judul atau genre buatan jika sumber gagal. Pilih kartu lalu OK untuk membuka halaman film dan menjalankan tombol Play normal, termasuk iklan sumber.

Panel, bar kontrol dan kartu fokus memakai Liquid Glass ringan untuk Android 11: cuplikan video 192×108 dalam memori, blur yang dihitung di worker, tint transparan dan kilau tepi berarah. Ini pendekatan native yang dioptimalkan, bukan shader refraksi fisik penuh dari demo web. Cuplikan paling sering diperbarui sekali per 1,5 detik ketika kontrol terlihat; tidak diperbarui ketika waktu video diam dan berhenti saat aplikasi di latar belakang. Cuplikan tidak disimpan atau diunggah. Video terlindungi atau batas lintas origin memakai fallback tint dan tepi tanpa mengambil cuplikan.

Pemeriksaan tambahan mencakup batas 8 film, genre sumber, penyaringan serial/duplikasi, navigasi film normal, fallback sumber gagal, serta pembatasan cuplikan video. Di TV, rekomendasi Outcome → You, Me, Us berhasil membuka dan memutar judul berikutnya. Angka memori dan frame pada folder pemeriksaan merupakan diagnostik singkat perangkat, bukan jaminan FPS atau perbandingan benchmark terkontrol.

Aplikasi TV pribadi yang membuka https://z2.idlixku.com/ dengan kontrol video native bergaya Netflix. Bukan aplikasi resmi IDLIX. Package com.w7.idlixwebtv, versionCode 10, minimum Android 8.0. Memperbarui aplikasi Web TV 1.0 tanpa mengganti launcher.

## Remote saat menjelajah

Panah menggerakkan kursor; OK mengeklik. Gerakkan ke tepi atas/bawah untuk scroll. Menu atau Settings TCL membuka kontrol browser. Keyboard TV dipakai ketika kolom input dipilih.

## Remote saat player utama aktif

- OK: putar/jeda.
- Kiri/kanan: mundur/maju 10 detik.
- Bawah pertama atau Menu/Settings: fokus ke kapsul Kualitas, Subtitle, Lainnya. Kiri/kanan memilih dan OK membuka pilihannya.
- Bawah lagi dari baris pilihan: tampilkan episode pada musim yang sedang diputar. Fokus dan posisi scroll otomatis mengikuti episode aktif. Kiri/kanan memilih, OK memutar. Episode aktif tidak diulang jika dipilih lagi.
- Atas: dari deretan episode kembali ke baris pilihan, lalu ke kontrol video. Saat menonton, tampilkan kontrol.
- Back: kembali dari submenu atau menutup pengaturan. Saat menonton, Back pertama menyembunyikan kontrol; Back berikutnya menutup player dan kembali ke halaman web.

Kontrol hilang otomatis setelah beberapa detik saat video berjalan. Saat jeda tetap terlihat sampai disembunyikan dengan Back. Seek bar dan tombol layar juga mendukung klik/touch.

Di submenu, gunakan atas/bawah dan OK. Memilih kualitas atau subtitle kembali ke kapsul; memilih kecepatan kembali ke Lainnya. Pada Lainnya, pilihan Gunakan kontrol web mengembalikan player asli beserta kursor. Untuk kembali ke kontrol custom: Menu browser lalu Player TV.

## Subtitle dan kualitas dinamis

Pilihan berasal dari API Shaka Player situs saat video dibuka. Semua track subtitle sumber ditampilkan, ditambah Nonaktif. Semua resolusi sumber ditampilkan tanpa duplikasi dari bahasa audio yang berbeda, ditambah Otomatis. Tidak ada batas satu bahasa atau satu resolusi. Bila sumber hanya menawarkan satu kualitas, hanya kualitas itu dan Otomatis yang muncul.

Angka kualitas memakai resolusi nominal sumber; video layar lebar dapat terpotong tingginya, misalnya 1280x536 pada sumber 720p. Kecepatan: 0,5x, 0,75x, 1x, 1,25x, 1,5x dan 2x. Subtitle tetap dirender situs. Video tetap berada di WebView; aplikasi tidak mengekspor URL stream atau token.

## Iklan dan kompatibilitas

Iklan awal tetap mengikuti situs. Kontrol custom aktif setelah player utama Shaka siap dan tidak dipakai untuk melewati timer iklan. Popup baru diblokir. Bila situs mengganti API atau player, tersedia kontrol web sebagai cadangan. Pemeriksaan Cloudflare, login, DRM dan batas akses tetap dikelola situs.

Izin Internet dan izin signature untuk membaca opasitas launcher sendiri. Tidak ada JavaScript bridge ke fungsi Android, akses file lokal, kamera, mikrofon atau penyimpanan. Sertifikat tidak valid ditolak. Debugging WebView hanya aktif pada build debug; APK release yang diserahkan tidak mengaktifkannya. Pemutaran dari tombol remote native didukung tanpa harus memberikan sentuhan terpisah ke WebView.

## Pemeriksaan

Build dan lint Android serta 37 pemeriksaan adapter lulus. Tes adapter menggunakan fixture beberapa bahasa/resolusi, pemilihan track, subtitle Nonaktif, kualitas Auto/manual, batas seek dan alur iklan. Jalankan dari folder source:

    node tests/player-adapter.test.mjs

Uji awal versi 1.1 memakai judul Outcome (2026): perpindahan dari iklan, video berjalan fullscreen, play/pause, seek +10 detik, subtitle Nonaktif/Indonesian, kualitas Auto/720p, kecepatan 1,25x lalu 1x, serta kembali ke kontrol web. Seluruh judul/server belum diuji. Hasil akhir dan checksum ada di verification.json.

## Build dan pasang

Butuh JDK 17+ dan Android SDK 34. Atur ANDROID_HOME atau local.properties sendiri.

    .\gradlew.bat --no-daemon lintRelease assembleRelease
    adb connect <IP-TV>:5555
    adb -s <IP-TV>:5555 install -r IDLIX-Web-TV-Liquid-Glass.apk
    adb -s <IP-TV>:5555 shell am start -n com.w7.idlixwebtv/.MainActivity

Hasil build: app/build/outputs/apk/release/app-release.apk. Keystore komputer pembuat tidak disertakan. Build di komputer lain membutuhkan signature yang cocok untuk menggantikan aplikasi tanpa uninstall. Versi 1.0 dan source aslinya tetap berada di folder hasil lama. Untuk rollback sebagai update, bangun source lama dengan versionCode lebih tinggi daripada versi yang sedang terpasang.

Source disalin, tidak dipindahkan. Ikon Google Material Icons Round memakai Apache License 2.0; lisensi dan atribusi disertakan. Pengujian dan inspeksi hanya dilakukan pada WebView aplikasi ini; APK final tidak menyediakan endpoint debugging.


## Deretan episode 1.2

Daftar diambil dari kartu/tautan episode yang benar-benar tersedia pada situs, diurutkan berdasarkan nomor dan tanpa duplikasi tautan Sebelumnya/Berikutnya. Hanya musim dan serial saat ini yang ditampilkan. Jumlah episode rencana dalam metadata tidak dijadikan episode palsu. Film menampilkan deretan rekomendasi tersendiri.

Pemilihan episode lain membuka tautan normal situs dan menekan tombol Play normal setelah halaman siap, termasuk alur iklan. Pemilihan episode yang sedang diputar hanya menutup deretan. Thumbnail memakai gambar episode dari sumber; bila gambar tidak tersedia, kartu dan nomor episode tetap dapat dipilih.

Saat di deretan episode, atas atau Back kembali ke baris pilihan. Saat di baris pilihan, atas atau Back kembali ke video. Semua pilihan track tetap dinamis seperti versi sebelumnya.

Uji tambahan 1.2 pada Doctor X: Mafia in White episode 5 dan 6: fokus awal tepat pada episode 5, memilih episode aktif tidak mereset waktu, pindah ke episode 6 memulai alur putar normal dan berhasil berjalan, kembali ke baris pengaturan dan video bekerja. Daftar sumber yang dimuat ulang bertambah dari 6 ke 7 episode dan diikuti otomatis. Label audio pada metadata varian tidak dipakai sebagai nama resolusi.
