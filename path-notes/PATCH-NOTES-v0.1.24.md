# NihonGo Native v0.1.24 — Loading & Form Polish

Base: v0.1.23 Kotlin BuildFix. Fokus hanya loader dan UI input/filter.

## Source berubah

1. `app/src/main/java/id/vixnihon/nativeapp/ui/NativeGlobalLoading.kt`: hilangkan bentuk garis persegi panjang yang terpotong di tepi kiri. Ganti `rememberInfiniteTransition` dengan clock dari `withFrameNanos` (sekitar 30fps) agar loader tidak membeku ketika skala animasi Android 0x. Gerakan hanya hidup selama composable loading ada, tidak menahan transisi halaman.
2. `NihongoFormField.kt` (baru): field Compose konsisten untuk label, placeholder, focus, tombol IME, helper text, dan bentuk membulat.
3. `NativeSettingsScreen.kt`: rapikan rating, saran, tinggi input, hint dan keyboard; versi Tentang diselaraskan.
4. `NativeCreditTopupScreen.kt`: input Order ID tidak lagi memakai OutlinedTextField bawaan dengan label yang meloncat, dukung IME Search, dan cegah tombol Cek Order tanpa ID.
5. `NativeCreditHistoryScreen.kt`: ringkasan filter collapsed default, segment pilihan seimbang, periode dua kolom, filter custom dan reset. Batasi tanggal `Dari <= Sampai`.
6. `NativeCreditWidgets.kt`: CreditPill menerima Modifier agar segmen dapat berbagi ruang.
7. `AndroidManifest.xml`: `adjustResize` untuk mengurangi risiko tombol Kirim Saran tertutup keyboard pada perangkat edge-to-edge.
8. `app/build.gradle.kts`: versi 0.1.24 / code 24.

## QA setelah build GitHub Actions

- Buka aplikasi dengan Developer options → Animator duration scale **0x**, cek cincin dan kelopak loading tetap bergerak saat benar-benar ada request aktif. Ulangi di 0.5x dan 1x.
- Tidak boleh muncul outline kotak besar terpotong pada bagian kiri layar.
- Kirim Saran: ketik multiline, buka keyboard, beri bintang, kirim, pastikan status server dan tombol disabled saat sibuk.
- Beli Kredit: tempel ID order lama, tekan Search pada keyboard atau Cek Status Order, kembali. Gunakan order non-transaksi nyata untuk uji.
- Riwayat: buka-tutup filter, coba Semua/Kredit/XP; Hari ini/7/30/Semua waktu; custom Dari/Sampai; reset; filter tetap sesuai data server.
- Periksa tampilan pada layar 320–430dp serta mode terang/gelap.
- JANGAN uji pembayaran uang sungguhan sebelum mengonfirmasi order, biaya, QR, serta status pada server.

## Batasan

Build Android penuh dan screenshot perangkat belum diverifikasi di sini. Semua status dan nominal pembayaran tetap berasal dari backend; patch ini hanya UI dan jam loading.
