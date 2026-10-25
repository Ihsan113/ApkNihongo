# NihonGo Native Android v0.1.11

## Perbaikan build berdasarkan log GitHub Actions v0.1.10

Task `:app:checkDebugAarMetadata` gagal karena `io.github.sceneview:sceneview:4.52.0` masih meminta `compileSdk 37`, sedangkan proyek dipertahankan pada SDK 36 dan AGP 8.13.2. Log terbaru tidak menunjukkan masalah metadata Compose lagi; isu yang tersisa adalah SceneView.

Perubahan:
- Turunkan `io.github.sceneview:sceneview` dari `4.52.0` ke `4.34.0`. Rilis SceneView 4.35.0 adalah titik perubahan compile SDK ke 37, sehingga 4.34.0 dipilih untuk tetap menggunakan SDK 36.
- Pertahankan `compileSdk = 36`, `targetSdk = 36`, Android SDK Platform 36, dan Build Tools 36.0.0.
- Pertahankan Compose BOM enforced `2026.06.01`, `activity-compose 1.12.0`, AGP 8.13.2, Kotlin 2.4.21, dan Gradle 8.14.4.
- Naikkan versi aplikasi menjadi `0.1.11` / `versionCode 12`.

## Tidak berubah
- Package/application ID `id.vixnihon.nativeapp`.
- Login Google, endpoint API, Home Jetpack Compose, model GLB, renderer SceneView/Filament native, workflow signing, dan folder `path-notes/`.
- Tidak menggunakan WebView.

## Validasi
- Perubahan versi dependency dan konfigurasi diperiksa secara statis.
- Build Gradle penuh belum dijalankan/terkonfirmasi di lingkungan penyusunan ZIP. Jalankan GitHub Actions; jika ada error berikutnya, gunakan log baru untuk memastikan langkah selanjutnya.
