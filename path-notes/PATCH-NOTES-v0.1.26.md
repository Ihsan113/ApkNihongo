# PATCH-NOTES v0.1.26 — Kotlin Compile Fix

Base: `NihonGo-Native-Android-v0.1.25-Learn-Full.zip`.

## Penyebab build gagal

GitHub Actions run `39711392396`, job `119603305104` gagal pada `:app:compileDebugKotlin`. Tiga compiler diagnostics adalah `Only safe (?.) or non-null asserted (!!.) calls are allowed on a nullable receiver of type 'JSONObject?'` pada `NativeLessonPlayer.kt` baris 345 (dua kali) dan 349 (sekali).

Objek soal (`q`) diperoleh dari `questionId?.let { questions[it] }` sehingga bertipe `JSONObject?`. Pemrosesan kesalahan di fungsi lokal `continueQuiz()` memanggilnya tanpa guard. Guard `if (q == null)` yang ada setelah deklarasi fungsi tidak memberikan smart cast ke dalam fungsi lokal tersebut.

## Patch

- Di awal `continueQuiz()`, setelah validasi `questionId`, menambahkan `val currentQuestion = q ?: return`.
- Ketika menyusun entri jawaban salah, `questionText` dan `promptText` sekarang membaca `currentQuestion` (non-null).
- Bump `versionCode` ke 26 / `versionName` ke `0.1.26`; README dan catatan ini ditambahkan.

Seluruh file lain, termasuk 296 paket bab (`assets/study/*.json.gz`), indeks materi, model 3D, dan aset tampilan—tidak berubah. Tidak ada perubahan pada endpoint, scoring, atau mekanisme pengeluaran kredit.

## Validasi dan uji CI

- Verifikasi checksum menunjukkan semua isi file base yang tidak masuk daftar perubahan identik.
- ZIP CRC berhasil; struktur dan nama file proyek dipertahankan.
- Build Android belum diverifikasi. Push **isi** ZIP (bukan ZIP-nya saja) ke root repository lalu jalankan workflow Android seperti biasa. Jika ada kegagalan baru, gunakan log job terbaru sebagai referensi.
