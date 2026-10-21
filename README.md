# NihonGo Native Android — v0.1.7

Fondasi aplikasi Android native untuk NihonGo, diambil dari source web yang diberikan dan menggunakan backend produksi yang sama.

## Yang sudah disiapkan

- **Login Google native** menggunakan Android Credential Manager; tidak menggunakan WebView.
- Konfigurasi Google dimuat dari `GET /api/auth/config`; ID token dikirim ke endpoint server `POST /api/auth/google` agar tetap divalidasi oleh server.
- Sesi diverifikasi lagi melalui `GET /api/auth/me` saat aplikasi dibuka.
- Token sesi dienkripsi dengan AES-GCM dan kunci Android Keystore sebelum disimpan di Preferences DataStore.
- Onboarding tiga halaman memakai ilustrasi dan copy dari source web, dengan status selesai yang disimpan per akun di perangkat.
- Home pada v0.1.7 menampilkan Home web NihonGo aktual lewat WebView first-party. Ini sengaja dipilih agar karakter Sensei VRM, sapaan/animasi, Reward Center, statistik mingguan, progres materi, dan seluruh navigasi/aksi Home tetap sama dengan versi web daripada menampilkan replika Compose yang belum lengkap.
- Login Google tetap native lewat Credential Manager. Token sesi server yang diterima setelah login disinkronkan ke storage origin NihonGo dengan bootstrap satu kali sebelum layar web ditampilkan; logout di web dikembalikan ke flow login native.
- Aset onboarding/login native tetap dioptimalkan sebagai WebP. Model VRM dan asset Home dimuat dari situs HTTPS resmi, sehingga Home membutuhkan koneksi internet dan dukungan WebGL pada Android System WebView.
- Compose BOM dipin ke `2025.12.00` (Compose 1.10) agar cocok dengan AGP 8.13.2 dan Android SDK 36. Workflow GitHub Actions disediakan untuk membuat debug APK tanpa harus membangun langsung di HP. Workflow memakai `android-actions/setup-android@v4` dan hanya meminta `platform-tools`; paket SDK lama bernama `tools` tidak lagi diminta. Pemeriksaan SHA-1 workflow diselaraskan dengan fingerprint debug keystore yang terlihat pada build terakhir (`90C46BCA94D902D4D761E4DAB85053267369AB31`). Jika Secrets memakai keystore berbeda, ubah `EXPECTED_SHA1` ke fingerprint keystore tersebut.

## Backend

Base URL di `data/AuthRepository.kt`:

```text
https://www.vixnihon.web.id
```

Endpoint yang digunakan:

```text
GET  /api/auth/config
POST /api/auth/google    { "credential": "<Google ID token>" }
GET  /api/auth/me        Authorization: Bearer <server JWT>
GET  /api/progress       Authorization: Bearer <server JWT> (best-effort warm-up)
```

Akun dibuat/ditemukan oleh mekanisme Google Login pada backend yang sudah ada. Tidak ada tabel autentikasi atau sistem password baru yang ditambahkan oleh aplikasi native ini.

## Build APK dari Android Studio

1. Ekstrak ZIP dan buka folder `nihongo-native-android` sebagai project di Android Studio.
2. Pastikan Android SDK Platform **36** dan JDK **17** tersedia.
3. Jalankan task `:app:assembleDebug` (atau dari terminal: `./gradlew :app:assembleDebug`).
4. APK debug dibuat di `app/build/outputs/apk/debug/app-debug.apk`.

`gradlew`/`gradlew.bat` di starter ini merupakan bootstrap kecil: pada pemakaian pertama, skrip mengunduh Gradle 8.13 dan memeriksa SHA-256 distribusi resmi. Sesudah itu Gradle dipakai dari cache lokal.

## Build lewat GitHub Actions (praktis dari HP)

