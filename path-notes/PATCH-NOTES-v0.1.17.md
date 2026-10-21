# v0.1.17 – Lip-sync + scroll Home

Base: ZIP v0.1.16; diubah hanya `HomeScreen.kt`, `NativeHomeSpeech.kt`, `NativeSenseiModel.kt`, Gradle versi, README dan patch notes.

## Lip Sync berbasis model asli
- Verifikasi GLB langsung: mesh `Face` mengandung 49 morph target, `extensions.VRM.blendShapeMaster` A/I/U/E/O pada target indices 32–36, Blink 12.
- `NativeJapaneseTts` meneruskan status mulai/selesai, serta `onRangeStart` untuk pemetaan kana → mulut jika mesin mengirimnya.
- SceneView mengubah bobot morph Face lewat `RenderableNode.setMorphWeights()` saat frame aktif dengan jeda minimal 45ms (kurang-lebih 22Hz, GPU morphing). Ketika TTS berhenti, semua bobot kembali nol.
- Lipsync bersifat *approximate*, TTS Android tidak menyediakan spektrum/phoneme timing presisi; mesin yang tak menyediakan `onRangeStart` masih menghasilkan animasi berdasarkan status berbicara.

## Scroll GPU & CPU
- Mengganti LazyColumn dengan verticalScroll karena Home hanya memiliki hero + reward + statistik; ini mempertahankan renderer/model agar tidak re-init saat scroll balik.
- Saat scroll sedang bergerak atau hero tak terlihat, SceneView tetap terpasang namun rendering diparkir (`isRendering=false`). Pada keadaan diam rendering hanya aktif singkat saat load (1.6 detik), ucapan atau tail 160ms.
- Mempertahankan TextureSurface transparan, geometri dan tekstur berkualitas seperti versi sebelumnya; tidak memaksakan penurunan resolusi.
- GPU Filament sudah hardware accelerated; tidak perlu trik manifest yang membuat scroll kembali lambat.

## Validasi sebelum rilis
1. Build GitHub Actions `:app:assembleDebug`.
2. Uji suara aktif: pastikan bibir bergerak sewaktu audio terdengar dan diam kembali setelah selesai.
3. Scroll puluhan kali dari Home ke Reward/Statistik dan kembali; periksa lewat profiler frame rendering/jank, konsumsi CPU/GPU, serta memori native.
4. Uji mode TTS nonaktif dan layar Home di background: renderer tidak meneruskan frame terus-menerus.
5. Jika suhu tetap meningkat, gunakan profiler perangkat untuk memilih cap FPS saat bicara; jangan turunkan kualitas asset tanpa bukti profil.

**Belum diuji build atau frame timing di Android fisik.**
