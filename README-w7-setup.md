# w7-tv-deploat: TCL 55P735 Debloat & Launcher Setup

Setup debloat dan penggantian home launcher bawaan untuk TV **TCL 55P735** (Platform: BeyondTV4, Google TV 11 / Android TV 11 SDK 30, build RTT2.211130.001) menggunakan framework [CleanYourTV](https://github.com/Gathin23/cleanyourtv).

---

## Ringkasan Setup

- **Target TV**: TCL 55P735 (IP: `192.168.1.7`, koneksi Ethernet)
- **Launcher Baru**: `com.gathin.tvhome` (Apple TV-style, no ads, 520 KB)
- **Dock Bawah (4 Tile)**:
  1. YouTube (`com.google.android.youtube.tv`)
  2. Netflix (`com.netflix.ninja`)
  3. Vidio (`com.vidio.android.tv`)
  4. Disney+ Hotstar (`in.startv.hotstar.dplus.tv`)
- **Fitur yang Dipertahankan**:
  - Voice Assistant (Google Assistant `com.google.android.katniss`, TCL Assistant, mic remote)
  - Firmware Updater TCL (`com.tcl.versionUpdateApp`)
  - Input & HDMI switcher (`com.tcl.suspension`, `com.tcl.tv`, `com.tcl.tvinput`)
  - Semua app streaming user (Spotify, Apple TV, Vidio, Netflix, Disney+, YouTube)
- **Fitur yang Dimatikan (Disabled, not uninstalled)**:
  - 32 paket bloatware, iklan, analitik, rekomendasi Google/TCL
  - Casting (Chromecast `mediashell`, Miracast, T-Cast)
  - Stock launcher Google TV (`launcherx`) & setup wizard (`setupwraith`)
  - Screensaver iklan Google diganti ke Clock screensaver bawaan launcher

---

## File Penting

- `debloat-custom-55p735.txt`: Daftar 32 paket yang dinonaktifkan
- `launcher/app/src/main/res/values/config.xml`: Konfigurasi dock 4 tile
- `backup/full-backup.txt`: Backup lengkap state paket dan settings sebelum debloat (421 paket)
- `backup/packages.txt`: List 421 nama paket TV sebelum disentuh
- `backup/cleanyourtv-state/`: State snapshot JSON dari tool recovery
- `shots/`: Screenshot layar TV sebelum dan sesudah setup

---

## Perintah Penting (Jalankan dari root repo)

Setup environment (sekali saja jika pindah PC):
```bash
python -m venv tools/venv
tools/venv/Scripts/pip install -r tools/requirements.txt
```

### 1. Cek Kondisi TV (Apakah ada package yang hidup lagi)
```bash
TV_HOST=192.168.1.7 tools/venv/Scripts/python tools/check.py debloat-custom-55p735.txt
```

### 2. Perbaiki Jika Ada Paket / Launcher yang Reset Setelah Update Firmware
```bash
TV_HOST=192.168.1.7 tools/venv/Scripts/python tools/check.py --fix debloat-custom-55p735.txt
```

Atau apply manual:
```bash
TV_HOST=192.168.1.7 bash tools/disable.sh debloat-custom-55p735.txt
TV_HOST=192.168.1.7 tools/venv/Scripts/python tools/tv.py "cmd package set-home-activity --user 0 com.gathin.tvhome/.MainActivity"
```

### 3. Kembalikan TV ke Kondisi Semula (Full Restore / Undo)
```bash
TV_HOST=192.168.1.7 bash tools/restore.sh debloat-custom-55p735.txt com.google.android.apps.tv.launcherx
```

### 4. Build Ulang Launcher (Jika Mau Ubah Dock/Konfigurasi)
```bash
cd launcher
JAVA_HOME="C:/Program Files/Zulu/zulu-21" ANDROID_HOME="$LOCALAPPDATA/Android/Sdk" TV_HOST=192.168.1.7 bash build-and-install.sh
```
