# NihonGo Native Android v0.1.10

## Perbaikan build berdasarkan log GitHub Actions v0.1.9

Log `:app:checkDebugAarMetadata` menunjukkan bahwa `io.github.sceneview:sceneview:4.60.0` dan Compose AndroidX `1.12.1` mensyaratkan compile SDK 37 serta AGP 9.1.0+, sementara proyek memakai SDK 36 dan AGP 8.13.2.

Perubahan: 
- Turunkan SceneView dari `4.60.0` ke `4.52.0`, sebelum perubahan kebutuhan compile SDK 37 pada lini 4.53+.
- Selaraskan Compose dengan `androidx.compose:compose-bom:2026.06.01` (Compose 1.11.4) menggunakan `enforcedPlatform` agar dependency transitif tidak menaikkan Compose ke 1.12.1.
- Selaraskan `androidx.activity:activity-compose` ke `1.12.0`.
- Perbarui `gradle-wrapper.properties` dan bootstrap script `gradlew` dari Gradle `8.13` ke `8.14.4`; AGP tetap 8.13.2 dan Kotlin tetap 2.4.21.
- Pertahankan `compileSdk = 36`, `targetSdk = 36`, Android SDK Platform 36, dan Build Tools 36.0.0.
- Versi aplikasi dinaikkan ke `0.1.10` / `versionCode 11`.

## Tidak berubah

- Package/application ID `id.vixnihon.nativeapp`.
- Alur Google login, endpoint API, struktur layar Home Compose, model GLB native, serta folder `path-notes/`.
- Workflow masih memeriksa SHA-1 sertifikat APK.

## Validasi

Pemeriksaan dilakukan terhadap konfigurasi dan kesalahan AAR metadata yang dilaporkan. Build penuh belum diklaim sukses; verifikasi akhir dilakukan dengan GitHub Actions setelah ZIP ini diunggah dan workflow dijalankan.
