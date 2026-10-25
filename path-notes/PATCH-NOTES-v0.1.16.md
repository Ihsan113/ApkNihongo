# NihonGo Native v0.1.16 — Perbaikan TTS Sensei (Home + Pengaturan)

**Base**: v0.1.15 Home + Settings. Versi ini tidak mengganti API login, halaman lain, maupun panel admin.

## Akar masalah yang diperbaiki

- Manifest v0.1.15 tidak punya `<queries><intent><action android:name="android.intent.action.TTS_SERVICE"/></intent></queries>`. Mulai Android 11 (package visibility), ini dapat membuat `TextToSpeech` gagal menemukan layanan meskipun pengguna memilih Google TTS di Settings. Ini kemungkinan utama berdasarkan screenshot, namun tetap perlu uji APK pada HP.
- Status TTS dulu hanya menampilkan error umum. Sekarang dibedakan antara gagal bind mesin, suara Japanese tidak tersedia, dan gagal memutar audio.
- Callback `OnInitListener` dan `UtteranceProgressListener` diproses di main thread serta dilindungi dari callback terlambat setelah Home ditutup.
- Jika mesin default tidak dapat memulai, coba **satu mesin alternatif terpasang**, jika ada (tanpa loop retry). Pilih suara Jepang offline yang telah terpasang jika didukung; gunakan mesin yang ada sebagai fallback.
- Gunakan audio usage **MEDIA** sehingga volume media perangkat relevan; autoplay tetap berhenti saat Home tidak terlihat/aktivitas pause; instance TTS dilepas (`shutdown`) dengan benar.
- Saat pengguna kembali ke Home, suara sapaan diputar lagi setelah sesi TTS siap. Tidak mengulang sapaan hanya karena pengguna mengubah slider.

## Diagnostik pengguna

Buka **Home → roda gigi → Suara & Mic → Tes suara: こんにちは**.
Hasilnya akan menunjukkan apakah mesin siap, tidak bisa dimulai, suara Japanese belum ada, atau terjadi error pemutaran. Tombol **Buka pengaturan TTS HP** mengarah ke pengaturan voice output jika tersedia, atau ke Setelan sistem sebagai fallback.

Jika Japanese belum tersedia, di Settings perangkat pilih *Speech Recognition & Synthesis from Google* → *Install voice data* → Japanese, periksa **volume media** dan ulangi tes. Pada beberapa custom ROM/AOSP mesin TTS bisa tidak tersedia walaupun opsi terlihat di Settings. Aplikasi tidak bisa menyulap dukungan suara jika mesin/bahasa tidak dipasang.

## Cara uji setelah GitHub Actions

1. Build debug dari ZIP ini. Jika gagal kirim link job/build.
2. Buka Pengaturan → Suara & Mic, tekan **Tes suara**: seharusnya terdengar kalimat Jepang.
3. Keluar ke Home dengan **Sapaan suara otomatis** aktif: satu sapaan lalu ucapan pendek setiap sekitar 10–15 detik saat Home aktif.
4. Tutup layar aplikasi/berpindah aplikasi: tidak boleh ada suara dari Home dan resource TTS dilepas.
5. Putar layar tidak perlu diuji karena aplikasi portrait-only; tes juga di jaringan offline **hanya** jika paket Japanese offline terinstal.
6. Jika masih gagal, catat status yang tampil di panel tes, jenis ROM/Android dan engine yang dipilih agar diagnosis berikutnya spesifik.

## Catatan batasan

- Ketersediaan suara Jepang ditentukan mesin yang terpasang di HP; tidak ada audio suara sintetis yang dibundel dalam ZIP.
- Belum ada lip-sync mulut avatar; perubahan ini hanya alur **suara Home dan Pengaturan**.
- Android SDK tidak tersedia di lingkungan pembuatan ZIP ini; tidak boleh mengklaim build APK atau playback perangkat berhasil sebelum GitHub Actions dan tes HP.
