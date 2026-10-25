# BetterSeed v1.2.1 — Android APK signing yang konsisten

## Tujuan

Setiap APK yang dihasilkan oleh GitHub Actions (`push`/`workflow_dispatch`) **harus** ditandatangani oleh sertifikat yang **sama**, sehingga SHA-1 untuk Google Cloud Android OAuth tidak berganti setiap build. Proses ini menggunakan keystore permanen, **bukan** debug keystore otomatis milik GitHub runner.

> **Sekali dibuat, jangan pernah mengganti keystore atau alias signing ini.** Simpan backup `.jks` dan password ke tempat privat yang aman. Keystore **tidak ada di ZIP dan tidak boleh di-commit**.

## Langkah 1 — Buat keystore satu kali di perangkat yang aman

Memerlukan Java JDK (`keytool`). Di PC Linux/Ubuntu/Termux dengan JDK, jalankan di luar folder repo:

```bash
mkdir -p "$HOME/betterseed-private"
keytool -genkeypair -v \
  -keystore "$HOME/betterseed-private/betterseed-release.jks" \
  -storetype PKCS12 \
  -keyalg RSA -keysize 3072 -validity 10000 \
  -alias betterseed
```

Pilih password keystore dan password key yang aman, lalu simpan di password manager. **Untuk PKCS12, biasanya key password sama dengan store password**; gunakan password tersebut untuk kedua secret jika diminta.

Jika **sudah memiliki keystore resmi BetterSeed dari build lama**, gunakan keystore lama, **jangan** generate yang baru.

Sebelum mengunggah secrets, catat fingerprint dari keystore permanen:

```bash
keytool -list -v -keystore "$HOME/betterseed-private/betterseed-release.jks" -alias betterseed
```

Cari baris **SHA1:** dan simpan nilainya. Fingerprint ini aman untuk didaftarkan ke Google Cloud; password dan file keystore tidak aman untuk dibagikan.

## Langkah 2 — Buat GitHub Actions repository secrets

Buka repository **Ihsan113/BeterSeed → Settings → Secrets and variables → Actions → New repository secret**.

Tambahkan **empat secrets signing** berikut (serta Web client ID yang sudah digunakan oleh fitur Drive):

| Nama Secret | Isi |
|---|---|
| `BETTERSEED_KEYSTORE_BASE64` | Isi file `.jks` setelah encoding base64 (satu baris) |
| `BETTERSEED_KEYSTORE_PASSWORD` | Password keystore |
| `BETTERSEED_KEY_ALIAS` | Alias signing, mis. `betterseed` |
| `BETTERSEED_KEY_PASSWORD` | Password private key (untuk PKCS12 sering sama dengan store password) |
| `BETTERSEED_GOOGLE_WEB_CLIENT_ID` | (Existing, untuk Google Sign-In) Web OAuth Client ID dari Google Cloud |

Tambahkan juga **GitHub Actions Variable (bukan Secret)** lewat **Settings → Secrets and variables → Actions → Variables → New repository variable**:

| Nama Variable | Isi |
|---|---|
| `BETTERSEED_PINNED_SHA1` | SHA-1 keystore yang kamu catat tadi (mis. `AA:BB:...`) |

Variable ini **mengunci** identitas antar-build. Jika kamu tidak sengaja mengganti file JKS/alias, build akan gagal sebelum APK diterbitkan.

Cara mendapatkan isi `BETTERSEED_KEYSTORE_BASE64`:

```bash
base64 < "$HOME/betterseed-private/betterseed-release.jks" | tr -d '\n' > "$HOME/betterseed-private/keystore.base64.txt"
```

Buka file `keystore.base64.txt`, salin **seluruh isinya** ke field Secret di GitHub; jangan commit teks base64 ini atau kirim ke percakapan. Hapus file `.base64.txt` setelah secrets tersimpan jika tidak diperlukan lagi.

Perhatian: untuk memasukkan secret, gunakan tampilan GitHub Secrets secara langsung. Jangan paste nilai secret di log Actions, isu GitHub, atau `README`.

## Langkah 3 — Build via GitHub Actions

