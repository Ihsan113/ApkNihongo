# NihonGo Native Android v0.1.12 — Home Layout Fix

## Tujuan

Menyelaraskan halaman Home Jetpack Compose dengan implementasi Home web tanpa memakai WebView. Fokus masalah laporan pengguna: kepala karakter terpotong, avatar bertabrakan dengan teks, ketiga shortcut Reward Center serta teksnya terpotong, dan hero 680dp yang menyisakan ruang kosong.

## Perubahan source

| Komponen | Sebelum | Sesudah |
|---|---|---|
| Kamera model Sensei | Posisi `(0, 0.84, 2.35)` dan tanpa arah target yang eksplisit | Posisi `(0, 0.80, 2.90)` dengan `lookAt(0, 0.80, 0)`; `scaleToUnits = 1.60f` |
| Avatar | Kanvas `284 × 432dp` mulai 56dp dari atas | Kanvas mengikuti lebar layar (61%, batas 188–264dp) dengan tinggi 320–383dp; posisi kanan dan rendah |
| Hero | `height(680.dp)` | `(maxWidth × 1.28)` dibatasi 480–540dp |
| Hero dan kartu | Card `align(BottomCenter).offset(y=35dp)` di container 680dp | `Layout` mengukur tinggi kartu dengan overlap 42dp, tanpa gap palsu |
| Teks hero | Sebagian bertabrakan dengan model | Lebar sapaan dibatasi, urutan `zIndex`, text shadow dan scrim gradasi |
| Reward | Scroll horizontal, kartu `width(126dp).height(126dp)` | Tiga kolom berbobot sama, jarak 8dp, `heightIn(min=156dp)`, teks wrap |
| Footer kartu belajar | Label jumlah selesai dan tombol bisa berdesakan | Label memakai `weight(1f)`, tombol tetap memiliki ruang |

## Cakupan yang sengaja tidak diubah

- Identitas aplikasi (`id.vixnihon.nativeapp`), autentikasi Google, keystore debug, repository API, DataStore, metadata materi, perhitungan progres dan statistik.
- Warna dan navigasi di luar halaman Home; halaman Belajar/Lab/Game/Sensei belum diimplementasikan penuh di versi ini.
- Renderer tetap SceneView 4.34.0 / Filament; tidak memakai WebView.

## Pemeriksaan / status

- Struktur dan source native dibandingkan dengan `client/src/screens/HomeScreen.jsx` dan `client/src/components/HomeAvatarCanvas.jsx`.
- Ukuran AABB GLB diperiksa: y sekitar -0.017 sampai 1.594 meter, sehingga titik tengah pada y≈0.79 meter digunakan untuk kamera.
- Perhitungan layout dicek statis untuk 320/360/393/480dp; reward tetap muat tiga kolom tanpa overflow horizontal.
- **Belum terverifikasi compile Android atau render perangkat**: lingkungan pembuatan paket tidak memiliki Android SDK maupun distribusi Gradle. Uji APK lewat Android Studio atau GitHub Actions sebelum menganggap patch final untuk seluruh variasi perangkat.

## Checklist uji APK

1. Masuk akun, buka Rumah. Pastikan wajah/rambut kepala Sensei **tidak terpotong** dan sapaan tidak tertutup avatar.
2. Coba layar 320dp, 360dp, 393dp. Pastikan ada **3 kartu** Reward di layar tanpa scroll kanan, dan keterangan kartu terbaca.
3. Scroll dari hero sampai statistik. Pastikan jarak antara kartu belajar, Reward Center dan statistik tidak aneh.
4. Ubah ukuran font sistem menjadi 130%. Pastikan kartu Reward bertambah tinggi jika diperlukan, dan tombol Lanjutkan tidak keluar dari layar.
5. Uji Pengaturan, mengganti bahasa, dan sign out. Pastikan fungsi lama tetap bekerja.
6. Jika model terlihat terlalu kecil/besar pada ponsel, bandingkan screenshot layar target dengan ukuran kamera dan viewport sebelum mengubah scale glTF lagi.
