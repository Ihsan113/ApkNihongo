# Patch notes — v0.1.6

- Signing config `shared` untuk debug dan release: memakai keystore dari GitHub Secrets (`NIHONGO_KEYSTORE_BASE64`, `NIHONGO_KEYSTORE_PASSWORD`, `NIHONGO_KEY_ALIAS`, `NIHONGO_KEY_PASSWORD`), fallback ke `app/debug.keystore`.
- Menghapus `applicationIdSuffix ".debug"`: debug sekarang memakai package `id.vixnihon.nativeapp`, sama dengan yang didaftarkan di Google.
- Workflow mencetak SHA-1 sertifikat APK hasil build dan membandingkannya dengan 8DDCC406...589E.
- .gitignore: `*.jks`, `client_secret*.json`, `.env`.
- versionCode 7 / versionName 0.1.6.
