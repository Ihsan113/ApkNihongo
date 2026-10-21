# NihonGo Native Android v0.1.9

## Perbaikan

- Memperbaiki workflow GitHub Actions yang gagal saat `sdkmanager` mencari paket `platforms;android-37`.
- Mengatur `compileSdk = 36` agar cocok dengan `targetSdk = 36` dan AGP 8.13.2 yang dipakai proyek.
- Workflow sekarang memasang `platforms;android-36` dan `build-tools;36.0.0`.
- Version dinaikkan ke `0.1.9` / `versionCode 10`.
- Tidak mengubah package ID, OAuth, login, signing, Home Compose, data API, atau renderer GLB.

## Alasan teknis

Log Actions menunjukkan `Warning: Failed to find package 'platforms;android-37'`. Walaupun Android 17 menggunakan API level 37, proyek saat ini tidak membutuhkan API 37. Mengunci CI pada API 36 menghindari kegagalan tahap instalasi SDK sebelum Gradle mulai build dan mempertahankan targetSdk 36 yang sudah dipakai aplikasi.

## Validasi

- Workflow dan Gradle diperiksa secara statis.
- ZIP tervalidasi setelah dikemas.
- Build Gradle penuh tetap harus dikonfirmasi di GitHub Actions.
