# NihonGo Native v0.1.25 — Belajar JLPT + Materi Native

Basis: v0.1.24 Loading UI Polish. Fokus: tab Belajar, tidak melakukan push GitHub.

## Yang diubah

- Tab **Belajar** kini memiliki JLPT Roadmap N5–N1, daftar bab, daftar materi (semua/belum/sudah), persentase progres, bintang dan panel detail bergaya native. Bottom navigation tetap tampil di roadmap.
- Koreksi aturan akses terhadap referensi `LearnScreen.jsx`: **tidak ada sequential lock**. Pengguna bebas memilih materi dan level. Akses kredit diperiksa dengan GET `/api/content/access/lesson/:id`, biaya hanya dipotong server via POST `/api/content/access/lesson/:id/start` setelah konfirmasi; tidak ada potongan lokal.
- **1296 lesson** konten asli web, termasuk 296 bab, diekspor dari `client/src/constants/lessons/lazy/*.js` menjadi aset JSON gzip per bab. Hanya bab yang diminta diurai saat dipilih; indeks ID menjaga navigasi N5–N1. Tidak menggunakan WebView atau engine JS.
- Native Lesson Player: pembuka, pola, penjelasan, pembentukan, contoh karakter/frasa, catatan Sensei, kesalahan umum, mini-practice, rangkuman, tes dan hasil. Japanese TTS tombol dengar (memerlukan mesin TTS di perangkat).
- Quiz mendukung **reading_choice, character_choice, multiple_choice, matching, sentence_matching, listening_mini, arrange**. Pilihan diacak, kesalahan dapat diulang; skor lulus minimal 80%, bintang 1–3.
- Hasil disimpan ke `/api/progress/lesson` dan ulang salah dikirim ke `/api/wrong-answers`; `/api/activity/quiz` dan heartbeat `/api/activity/study-heartbeat` untuk statistik/misi. Tidak mengklaim hasil lulus sebelum server mengonfirmasi penyimpanan.
- Home rekomendasi materi berikutnya kini mengikuti aturan akses bebas, bukan mengunci materi berurutan.

## Batasan dan hal yang perlu diuji

- APK belum di-build di lingkungan ini (Android SDK dan dependensi Gradle offline tidak tersedia). GitHub Actions wajib untuk validasi kompilasi.
- Tampilan menggunakan gaya native mirip web; tidak seluruh detail animasi perjalanan JLPT atau panel pembahasan Sensei/hasil interaktif web disalin pixel-perfect.
- Dua kategori `matching` dan `sentence_matching` saat ini disajikan sebagai pemilihan pasangan yang tepat (bukan gesture drag-and-drop visual web). Keduanya tetap memiliki evaluasi benar/salah.
- Tes menggunakan ulang soal yang salah setelah babak utama; score final berdasarkan penguasaan dari soal asli, tanpa hitung ganda.
- Materi ekspor merupakan snapshot dari ZIP web yang dikirim. Kalau materi web berubah, aset native perlu disinkronkan ulang.
- Uji akses saldo cukup/tidak cukup, dev bypass, unlimited, materi sudah dibayar, putus koneksi tepat setelah POST start, beralih tab/keluar materi, suara TTS, level N5 dan N1, dan pemeriksaan progres setelah reopen.

## Aset

`app/src/main/assets/study/index.json` berisi 1296 pemetaan ID dan 296 arsip JSON gzip per bab. Dibaca off-main thread agar scroll roadmap tetap ringan.
