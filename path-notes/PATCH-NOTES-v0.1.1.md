# Patch notes — v0.1.1

## Perbaikan

- Memutakhirkan `android-actions/setup-android` dari `v3` ke `v4`.
- Mengunci input paket SDK ke `platform-tools`, sehingga action tidak lagi mencoba memasang paket `tools` yang sudah tidak disediakan Google.
- Memasang SDK Platform 36 dan Build Tools 36.0.0 dengan `sdkmanager --install` setelah setup SDK.
- Menaikkan `versionCode` menjadi 2 dan `versionName` menjadi `0.1.1`.

Error `Failed to find package 'tools'` terjadi di tahap setup SDK, sebelum proses kompilasi Kotlin/Gradle dimulai. Patch ini menargetkan penyebab tersebut. Kesuksesan kompilasi APK tetap harus dipastikan dari hasil GitHub Actions; error lanjutan apa pun perlu ditangani berdasarkan log langkah build berikutnya.
