# NihonGo Native v0.1.19 — Home Hand Gestures from Web

**Base:** v0.1.18 AnimeMotion. **Scope:** gerakan tangan dan lengan Sensei di halaman Home saja. Tidak ada perubahan visual kamera, layout, TTS, lip-sync, menu Pengaturan, API, login, atau admin.

## Sumber referensi yang dibandingkan

- Web: `client/src/components/SenseiAvatar.jsx`, khusus `IDLE_POSE`, `TALK_GESTURES`, `HOME_GESTURES`, pemilihan gestur, easing/envelope, dan `lerpBone` untuk kedua lengan serta pergelangan.
- Web: `client/src/components/HomeAvatarCanvas.jsx`, memastikan pada Home mode `mode="home"` dan `speaking` berasal dari suara.
- Native: `app/src/main/assets/models/waifu_native.glb`. Quaternion pose diam bahu/lengan/tangan sudah sesuai nilai idle JSX; **jangan mengganti rotasi dasar**.

## Perubahan

1. Menghapus wave/tangan kanan terangkat besar saat masuk Home. Gestur itu tidak ada di kode Home web yang dipakai sebagai referensi.
2. Memindahkan pola **empat gestur saat bicara**: tangan kanan menjelaskan, tangan kiri membuka, dua tangan sedikit membuka, dan penekanan kecil tangan kanan. Gestur berganti setiap sekitar 1,05–1,75 detik aktif dengan jeda 0,25–0,67 detik, dan envelope `sin(π * smoothstep(t)) * 0,86` seperti web.
3. Mengikuti **gestur Home saat diam**: angguk/lihat samping tidak memengaruhi tangan; `presentLesson` menggerakkan lengan dengan lembut sewaktu-waktu. Durasi 1,90 detik untuk `presentLesson`, jeda 6–14 detik setelah gestur selesai.
4. Menyalin micro-motion: goyangan lengan, siku dan pergelangan tipis, sedikit lebih aktif ketika berbicara.
5. Setiap target pose dihitung dalam radian sesuai Three.js, kemudian diubah menjadi **delta derajat dari pose GLB** ketika diterapkan pada `Node.rotation` Filament. Rest pose, translasi tulang dan urutan skinning tetap sama. Tidak perlu resource tambahan.
6. Lerp menyesuaikan waktu frame (`dt`), sehingga tidak dua kali lebih cepat pada layar 120Hz dan tidak terlalu lambat ketika update idle dibatasi ~30Hz.

## Berkas yang diubah

- `app/src/main/java/id/vixnihon/nativeapp/ui/NativeSenseiModel.kt` — hanya mengganti driver enam sendi lengan/tangan.
- `app/src/main/java/id/vixnihon/nativeapp/ui/WebHomeHandMotion.kt` — state machine dan kurva gestur dari kode web; pure Kotlin, tanpa dependency Android.
- `app/build.gradle.kts` — v0.1.19/versionCode 19.
- `README.md`, catatan ini — dokumentasi.

## Validasi

- Compile kelas gesture pure Kotlin dengan `kotlinc` lokal: lulus.
- Simulasi 40 detik gerak idle/bicara dengan random seed stabil: semua sudut finite, rotasi terbesar ~22° terhadap pose diam, gerakan bicara terlihat, tanpa wave ~72° seperti versi sebelumnya.
- Quaternion GLB diuji terhadap pose dasar tangan web.
- Android full build, visual di perangkat, FPS dan integrasi SceneView belum dapat diverifikasi tanpa Android SDK. Uji dengan Actions dan Android fisik.
