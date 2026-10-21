# Patch notes — v0.1.5

## Diagnosa login Google
- Blok `catch` di LoginScreen sebelumnya menyamarkan semua error Credential Manager menjadi satu pesan generik. Sekarang tipe error dipetakan (batal, tidak ada akun, provider/Play Services, ditolak/OAuth mismatch) dan detail `[NamaException] pesan` ditampilkan + dicatat di Logcat (tag `NihonGoLogin`).

## Kunci debug tetap
- Menambahkan `app/debug.keystore` dan `signingConfigs.debug` supaya SHA-1 APK debug stabil. Sebelumnya setiap run GitHub Actions membuat keystore debug baru, sehingga SHA-1 berubah dan OAuth Android client tidak pernah cocok.
- Workflow mencetak SHA-1 di log Actions.
- SHA-1 keystore ini: 90:C4:6B:CA:94:D9:02:D4:D7:61:E4:DA:B8:50:53:26:73:69:AB:31
- versionCode 6 / versionName 0.1.5.

## Yang harus dilakukan di Google Cloud Console (project yang sama dengan Web client ID server)
Buat OAuth client ID bertipe **Android**: package `id.vixnihon.nativeapp.debug`, SHA-1 di atas. Untuk release, buat client terpisah dengan package `id.vixnihon.nativeapp` dan SHA-1 keystore release.
