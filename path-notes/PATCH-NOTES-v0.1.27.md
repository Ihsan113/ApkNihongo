# v0.1.27 — Belajar: materi tidak bisa dibuka + web roadmap UI

Base: `NihonGo-Native-Android-v0.1.26-Learn-BuildFix.zip`. Tidak mengubah sistem login, server, kredit/QRIS, Home, Sensei, Misi, atau Pengaturan.

## Bugfix paket materi
- Dari laporan perangkat, sesudah klik **Mulai Materi** muncul toast path `study/n5-ef71eb7c8169.json.gz`, konsisten dengan kegagalan `AssetManager.open`. Aset ini sebenarnya ADA dalam ZIP v0.1.26; penyebab final pada APK tidak dapat dibuktikan tanpa menginspeksi APK fisik. Untuk memperkeras packaging, seluruh 296 chapter GZIP diganti ke ekstensi asset-neutral `.dat` **tanpa mengubah satu pun byte kontennya**.
- `study/index.json` diperbarui untuk 1.296 ID materi; loader tetap menggunakan GZIPInputStream, sekarang cache index satu kali dan chapter aktif saja.
- Preflight saat membuka Belajar memeriksa bahwa bab `self-intro` tersedia dalam APK. Jika gagal, pesan menjelaskan paket APK bermasalah alih-alih hanya menampilkan path.
- `scripts/check_lesson_assets.py` menvalidasi 1.296 ID dan 296 file chapter dari source atau compiled APK. GitHub Actions **gagal lebih awal** bila ada chapter yang tidak terkemas / korup. Jalankan `python3 scripts/check_lesson_assets.py app/src/main/assets` untuk tes source.
- Asset `.dat` dicantumkan sebagai `androidResources.noCompress` agar tidak dikompresi dua kali di APK, sehingga bacaan GZIP lebih ringan.

## UI/UX Roadmap mengikuti kode web
- Mengikuti komponen `LearnScreen.jsx` (`WindingPath`, `MapNode`, `NodeSheet`, `SectionDivider`): bab dan materi sekarang node **lingkaran** berkelok, bukan kartu lebar berselang-seling.
- Node bab berdiameter ~60dp, node materi 50dp. Judul singkat dua baris di bawah, bintang untuk yang selesai, lingkaran terisi untuk yang lulus; garis putus-putus lembut sebagai jalur. Item tetap menggunakan LazyColumn sehingga scroll tidak membuat seluruh roadmap sekaligus.
- N5 Dasar dan N5 Full Prep memakai pemisah section; panel detail materi menampilkan badge, deskripsi, status bintang/score, dan tombol Mulai/Ulangi sebelum navigasi.
- Tersedia ringkasan energi belajar dan checkpoint bab. Tidak ada mode fullscreen yang dipaksakan pada native.
- Cek harga/akses energi tetap lewat GET backend, pemotongan hanya lewat POST setelah konfirmasi. Tidak ada angka saldo/sukses transaksi lokal baru.

## Tes yang perlu dilakukan
1. Push isi ZIP lengkap (terutama folder `app/src/main/assets/study/`); **hapus asset `.json.gz` lama** dari repo agar versi lama tidak ikut tertinggal.
2. Pastikan langkah **Verifikasi paket 1.296 materi dalam APK** pada GitHub Actions PASS.
3. Install APK terbaru, buka `Belajar → N5 → Bab 5 → Perkenalan Diri Lengkap → Mulai Materi`. Harus membuka pembuka, bukan toast path.
4. Buka materi N1 acak. Pastikan pembayaran/energi diminta hanya jika backend meminta; jangan bayar sungguhan untuk tes.
5. Cek panel bab, filter Semua/Belum selesai/Selesai, back, bintang, scroll pulang-pergi.

## Batasan
Belum menjalankan Android Gradle build di lingkungan ini (Android SDK tidak tersedia). Hasil visual di berbagai ukuran layar dan semua jenis soal masih perlu pengujian APK fisik. Implementasi native mendekati UI inti web, belum pixel-perfect untuk semua ukuran.
