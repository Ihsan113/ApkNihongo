# Patch notes — v0.1.3

## Perbaikan Kotlin metadata dan receiver Compose

- Menyelaraskan plugin Kotlin Android dan Kotlin Compose dari `2.2.20` ke `2.4.21`. Dependency `googleid:1.2.1` yang digunakan log build pengguna membawa metadata Kotlin `2.4.0`; compiler Kotlin `2.2.20` tidak dapat membacanya. Kotlin Gradle Plugin 2.4.21 mendukung Gradle 8.13 dan Android Gradle Plugin 8.13.2 berdasarkan matriks kompatibilitas resmi Kotlin.
- Memindahkan pengukuran `maxWidth` dan `maxHeight` dari receiver implisit ke variabel lokal `viewportWidth` / `viewportHeight` di `LoginScreen` dan `OnboardingScreen`. Ini memperbaiki error receiver di dalam `ColumnScope` yang dilaporkan compiler.
- Menaikkan `versionCode` ke 4 dan `versionName` ke `0.1.3`.
- Tidak menggunakan `-Xskip-metadata-version-check`; solusi adalah menyamakan versi compiler, bukan menyembunyikan incompatibility.

## Verifikasi yang dilakukan

- Mengecek bahwa tiga plugin Kotlin di root build menggunakan versi `2.4.21` yang sama.
- Mengecek bahwa dependency metadata dan referensi viewport konsisten pada kedua screen.
- Mengecek integritas ZIP dan validitas YAML workflow.
- Build APK penuh masih perlu dikonfirmasi lewat GitHub Actions karena lingkungan patch ini tidak menyediakan Android SDK siap pakai.
