# NihonGo Native v0.1.15 — Home + Pengaturan Pengguna

**Base:** ZIP `NihonGo-Native-Android-v0.1.14-HomeParity.zip` (source lengkap). Tidak menyentuh panel admin, konfigurasi OAuth, workflow signing, top up QRIS atau engine pelajaran di luar Home.

## 1. Home — perbandingan screenshot web & native 21 Oktober 2026

- Kamera GLB dipindahkan dari jarak `z=2.90` ke `z=1.92`, tetap fokus pada titik tengah model hasil konversi (`y=0.82`). Tujuh puluh persen penyesuaian komposisi berasal dari zoom, tanpa melebarkan area 3D atau merusak margin teks. Bagian kaki sengaja dapat terpotong saat hero seperti **screenshot Chrome**, bukan dibuat full-body kecil sebagaimana screenshot native v0.1.14. Aset model 3D lokal tidak berubah dan tidak perlu download saat Home dibuka.
- Hero tetap 506dp, background 7 segmen waktu, overlay vertikal dan glow merah sudut kanan atas mengikuti stylesheet web. Header teks 42sp, intro 17sp, section progress overlap 42dp, Reward/weekly tetap mempertahankan integrasi server versi 0.1.14.
- Suara Home default **aktif**, kini mengikuti web: sambutan Jepang setelah ~520ms, kemudian kalimat Jepang acak tiap 10–15 detik, selama Home berada di foreground. BUKAN muted opt-in seperti v0.1.14. Selama Home tersembunyi, engine TextToSpeech berhenti dan resource dilepas (konsumsi baterai). Listener melaporkan status speaking dan jika voice Japanese tak ada, petunjuk instalasi suara muncul. Pengaturan rate, pitch, volume tersimpan permanen secara lokal.
- **Batasan penting**: Ini native Android system TTS dan bisa memiliki kualitas suara berbeda dari server TTS web. Mouth/blendshape lip-sync VRM belum diaktifkan; GLB telah memiliki morph A/I/U/E/O namun perlu animasi Filament yang divalidasi di perangkat. Suara terucap tidak sama dengan janji mulut model telah sinkron.

## 2. Pengaturan pengguna penuh (tanpa admin)

Dialog lama ditingkatkan menjadi **NativeSettingsScreen** dengan hirarki Home > Pengaturan > detail, sesuai kategori `SettingsScreen.jsx`: Akun, Suara & Mic, Avatar, Tema (system/light/dark), Bahasa Sistem, Penyimpanan Lokal, Kirim Saran, Tentang Kami, FAQ, Kontak Bantuan, Kebijakan Privasi, Syarat & Ketentuan, Hapus Akun & Data.

- Pengaturan suara disimpan di SharedPreferences lokal (aspek yang di web juga disimpan di localStorage), no hardcoded sample values. Tema mempengaruhi warna utama Home + Settings; latar Home tetap menyesuaikan jam. Bahasa Indonesia/English tersambung ke DataStore sebelumnya.
- Feedback `POST /api/feedback` meniru frontend; penghapusan akun `POST /api/auth/delete-account` butuh mengetik `CONFIRM` dan modal konfirmasi kedua, tidak diotomatisasi. Logout tetap memakai Credential Manager + sesi native.
- Avatar APK saat ini hanya `waifu_native.glb` (`waifu.vrm` pada referensi web): pilihan avatar lain tidak ditampilkan secara palsu. Profil pengguna dari sesi server asli. Mic Premium hanyalah penjelasan cara kerja web, belum mengaktifkan rekaman atau tagihan pada layar Settings native. Aset sudah offline dalam APK: kelola penyimpanan menghapus hanya cache sementara, tidak menghapus progress.
- Panel admin *tidak disentuh*.

## Uji sebelum release

- **Belum build APK / instrumentation test**: tidak ada Android SDK atau Gradle dependencies di lingkungan ini. Struktur Gradle/ZIP diperiksa, tetapi keberhasilan kompilasi tetap harus divalidasi lewat GitHub Actions. `./gradlew :app:assembleDebug` pada JDK17 + SDK36.
- Screenshot pembanding yang perlu diuji: HP ~393dp portrait pukul 21:49, screenshot `Chrome` & `NihonGo`; cek tepi rambut, kamera zoom, cards overlap, background, dan scroll 60fps. Jika rambut terlalu besar/kepala melewati atas, tuning parameter di `NativeSenseiModel.kt` satu-satunya titik perubahan.
- Test audio: volume media HP aktif, Text-to-speech Engine punya **Japanese** voice. Jika belum, install voice Japanese melalui Pengaturan HP. Voice dari web mungkin memakai backend TTS, sehingga timbre native dapat berbeda. Uji sound on/off, foreground/background dan perubahan bahasa. Pastikan menghindari duplikasi TTS saat settings dibuka.
- Test settings: theme light/dark/system, toggle voice, slider rate/pitch/volume (restart app), language, feedback API, delete confirmation (hanya sandbox account), serta back button.
