# NihonGo Native Android — v0.1.22

Proyek Android **full native** untuk menyalin tampilan dan alur NihonGo versi web. Situs dipakai sebagai referensi desain dan kontrak API; layar Android dibangun dengan Jetpack Compose dan tidak menampilkan situs melalui WebView.

## Beli Kredit, Riwayat & Detail QRIS — v0.1.22

Base **v0.1.21**. UI `CreditTopupScreen.jsx`, `CreditHistoryScreen.jsx`, dan `CreditPaymentScreen.jsx` ditiru menggunakan Jetpack Compose native dalam `NativeCreditTopupScreen.kt`, `NativeCreditHistoryScreen.kt` dan `NativeCreditPaymentScreen.kt`. Paket, saldo, XP, pembayaran, QRIS, dan transaksi dibaca dari server. Konfirmasi checkout, unduh QRIS memakai pemilih file, filter tanggal dan jenis riwayat, serta navigasi kembali berjenjang didukung. Tidak ada WebView. Lihat `path-notes/PATCH-NOTES-v0.1.22.md` untuk verifikasi dan batasan.

**Penting:** ZIP sumber belum menjalani build Android lengkap; verifikasi di GitHub Actions dan uji alur pembayaran di lingkungan aman sebelum transaksi sungguhan.

## Beli Kredit, QRIS, dan Riwayat v0.1.22 (current)

Base adalah **v0.1.21**. Reward Center → Beli Kredit sekarang memuat paket dan saldo dari API, konfirmasi estimasi biaya provider, pembelian Premium Materi, lookup Order ID, dan lima pembelian terakhir. Layar Detail Pembayaran menampilkan QRIS dari endpoint server/QR string asli, hitung mundur, refresh status otomatis hanya saat foreground dan order masih pending, simpan gambar QR, salin ID, serta pembatalan dengan konfirmasi. Reward Center → Riwayat menampilkan transaksi XP/kredit, tiga filter jenis, lima filter tanggal termasuk tanggal custom, pagination 20 item, dan order top-up. Navigasi antarlayar memakai Compose native, bukan WebView. Responsif pada handset kecil sesuai breakpoint web.

**Transaksi tetap otoritatif dari backend**; estimasi di konfirmasi bukan jumlah final, dan order sukses tidak pernah diklaim lokal. Build APK dan pembayaran nyata belum diuji. Detail serta langkah QA: [`path-notes/PATCH-NOTES-v0.1.22.md`](path-notes/PATCH-NOTES-v0.1.22.md).

## GlobalLoading native mengikuti web (v0.1.21)

Base **v0.1.20**. Layar loading global kini mengikuti `client/src/components/GlobalLoading.jsx`: logo resmi NihonGo, orbit cincin warna merah/emas, animasi melayang, sakura, glow, progress shimmer, status Jepang dan teks ID/EN, empat badge fitur serta tip. Dijalankan full native dengan **Jetpack Compose** tanpa WebView. Varian **account** muncul saat pemeriksaan sesi startup; varian **page compact** muncul saat Home pertama kali memuat progres dan statistik, serta fetch awal Misi/Beli Kredit/Riwayat. Tidak ada jeda buatan 3 detik pada native; loading ditutup ketika data sudah selesai, termasuk saat error supaya pemulihan/retry tetap tersedia. Home, Sensei, TTS, misi dan backend tetap dipertahankan. Lihat [`path-notes/PATCH-NOTES-v0.1.21.md`](path-notes/PATCH-NOTES-v0.1.21.md) untuk detail dan checklist. **APK belum diuji di perangkat**.

## Halaman Misi native mengikuti web (v0.1.20)

Base **v0.1.19**. Menu **Home → Reward Center → Misi** sekarang membuka `NativeMissionsScreen.kt` yang menyalin komposisi, ilustrasi dan logika klaim dari `MissionsScreen.jsx`. Progress dan hadiah sepenuhnya disinkronkan dengan backend asli, bukan angka simulasi. Refresh, loading/error, aturan opsional Review dan bonus, statistik harian, batas kredit dan reset direpresentasikan. Misi Belajar membuka daftar Learn native; permainan Quiz dan Review belum tersedia sebagai aktivitas native penuh (tombol menyampaikan batasan itu tanpa mengubah progres). Perubahan terbatas pada layar Misi, integrasi route, 3 aset, dan versi, serta dependency Lifecycle Compose untuk refresh saat resume. Lihat [`path-notes/PATCH-NOTES-v0.1.20.md`](path-notes/PATCH-NOTES-v0.1.20.md) untuk detail dan checklist. APK masih harus dibuild lewat GitHub Actions dan diuji di perangkat.

