# NihonGo Native Android — v0.1.9

Proyek Android **full native** untuk menyalin tampilan dan alur NihonGo versi web. Situs dipakai sebagai referensi desain dan kontrak API; layar Android dibangun dengan Jetpack Compose dan tidak menampilkan situs melalui WebView.

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

Catatan patch semua versi ditempatkan rapi di folder [`path-notes/`](path-notes/). Untuk melihat perubahan versi ini, buka [`PATCH-NOTES-v0.1.9.md`](path-notes/PATCH-NOTES-v0.1.9.md). Catatan versi baru berikutnya juga harus disimpan di folder `path-notes/`, bukan di root proyek.

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

Workflow memasang Android SDK Platform 36 dan Build Tools 36.0.0. Android Gradle Plugin masih 8.13.2/Kotlin 2.4.21. API 37 tidak diperlukan oleh kode saat ini; jika kebutuhan API 37 muncul kelak, naikkan SDK dan AGP secara terencana, bukan hanya mengganti nama paket di workflow.

Untuk build lokal, gunakan JDK 17 dan Android SDK Platform 36, kemudian jalankan:

```bash
./gradlew :app:assembleDebug
```

`gradlew` merupakan bootstrap kecil yang mengunduh distribusi Gradle 8.13 pada pemakaian pertama lalu memeriksa SHA-256 distribusi.

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

- **0.1.9 / versionCode 10** — perbaikan SDK CI: compileSdk/platform dikembalikan ke API 36 agar paket SDK tersedia dan cocok dengan toolchain saat ini.
- **0.1.8 / versionCode 9** — Home Compose native, model GLB via Filament, data Home dari API.
- **0.1.7 / versionCode 8** — versi sebelumnya mencoba menampilkan Home via WebView; pendekatan tersebut dibatalkan di v0.1.8 karena target proyek adalah full native.