Workflow sudah diperbaiki agar tidak lagi gagal pada `Warning: Failed to find package 'tools'`. Aksi Android SDK terbaru dipakai dengan paket `platform-tools` eksplisit, lalu platform Android 36 dan build-tools 36.0.0 dipasang pada langkah terpisah.

1. Buat repository GitHub dan unggah seluruh isi folder project ini.
2. Buka tab **Actions** → workflow **Build NihonGo Native Android** → **Run workflow**.
3. Setelah selesai, buka artifact `NihonGo-Native-debug-apk` lalu unduh APK.

APK debug dan release sama-sama menggunakan application ID `id.vixnihon.nativeapp`; build debug hanya menambahkan suffix pada version name, bukan pada package ID.

## Pengaturan Google OAuth yang wajib dicek

Server yang sudah ada harus mengembalikan `googleConfigured: true` dan `googleClientId` yang valid dari `/api/auth/config`. Client ID tersebut harus merupakan **OAuth Web client ID** yang sama dengan `GOOGLE_CLIENT_ID` yang digunakan backend untuk memvalidasi ID token.

Di Google Cloud Console, pastikan konfigurasi Android untuk Credential Manager dibuat bagi application ID dan sertifikat SHA-1 yang benar. Debug dan release memiliki application ID/sertifikat berbeda; bila belum didaftarkan, login native bisa gagal meskipun endpoint server sehat. Jangan menaruh client secret atau kunci privat di source Android.

Untuk mendapatkan SHA-1 debug, jalankan:

```bash
./gradlew :app:signingReport
```

## Catatan ruang lingkup dan verifikasi

- Ini adalah **project source Android**, bukan APK yang sudah diklaim berhasil diuji. Lingkungan kerja ini tidak dapat mengunduh distribusi Gradle dari internet, sehingga build Android penuh belum bisa diverifikasi di lingkungan ini.
- Login memerlukan internet, Google Play Services/Credential Manager yang berfungsi, konfigurasi OAuth yang sesuai, dan backend aktif.
- Home menggunakan situs NihonGo yang berjalan dalam WebView terkunci ke domain HTTPS first-party untuk menjaga model dan fungsi tetap konsisten. Pengalaman belajar/reward/tab yang terbuka dari Home menggunakan layar web yang sudah ada; login Google awal tetap native.
- Model 3D bergantung pada dukungan WebGL di Android System WebView dan koneksi internet.
- Tema native untuk layar login/onboarding mengikuti tampilan web: `#07070D`, panel `#0F0F17`, dan aksen merah `#E03535`.


## Patch kompatibilitas 0.1.2

Error `androidx.compose.runtime:runtime-saveable-android:1.12.0 requires Android Gradle plugin 9.1.0 or higher` diperbaiki dengan memindahkan Compose BOM ke `2025.12.00`, yang menggunakan Compose 1.10. Dependency `core-ktx` dan `activity-compose` juga diselaraskan untuk baseline SDK 36. Detail perubahan ada di `PATCH-NOTES-v0.1.3.md`.


## Patch kompatibilitas 0.1.4

Error `Using 'jvmTarget: String' is an error. Please migrate to the compilerOptions DSL` diperbaiki dengan menghapus `android.kotlinOptions { jvmTarget = "17" }` dan memindahkan target JVM ke `kotlin.compilerOptions` bertipe (`JvmTarget.JVM_17`). Ini mengikuti DSL resmi Kotlin Gradle Plugin 2.4.x. Versi aplikasi menjadi `0.1.4` / `versionCode 5`.


## Patch v0.1.7 — Home parity

Home Compose placeholder diganti dengan WebView first-party pada `https://www.vixnihon.web.id/` agar avatar VRM, statistik, Reward Center, dan navigasi aktual tampil sama seperti di web. Sesi dari Credential Manager native disinkronkan setelah origin diverifikasi; halaman ditahan dari pandangan pengguna selama bootstrap. Logout dikirim balik ke sesi native dan perubahan bahasa disinkronkan ke DataStore. Lihat `PATCH-NOTES-v0.1.7.md`.
