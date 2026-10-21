# NihonGo Native Android v0.1.7 — Home Web Parity

## Perubahan
- Home pada APK kini memuat Home NihonGo web resmi pada first-party HTTPS WebView agar UI, Sensei VRM, animasi/sapaan, Reward Center, statistik mingguan, dan navigasi/aksi Home menggunakan implementasi web aktual.
- Login Google tetap memakai alur native Credential Manager yang sudah ada.
- Sesi dari native disinkronkan ke `localStorage` hanya pada origin `https://www.vixnihon.web.id` / `https://vixnihon.web.id`. WebView disembunyikan sampai bootstrap selesai agar halaman login publik tidak berkedip sebelum sesi native tersedia.
- Status onboarding lokal disiapkan untuk akun yang sudah melewati onboarding native; halaman landing marketing tidak ditampilkan lagi setelah bootstrap.
- Logout dari UI web diteruskan kembali ke sesi native agar pengguna kembali ke login Google native. Perubahan bahasa dari pengaturan web juga disimpan ke preferensi native.
- Navigasi ke tautan HTTPS/HTTP di luar domain NihonGo dibuka lewat aplikasi browser eksternal, bukan diberi akses ke jembatan JavaScript native.
- Ditambahkan pesan dan tombol coba lagi saat Home web tidak dapat dimuat.
- Version code dinaikkan ke 8 dan version name menjadi 0.1.7.

## Batasan dan validasi
- Ini adalah pendekatan hybrid untuk menjaga kesamaan perilaku dengan versi web; layar Home bukan lagi replika Compose mandiri.
- Rendering model VRM memerlukan WebView yang mendukung WebGL dan koneksi internet untuk memuat situs/aset web.
- Build Android penuh dan pengujian runtime belum dianggap lulus sampai workflow GitHub Actions berhasil dan APK dites pada perangkat.

- Pemeriksaan SHA-1 di workflow diselaraskan dengan sertifikat yang teramati pada build GitHub terakhir (`90C46BCA94D902D4D761E4DAB85053267369AB31`). Jika custom keystore dari Secrets menghasilkan sertifikat lain, nilai `EXPECTED_SHA1` tetap harus disesuaikan.
