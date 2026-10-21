# Patch notes — v0.1.4

## Perbaikan compilerOptions DSL

- Memigrasikan `android.kotlinOptions { jvmTarget = "17" }` ke `kotlin { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }`. Kotlin Gradle Plugin 2.4.x memperlakukan `kotlinOptions.jvmTarget` berbentuk string sebagai error, sehingga konfigurasi lama menghentikan evaluasi build sebelum kompilasi kode.
- Menambahkan import `org.jetbrains.kotlin.gradle.dsl.JvmTarget` yang diperlukan oleh DSL bertipe.
- Mempertahankan target bytecode JVM 17 dan konfigurasi Java 17; tidak mengubah kode UI, endpoint autentikasi, atau dependency lainnya.
- Menaikkan `versionCode` menjadi 5 dan `versionName` menjadi `0.1.4`.

## Verifikasi

- Memastikan tidak ada lagi referensi `kotlinOptions` pada konfigurasi app.
- Memastikan hanya ada satu konfigurasi `buildFeatures { compose = true }`.
- Memeriksa YAML workflow, XML resources, dan integritas ZIP.
- Build APK penuh tetap perlu dikonfirmasi oleh GitHub Actions; validasi statis bukan bukti bahwa APK telah berhasil dikompilasi.
