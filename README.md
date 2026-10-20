# NihonGo Native Android — v0.1.2

Fondasi aplikasi Android native untuk NihonGo, diambil dari source web yang diberikan dan menggunakan backend produksi yang sama.

## Yang sudah disiapkan

- **Login Google native** menggunakan Android Credential Manager; tidak menggunakan WebView.
- Konfigurasi Google dimuat dari `GET /api/auth/config`; ID token dikirim ke endpoint server `POST /api/auth/google` agar tetap divalidasi oleh server.
- Sesi diverifikasi lagi melalui `GET /api/auth/me` saat aplikasi dibuka.
- Token sesi dienkripsi dengan AES-GCM dan kunci Android Keystore sebelum disimpan di Preferences DataStore.
- Onboarding tiga halaman memakai ilustrasi dan copy dari source web, dengan status selesai yang disimpan per akun di perangkat.
- Home awal memakai palet, background, header Jepang, card “Lanjutkan Belajar”, ringkasan, dan pola navigasi NihonGo. Menu belajar selanjutnya masih berupa placeholder/snackbar karena cakupan tahap ini memang sampai Home.
- Gambar diubah menjadi WebP dengan ukuran terkontrol agar tidak membawa seluruh aset web/3D yang berat ke aplikasi.
- Compose BOM dipin ke `2025.12.00` (Compose 1.10) agar cocok dengan AGP 8.13.2 dan Android SDK 36. Workflow GitHub Actions disediakan untuk membuat debug APK tanpa harus membangun langsung di HP. Workflow memakai `android-actions/setup-android@v4` dan hanya meminta `platform-tools`; paket SDK lama bernama `tools` tidak lagi diminta.

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

APK debug menggunakan application ID `id.vixnihon.nativeapp.debug`. Build release menggunakan `id.vixnihon.nativeapp`.

## Pengaturan Google OAuth yang wajib dicek

Server yang sudah ada harus mengembalikan `googleConfigured: true` dan `googleClientId` yang valid dari `/api/auth/config`. Client ID tersebut harus merupakan **OAuth Web client ID** yang sama dengan `GOOGLE_CLIENT_ID` yang digunakan backend untuk memvalidasi ID token.

Di Google Cloud Console, pastikan konfigurasi Android untuk Credential Manager dibuat bagi application ID dan sertifikat SHA-1 yang benar. Debug dan release memiliki application ID/sertifikat berbeda; bila belum didaftarkan, login native bisa gagal meskipun endpoint server sehat. Jangan menaruh client secret atau kunci privat di source Android.

Untuk mendapatkan SHA-1 debug, jalankan:

```bash
./gradlew :app:signingReport
```

## Catatan ruang lingkup dan verifikasi

- Ini adalah **project source native**, bukan APK yang sudah saya klaim berhasil diuji. Lingkungan kerja ini tidak menyediakan Android SDK/Gradle terpasang dan tidak dapat menjangkau domain deployment melalui DNS, jadi build APK dan login live belum bisa diverifikasi dari sini.
- Login memerlukan internet, Google Play Services/Credential Manager yang berfungsi, konfigurasi OAuth yang sesuai, dan backend aktif.
- Home saat ini adalah fondasi ringan untuk tahap awal. Avatar 3D berbasis VRM dan layar-layar belajar penuh dari web belum dipindahkan ke native pada versi ini.
- Tema awal mengikuti tampilan gelap web: `#07070D`, panel `#0F0F17`, dan aksen merah `#E03535`.


## Patch kompatibilitas 0.1.2

Error `androidx.compose.runtime:runtime-saveable-android:1.12.0 requires Android Gradle plugin 9.1.0 or higher` diperbaiki dengan memindahkan Compose BOM ke `2025.12.00`, yang menggunakan Compose 1.10. Dependency `core-ktx` dan `activity-compose` juga diselaraskan untuk baseline SDK 36. Detail perubahan ada di `PATCH-NOTES-v0.1.2.md`.
