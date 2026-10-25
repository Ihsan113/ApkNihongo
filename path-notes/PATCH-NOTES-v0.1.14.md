# NihonGo Native Android 0.1.14 — Home parity & native shortcuts

## Scope
Based directly on `client/src/screens/HomeScreen.jsx` from the supplied **nihongowebmasalah.zip** and Android **v0.1.13**. **Home only**. No WebView. UI still needs screenshot review on an actual device. This is source ZIP, not a compiled/tested APK.

## Web → Native cross-check

| Home on web | Native in 0.1.14 | Notes |
| --- | --- | --- |
| Hero 506 CSS px, day background 24%/20%, gradient dark fade | 506dp, BiasAlignment(-0.52,-0.60), WebP original background, progressive overlay | Updates every minute without requesting network |
| Header 日本語, subtitle, settings icon | Native Compose typography 42sp, 12sp, 46dp icon, header positioned after safe top inset | Android safe status bars |
| Right avatar in 66% width / 432px high at top 50px | Native GLB TextureSurface (SceneView/Filament) in same stage area | Full VRM motions, facial expressions, lip-sync **not** recreated yet; native GLB pose is static |
| Welcome + JP TTS + random 10–15s idle phrases | Greeting text matches web; Japanese local TTS & idle loop available under Home Settings opt-in | Disabled by default for performance/user audio preference; stops when Home offscreen/background |
| Continue card overlaps hero by 42px | Custom `Layout` measures actual card and overlaps by 42dp | Next chapter chosen from server progress + local lesson metadata; N4 badge corrected |
| Unlocked next lesson: previous passed with >=2 stars | Same lookup and conditions | Native chapter list opens; **native lesson player is not yet built** |
| Reward Center: Mission / Topup / History shortcuts | Three adaptive tiles; fits all in standard phones; narrow phones can swipe | No clipped text; uses proper destination routes |
| Daily missions and claim action | Authenticated GET `/api/missions/today`, POST `/api/missions/claim` after eligibility check | Data real, never simulated |
| Credit package catalog | Authenticated GET `/api/credits/packages` | Read only. Payment flow not started until secure native QRIS screen is built |
| Credit & XP history | Authenticated GET `/api/credits/history` | Filters kind & pagination |
| Weekly XP data, summary, credit, level | Authenticated `/api/stats/weekly`, nested/raw payload support | Zero values are valid; stale last success retained on connection loss with retry notice |
| XP bar chart scales to highest value | Native Canvas with dynamic ticks and 7 bars | Dates use local 7 days and correct ID/EN day labels |
| Stats cards & economy compact proportions | Reduced oversized native padding/icons/text | Matches CSS hierarchy more closely |
| Streak and 7-day minutes sparkline with day hover | 22dp week dots with checkmarks + tappable graph points showing day/minutes overlay | Tooltip does not push layout down |
| Home bottom nav | Home/Belajar open native section; Lab/Game/Sensei still marked not implemented | Other modules outside Home migration scope |
| Web incremental refresh after focus/data changes | Native refresh on Android resume and on returning from linked Home shortcuts | Real data re-fetched without per-frame network |

## Performance safeguards

- `LazyColumn` virtualization for Home; no browser/WebView.
- Static native background artwork converted to WebP already present (assets kept unchanged).
- 3D model animation stays off; Compose lazily instantiates first Home item, isolated from separate stats state changes.
- Japanese TTS engine is only created when user enables voice; stops and releases on exit.
- Local lesson metadata loaded once off main thread; server progress and weekly stats fetched concurrently on IO.
- 7-day charts use Compose Canvas without WebKit or third-party chart engine.
- Refresh time-of-day background every minute without re-fetching APIs.

## Known limits / honest status

- **APK has not been compiled** in this environment: Android SDK/Gradle distribution unavailable. Syntax checked with local Kotlin parser without Android dependencies; this cannot substitute for Gradle `assembleDebug`.
- Full VRM animation/emotions/phoneme lipsync and light-theme color parity are not part of this patch.
- Native lesson player, actual QRIS order/QR screen, and other bottom navigation modules need their own implementation phases. The Home buttons show real native data/destinations; they do not counterfeit completion of these modules.
- Confirm exact visual parity by testing on real devices at 320 / 360 / 393 / 430dp and font scale 1.0 & 1.3.

## Build

`./gradlew :app:assembleDebug` using JDK 17 and Android SDK 36 (existing GitHub Actions workflow). If CI fails, provide *the first Kotlin compiler error* (not only the stack trace) for the next patch.
