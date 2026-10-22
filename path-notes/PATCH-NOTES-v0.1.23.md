# NihonGo Native — v0.1.23 Build Fix

Base: `NihonGo-Native-Android-v0.1.22-Credit-History-QRIS.zip`.

Referensi: https://github.com/Ihsan113/ApkNihongo/actions/runs/39700580564/job/119568539475

### Error GitHub Actions

Pada task `:app:compileDebugKotlin` di `app/src/main/java/id/vixnihon/nativeapp/ui/NativeMissionsScreen.kt`:

- Baris 22: `Unresolved reference 'offset'` karena `androidx.compose.ui.draw.offset` tidak ada.
- Baris 70: `Unresolved reference 'Lifecycle'` karena `androidx.lifecycle.Lifecycle` tidak diimport.
- Baris 70: `Unresolved reference 'ON_RESUME' on receiver of type 'ImageVector'` merupakan efek resolusi simbol yang keliru dari masalah `Lifecycle`.

### Perubahan

1. Hapus `import androidx.compose.ui.draw.offset` (gunakan `androidx.compose.foundation.layout.*` yang sudah ada).
2. Tambahkan `import androidx.lifecycle.Lifecycle`.
3. Ubah `versionCode=23`, `versionName=0.1.23` dan README.

Komponen login, Home, Sensei, Settings, Missions logic, Top Up, History, QRIS tidak diubah.

### Validasi

- Perbandingan file source asli terhadap ZIP patch (perubahan hanya file tercatat).
- Pemeriksaan sintaks/import manual dan resolusi nama; `Lifecycle` tersedia dari dependency Lifecycle project.
- `zipfile.testzip()` dan file list berhasil.
- **Build penuh dan tes runtime belum dilakukan.** Jalankan workflow `Build NihonGo Native Android` di GitHub.