## Gestur tangan Home mengikuti source web v0.1.19 (current)

Base **v0.1.18**. Hanya gerak lengan/tangan Sensei yang diperbaiki dari referensi `client/src/components/SenseiAvatar.jsx`: mengganti lambaian tangan kanan besar dengan 4 gestur bicara pendek, transisi halus, micro-motion pergelangan, dan `presentLesson` sesekali saat idle. GLB sudah memakai pose dasar tangan yang sama dengan web, maka native hanya menambahkan delta rotasi yang dikonversi dari radian Three.js ke derajat SceneView. **Tidak mengubah kamera, UI Home, TTS, animasi muka/kepala, scroll, Pengaturan, atau admin.**

Sumber rinci dan validasi di [`path-notes/PATCH-NOTES-v0.1.19.md`](path-notes/PATCH-NOTES-v0.1.19.md). Animasi Kotlin murni telah diuji; compile APK dan kesamaan gerak di perangkat masih perlu dicek.

## Animasi Sensei lebih hidup + framing web v0.1.18 (historis)

Base **v0.1.17**. Komposisi avatar di hero diperbesar menjadi 76% lebar layar dengan kamera lebih dekat dan framing kepala lebih tinggi, agar proporsi wajah dan torso mendekati screenshot Chrome rujukan. Karakter tetap berada di bawah teks sehingga sapaan terbaca, kaki sengaja dapat terpotong di area Continue Learning.

GLB sumber tidak mengandung animasi bawaan, tetapi tulang VRM (kepala, leher, dada, bahu, lengan atas/bawah, telapak) tersedia. Animasi **procedural native** sekarang mencakup napas, gerak kepala, sapaan tangan saat pertama muncul, gestur saat berbicara, sesekali gerak tangan saat idle, dan blink ketika diam maupun berbicara. Rotasi tulang selalu diterapkan relatif pada pose dasar sehingga tidak bergeser dari waktu ke waktu. Morph A/I/U/E/O TTS dari v0.1.17 tetap digunakan. Filament diparkir selama Home scroll/offscreen, tetapi hero yang tampak diam menggerakkan model agar tidak terlihat statis. Loop tulang adaptif: hingga 60Hz saat bicara/sapaan, sekitar 30Hz saat diam. Baca `path-notes/PATCH-NOTES-v0.1.18.md` untuk detail serta keterbatasan. **Visual dan FPS perangkat masih harus diuji setelah build GitHub Actions.**

## Lip Sync VRM + optimasi scroll Home v0.1.17

Base v0.1.16. Model `waifu_native.glb` memang menyediakan VRM A/I/U/E/O sebagai morph targets 32–36 dan blink pada indeks 12 di mesh Face. Renderer Filament sekarang menulis bobot morph mesh Face ketika Android TTS berbicara; `onRangeStart` TTS memetakan kana jika engine mendukung, dan saat tidak tersedia model membuat bukaan mulut ritmis selama ucapan. Ini **lip-sync perkiraan berbasis TTS callback**, bukan sinkronisasi audio fonem-presisi.

Home hanya berisi beberapa komponen, maka LazyColumn diganti dengan Column.verticalScroll supaya GLB/renderer **tidak dilepas/dibuat ulang** ketika pengguna kembali scroll dari Reward/Stats ke Hero. SceneView tetap GPU-accelerated (Filament) dengan `isRendering=false` saat idle, scroll, dan Hero tidak terlihat; rendering hidup selama load awal, TTS berbicara, dan 160ms sesudahnya untuk menutup mulut. Tidak menggunakan WebView, tidak mengubah gambar/tekstur, data backend, login, atau pengaturan admin. Detail [`path-notes/PATCH-NOTES-v0.1.17.md`](path-notes/PATCH-NOTES-v0.1.17.md). Build APK dan pengujian 60fps fisik masih diperlukan.

## Perbaikan Suara Sensei v0.1.16

Base **v0.1.15**. Memperbaiki visibilitas `TextToSpeech` Android 11+ pada manifest; inisialisasi TTS lebih defensif (main-thread callbacks, alternate installed engine once, pemeriksaan Japanese, output MEDIA), dan menambah **tes suara** serta jalan pintas Setelan TTS dari **Pengaturan → Suara & Mic**. Sapaan Jepang Home dan idle phrase hanya aktif saat Home/aktivitas foreground. Belum ada lip-sync atau audio engine bawaan. Lihat [`path-notes/PATCH-NOTES-v0.1.16.md`](path-notes/PATCH-NOTES-v0.1.16.md) untuk diagnosis dan skenario uji. **Belum diverifikasi build atau playback pada Android fisik.**

