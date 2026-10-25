# v0.1.21 — Global Loading Parity (Native)

**Base:** `NihonGo-Native-Android-v0.1.20-Missions-WebParity.zip`.
**Web reference:** `nihongowebmasalah.zip/client/src/components/GlobalLoading.jsx` and `client/src/App.jsx`, with `client/public/loading/nihongo-loading-logo.png`.

## What changed

- New `ui/NativeGlobalLoading.kt`: a reusable fullscreen Jetpack Compose version of the web's `GlobalLoading` in Indonesian and English. Supports `variant = "account" / "page"`, `compact`, optional messages, optional genuine 0–100 progress. Distinguishes account vs page copy.
- Bundled the **original** NihonGo loading logo as a transparent, resized 600px WebP `drawable-nodpi/loading_nihongo_logo.webp` (~65KB), no duplicate network request or WebView.
- Visual details: dark/light background gradient, soft pink/red/gold ambient glows, faint decorative arches/ripples, seven sakura petals, orbiting multicolor 5dp ring, floating logo, NihonGo wordmark, `読み込み中…`, account/page status, red–gold progress shimmer, four info labels in full mode, learning tip.
- Compose InfiniteTransition handles GPU-friendly spinning, floating, shimmer, and petal movement while composable is visible. No background animation tasks. Determinate progress uses a smoothed bar, never fabricates percentage.
- `NihongoRoot`: startup session validation uses the full **account** loader instead of the old `日本語 + CircularProgressIndicator` spinner.
- `HomeScreen`: first entry uses the **page** loader until initial lesson catalog and home stats/progress requests complete (even on error, it dismisses so user can retry). Existing Home/3D/TTS/layout, and all data sources are retained. On refresh, the entire screen does not flash a loader again.
- `NativeMissionsScreen`: initial GET displays page loader, only while initial data is missing. Refresh and claim do not block the screen with overlay.
- `HomeLinkedDestination`: initial top-up and history requests show page loader. On later pagination/refresh, existing content remains usable and no fullscreen overlay is displayed. Learn list displays immediately (no network wait).
- Android `versionName` `0.1.21`, `versionCode` `21`.

## Intentional native improvement

The web uses a 3000ms artificial minimum delay for first-time lazy JS imports. Native doesn't download those JS bundles and **doesn't impose a fake 3-second delay**: overlays automatically disappear on real data readiness to keep navigation responsive. Actual backend response may still take time. This only imitates the design/behavior, not the web's artificial wait.

## Regression checklist (after GitHub Actions build)

1. Launch signed-in app: the full **account** loading with rotating ring/logo/sakura displays during session verification. It goes away once auth resolves.
2. After fresh sign-in or app restart: **page** loader displays during Home initial data fetch. No endless spinner when backend fails: Home retry must remain reachable.
3. Home → Misi / Beli Kredit / Riwayat: first data fetch displays the compact page loading; successful and error responses both close it. Navigation Back should work. Retry and claim should not trigger a second blocking screen.
4. Compare Android loader against `GlobalLoading.jsx`: logo, red-gold ring, background, text, tip, pill badges, bilingual copy, petals. Test light mode and 320/360/393dp screen sizes, increased font scale, and orientation enforced portrait.
5. Confirm no new allocations of Filament, changed TTS behavior, or gameplay/business rules; verify Home scroll remains smooth after exiting loader.

## Verification status

- New animation is built from native Compose APIs and shipped in source. Assets converted successfully and patch is scoped to named files.
- Pure Kotlin syntax parsing attempted with local `kotlinc`, but the environment lacks Android/Compose classpath, so an **APK build and real rendering/performance verification are not available locally**. Use GitHub Actions `:app:assembleDebug` and test on Android device.
