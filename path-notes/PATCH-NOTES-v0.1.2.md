# Patch notes — v0.1.2

## Perbaikan kompatibilitas Gradle / Compose

- Mengganti Compose BOM `2026.08.00` (Compose 1.12, membutuhkan AGP 9.1+ dan compileSdk API 37) menjadi `2025.12.00` (Compose 1.10 dan Material 3 1.4), yang lebih sesuai dengan AGP 8.13.2 serta Android SDK 36 yang digunakan workflow.
- Menurunkan `androidx.core:core-ktx` dari `1.18.0` ke `1.17.0` agar tetap menggunakan baseline compileSdk API 36.
- Menurunkan `androidx.activity:activity-compose` dari `1.13.0` ke `1.12.4`, selaras dengan baseline dependency Compose yang lebih konservatif.
- Menaikkan `versionCode` menjadi 3 dan `versionName` menjadi `0.1.2`.

## Akar error

Compose BOM `2026.08.00` menarik Compose Runtime 1.12.0. Dokumentasi resmi rilis Compose menjelaskan bahwa seri 1.12 membutuhkan API 37 dan Android Gradle Plugin yang lebih baru, sehingga tidak cocok dengan project ini yang memakai AGP 8.13.2 dan compileSdk 36. Patch ini mempertahankan toolchain yang sudah ada dan menyelaraskan versi dependency.

## Verifikasi

- Diperiksa bahwa BOM `2026.08.00` tidak lagi direferensikan.
- Diperiksa bahwa Compose BOM `2025.12.00` dan versi dependency kompatibel tercantum pada Gradle file.
- Struktur ZIP akan diperiksa setelah pengemasan. Build APK tetap harus dikonfirmasi oleh GitHub Actions karena lingkungan patch ini tidak menyediakan Android SDK/akses distribusi Gradle untuk menjalankan build penuh.