## Home + Pengaturan Pengguna v0.1.15

Base proyek adalah ZIP **v0.1.14**. Berdasar dua screenshot terbaru, avatar Home diperbesar (kamera native jarak 1.92m), glow/backdrop diselaraskan, dan suara Jepang otomatis (sambutan + idle 10–15 detik) dinyalakan seperti web. Android TTS butuh dukungan bahasa Japanese di perangkat; keterbatasan VRM lip-sync dijelaskan di catatan patch.

Halaman Pengaturan pengguna kini mengikuti hierarki web: Akun, Suara & Mic, Avatar, Tema, Bahasa, Penyimpanan, Feedback, Bantuan/Legal, dan konfirmasi hapus akun. Admin tetap **tidak disentuh**. Klik ke [`path-notes/PATCH-NOTES-v0.1.15.md`](path-notes/PATCH-NOTES-v0.1.15.md) untuk detail batasan dan rencana uji. **Belum dibuild APK di lingkungan ini.**

## Home parity v0.1.14 (current)

Versi ini mencocokkan kembali kode JSX Home web terhadap Android native, bukan sekadar menambal overflow. Hero 506dp dengan background dan overlay senada web, avatar native Filament kanan, welcome/header, overlap kartu belajar, Reward Center, grafik XP, level/kredit, 4 statistik, dan grafik streak/minutes dibuat lebih proporsional. Home diambil dari data server terautentikasi. **Misi** bisa menampilkan & klaim hadiah, **Riwayat** filter dan halaman, **Top Up** menampilkan katalog, **Lanjutkan** menampilkan materi dan status unlock. Checkout QRIS dan player pelajaran native sengaja belum diaktifkan karena perlu layar native lengkap agar tidak ada transaksi/aksi palsu. Settings menyediakan suara TTS Jepang opsional (default mati untuk hemat baterai). Tidak ada WebView.

Logika detail dan ketentuan uji dibahas di [`path-notes/PATCH-NOTES-v0.1.14.md`](path-notes/PATCH-NOTES-v0.1.14.md). **Belum divalidasi build APK karena toolchain Android tidak terpasang di sesi ini.** Jalankan GitHub Actions dan uji di perangkat sebelum menilai kesamaan piksel-per-piksel.

## Perbaikan Home v0.1.12 — fokus 100% native

Patch ini memperbaiki tampilan **Home** berdasarkan dua screenshot Android dan source halaman Home web asli. Tidak mengubah login, akun, API backend, dan layar lain.

- Kamera Sensei kini diarahkan ke pusat geometri model (Y=0.80m), bukan ke titik nol dunia di lantai; jarak kamera 2.90m dan ukuran karakter 1.60m agar **kepala, tangan dan sepatu** masuk framing. Model GLB tetap dirender dengan **SceneView/Filament native**, bukan WebView.
- Avatar diletakkan di sisi kanan dan lebih rendah; judul Jepang serta sambutan diprioritaskan di lapisan atas (`zIndex`) dengan bayangan teks/scrim untuk menjaga keterbacaan.
- Hero tidak lagi memakai **tinggi kaku 680dp**. Tingginya mengikuti lebar layar (480–540dp, sekitar 506dp pada ponsel 395dp), meniru desain web.
- Kartu **Lanjutkan Belajar** tetap overlap sekitar 42dp di ujung hero, tetapi tinggi kartu **diukur nyata** memakai Compose `Layout`; Reward Center tidak lagi terdorong oleh jarak kosong.
- **Misi / Beli Kredit / Riwayat** sekarang satu baris tiga kolom `weight(1f)`, bukan `horizontalScroll` dengan kartu 126dp tetap. Judul dan deskripsi boleh membungkus hingga dua/tiga baris, sehingga tidak terpotong secara vertikal/horizontal.
- Label jumlah materi pada footer kartu diatur dengan `weight` agar tombol **Lanjutkan** tetap tampil pada layar lebih sempit.