Push source BetterSeed **v1.2.1+32** dari ZIP. Workflow akan:

1. Build Flutter platform Android seperti biasa dan menjalankan `flutter analyze` serta semua test.
2. Mengambil `.jks` hanya dari GitHub Secrets ke folder sementara runner dan memastikan keystore+alias valid.
3. Memverifikasi SHA-1 keystore terhadap variable `BETTERSEED_PINNED_SHA1`, lalu mengonfigurasi Android Gradle `release` agar memakai keystore tersebut.
4. Membangun **`app-release.apk`** menggunakan signing tetap.
5. Memverifikasi SHA-1 sertifikat APK sama dengan SHA-1 keystore, lalu mencetak fingerprint SHA-1 aman di log job.
6. Mengunggah artifact `BetterSeed-stable-release-apk` (tanpa keystore/password).

Jika secret hilang, variable fingerprint tidak cocok, atau password salah, **workflow gagal dengan pesan yang jelas**, tidak membuat APK debug dengan SHA-1 berubah-ubah sebagai pengganti. Pada PR, hanya analisis dan tes yang dijalankan (tanpa tanda tangan dan tanpa APK distribusi).

## Langkah 4 — Daftar SHA-1 ke Google Cloud Console

Cari log step **Configure persistent release signing** atau **Verify APK signature matches stable SHA-1**. Salin nilai **SHA-1** yang dicetak (fingerprint sertifikat, **bukan** isi key).

Di Google Cloud Console → Google Auth Platform → Clients, buat/edit Android OAuth client dengan:

- Android package: `com.example.betterseed` (sesuai APK saat ini)
- SHA-1: fingerprint dari log signed release APK

Gunakan **Web Client ID** terpisah (sebagai `BETTERSEED_GOOGLE_WEB_CLIENT_ID`) untuk BetterSeed Google Drive. Selain itu Drive API/OAuth consent screen harus sudah disiapkan.

## Penting: upgrade dari APK debug lama

- **Paket `com.example.betterseed` yang sama + sertifikat yang sama + versionCode lebih tinggi** → instal sebagai update dan data lokal tetap ada.
- **Debug APK lama** yang ditandatangani runner GitHub sebelumnya **tidak otomatis cocok** dengan keystore release baru. Android akan menolak update dengan signature berbeda. Ini tidak bisa diperbaiki dengan menyamakan SHA-1 tanpa memiliki private key lama.
- **Jangan uninstall APK lama dulu** jika data checklist, air, dan XP masih penting. Cadangkan data yang ada sebelum migrasi ke APK release baru (jika Drive sudah berfungsi, gunakan Backup Now lalu pastikan backup bisa diakses; atau buat salinan data dengan metode tepercaya). Setelah uninstall, data lokal biasanya ikut hilang.
- **Google Play App Signing:** SHA-1 sertifikat yang dipakai Google Play untuk APK yang diunduh pengguna bisa berbeda dari SHA-1 keystore upload. Ketika rilis melalui Play, daftar juga SHA-1 **App Signing Certificate** dari Play Console ke Google Cloud.

## Validasi dan keamanan

- File `.jks` dan `key.properties` diabaikan Git dan dibuat hanya pada runner selama build. Jangan dibagikan atau dimasukkan ke ZIP.
- Backup keystore secara offline (minimal dua tempat aman) karena jika hilang APK distribusi di luar Google Play bisa kehilangan kemampuan update.
- Jika fingerprint berubah, workflow akan menghentikan build karena berbeda dengan **variable `BETTERSEED_PINNED_SHA1`**, sebelum menerbitkan APK. Jangan mengubah variable fingerprint atau keystore setelah digunakan untuk rilis pertama.

## Test manual sesudah konfigurasi

- [ ] Buat dua build berturutan. SHA-1 dari dua log identik.
- [ ] Unduh release APK kedua dan pasang sebagai update dari release APK pertama tanpa uninstall.
- [ ] Pastikan checklist, progress, tema Garden, reminder, dan widget masih tersedia.
- [ ] Coba Google Login/Google Drive setelah OAuth didaftarkan menggunakan SHA-1 release.