**Penting:** ini patch source siap dibuka di Android Studio, bukan APK yang telah divalidasi langsung di perangkat. Lingkungan penyusunan ZIP tidak memiliki Android SDK/Gradle distribusi dan tidak dapat melakukan uji render perangkat. Jalankan build Android Studio/GitHub Actions, lalu uji visual di lebar 320dp, 360dp, 393dp dan font sistem diperbesar 1.3×. Jika tampilan 3D masih berbeda dari web, verifikasi screenshot APK aktual karena renderer native memakai sudut kamera/FOV yang berbeda dari Three.js.

## Perubahan build v0.1.11

- Memperbaiki ulang kegagalan `:app:checkDebugAarMetadata`: SceneView `4.52.0` ternyata juga memerlukan compile SDK 37, sehingga tetap tidak cocok dengan API 36/AGP 8.13.2.
- Menurunkan SceneView ke `4.34.0`, yaitu rilis sebelum lini SceneView menaikkan compile SDK ke 37 pada versi 4.35.0. Ini mempertahankan render GLB dengan Filament secara native tanpa berpindah ke WebView dan tanpa meminta SDK 37 yang tidak ditemukan oleh sdkmanager runner.
- Mempertahankan `compileSdk = 36`, `targetSdk = 36`, Compose BOM enforced `2026.06.01`, AGP `8.13.2`, dan Gradle `8.14.4`.
- Version dinaikkan ke `0.1.11` / `versionCode 12`.

## Perubahan build v0.1.10

- Mengatasi `checkDebugAarMetadata` yang gagal karena SceneView 4.60.0 dan Compose 1.12.1 mewajibkan compile SDK 37 serta AGP 9.1.0.
- Menurunkan SceneView ke 4.52.0, yang mendahului perubahan persyaratan SDK 37 pada lini 4.53+, agar tetap cocok dengan compile SDK 36/AGP 8.13.2.
- Mengunci Compose ke BOM 2026.06.01 memakai `enforcedPlatform` supaya transitive dependency tidak diam-diam menaikkan Compose ke 1.12.1; menyelaraskan `activity-compose` ke 1.12.0.
- Memperbarui Gradle Wrapper ke 8.14.4 untuk menghilangkan peringatan versi Gradle 8.13 yang akan segera tidak didukung plugin Kotlin mendatang.
- Version dinaikkan ke `0.1.10` / `versionCode 11`.

## Perubahan build v0.1.9

- Memperbaiki kegagalan GitHub Actions yang meminta paket SDK `platforms;android-37` tetapi paket itu tidak tersedia dengan nama tersebut di kanal SDK yang dipakai runner.
- Menyamakan `compileSdk` ke API 36 dengan `targetSdk = 36` dan menginstal `platforms;android-36` + `build-tools;36.0.0`. Ini menghindari platform API 37 yang belum dibutuhkan kode saat ini dan menjaga toolchain AGP 8.13.2 tetap konsisten.
- Version dinaikkan ke `0.1.9` / `versionCode 10`.

## Perubahan Home v0.1.8

- Layar Home dibuat ulang dengan Compose mengikuti referensi: hero yang berubah sesuai waktu, teks sambutan, kartu **Lanjutkan Belajar**, Reward Center horizontal, kartu level/kredit, grafik XP 7 hari, empat statistik, kartu streak/estimasi belajar, serta bottom navigation `Rumah · Belajar · Lab · Game · Sensei`.
- Model Sensei disertakan sebagai GLB lokal dan dirender menggunakan **SceneView + Filament** di Android. Ini renderer 3D native, bukan WebGL browser atau WebView. Pose diam model disesuaikan ke pose berdiri dari source web; GLB tidak menyertakan seluruh sistem ekspresi wajah dan animasi idle VRM milik web.
- Metadata lokal 1.296 pelajaran digunakan untuk menentukan materi/chapter lanjut sesuai aturan web (pelajaran dianggap lulus jika `completed` dan minimal 2 bintang; pelajaran berikutnya terbuka setelah sebelumnya lulus).
- Progress dan statistik Home dibaca dari backend melalui `GET /api/progress` serta `GET /api/stats/weekly`. Angka XP, akurasi, saldo kredit, level, streak, dan chart tidak dikunci pada angka contoh.
- Login Google tetap memakai Credential Manager native. Package ID dan signing debug dipertahankan dari proyek sebelumnya agar konfigurasi login yang sudah bekerja tidak berubah tanpa sengaja.

## Dokumentasi perubahan

Catatan patch semua versi ditempatkan rapi di folder [`path-notes/`](path-notes/). Untuk melihat perubahan versi ini, buka [`PATCH-NOTES-v0.1.12.md`](path-notes/PATCH-NOTES-v0.1.12.md). Catatan versi baru berikutnya juga harus disimpan di folder `path-notes/`, bukan di root proyek.

## Endpoint backend

Base URL di `app/src/main/java/id/vixnihon/nativeapp/data/AuthRepository.kt`:

```text
https://www.vixnihon.web.id
```

- `GET /api/auth/config`
- `POST /api/auth/google` — menerima Google ID token
- `GET /api/auth/me` — diverifikasi menggunakan server JWT
- `GET /api/progress` — progres belajar per akun
- `GET /api/stats/weekly?lang=id` — XP, level, kredit, akurasi, streak, dan statistik 7 hari

Backend tetap menjadi sumber kebenaran untuk akun/progres/ekonomi. Tidak ada client secret Google yang ditanam di aplikasi Android.

## Build APK melalui GitHub Actions

1. Ekstrak ZIP dan unggah seluruh isi folder proyek ke repositori GitHub.
2. Buka **Actions → Build NihonGo Native Android → Run workflow**.
3. Setelah workflow selesai, unduh artifact **NihonGo-Native-debug-apk**.

Workflow memasang Android SDK Platform 36 dan Build Tools 36.0.0. Android Gradle Plugin masih 8.13.2/Kotlin 2.4.21; Gradle Wrapper sekarang 8.14.4. SceneView dikunci ke 4.34.0 dan Compose BOM 2026.06.01 enforced untuk menghindari dependensi API 37 yang gagal di toolchain ini.

Untuk build lokal, gunakan JDK 17 dan Android SDK Platform 36, kemudian jalankan:

```bash
./gradlew :app:assembleDebug
```

`gradlew` merupakan bootstrap kecil yang mengunduh distribusi Gradle 8.14.4 pada pemakaian pertama lalu memeriksa SHA-256 distribusi.

## Google OAuth dan signing

- Web OAuth Client ID tetap dibaca dari `GET /api/auth/config` dan dipakai sebagai server client ID untuk alur Credential Manager.
- OAuth Client bertipe Android harus cocok dengan `applicationId` `id.vixnihon.nativeapp` dan SHA-1 sertifikat APK yang dipasang.
- Workflow menampilkan SHA-1 APK. Keystore debug yang disertakan mempunyai SHA-1 `90C46BCA94D902D4D761E4DAB85053267369AB31`; jika GitHub Secrets memakai keystore lain, ubah nilai `EXPECTED_SHA1` di `.github/workflows/android-debug.yml` agar cocok dengan sertifikat yang memang dipakai.
- Jangan commit file `.jks`, password keystore, `.env`, atau Google `client_secret` ke repositori.

## Batasan dan validasi

- Build Android penuh belum dapat dijalankan di lingkungan pembuatan ZIP ini karena distribusi Gradle tidak dapat diunduh dari lingkungan tersebut. Validasi final harus dilakukan melalui GitHub Actions.
- Home native, data home, dan render GLB disiapkan; halaman detail Belajar, Lab, Game, Sensei, Misi, Top Up, dan Riwayat masih perlu diimplementasikan sebagai layar native berikutnya. Tombol yang menuju layar-layar tersebut saat ini menampilkan pemberitahuan yang eksplisit, bukan membuka halaman web.
- Model Sensei yang disertakan dirender native dalam pose diam. Ekspresi wajah, TTS, blink, dan animasi gesture procedural web belum ditransfer ke renderer native.

## Versi

- **0.1.14 / versionCode 14** — menyamakan ulang Home dengan source JSX dan membuat tujuan shortcut native berbasis API.
- **0.1.12 / versionCode 13** — frame Sensei, tinggi hero, kartu belajar overlap 42dp dan 3 Reward Center responsif.
- **0.1.11 / versionCode 12** — menurunkan SceneView ke 4.34.0 karena SceneView 4.52.0 masih membutuhkan compile SDK 37.
- **0.1.10 / versionCode 11** — menyelaraskan versi SceneView/Compose ke API 36 dan menaikkan Gradle Wrapper ke 8.14.4.
- **0.1.9 / versionCode 10** — perbaikan SDK CI: compileSdk/platform dikembalikan ke API 36 agar paket SDK tersedia dan cocok dengan toolchain saat ini.
- **0.1.8 / versionCode 9** — Home Compose native, model GLB via Filament, data Home dari API.
- **0.1.7 / versionCode 8** — versi sebelumnya mencoba menampilkan Home via WebView; pendekatan tersebut dibatalkan di v0.1.8 karena target proyek adalah full native.
