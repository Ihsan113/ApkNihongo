# NihonGo Native Android v0.1.8

## Home native redesign
- Replaced the WebView-based home screen with Jetpack Compose components matching the provided web references.
- Restored the full home hierarchy: hero, continue-learning card, Reward Center, weekly economy/XP charts, four learning metric cards, streak/study-minutes panel, and bottom navigation labels (Rumah, Belajar, Lab, Game, Sensei).
- Added local compact lesson metadata from the source web project to resolve the current lesson/chapter using the same pass and unlock rules (completed + at least two stars, previous lesson passed).
- Home metrics are fetched from the authenticated native REST client (`/api/progress` and `/api/stats/weekly`) instead of using fixed sample values.
- Added the Sensei character as a native GLB asset rendered by SceneView/Filament. No Android WebView or browser/WebGL is used for the screen or 3D model.
- Version raised to 0.1.8 / versionCode 9. Signing configuration and package ID are unchanged.

## Important scope note
The native Home visuals and server-backed data are now in place. Learn lesson content, Mission, Credit Top-up, History, Lab, Game and Sensei destination pages are still separate native screens to implement; Home actions show a notice rather than pretending those screens already exist. The bundled GLB has no skeletal animation tracks, so it renders the character natively in its imported pose (the website's VRM expression/idle animation system is not embedded).

## Build
- `compileSdk` raised to 37 for the selected SceneView release's AndroidX dependencies.
- CI installs Android SDK platform 37 and build-tools 36.0.0.
- Build has not been run in this container; verify using GitHub Actions.
- Applied the web Sensei idle/resting pose as static bone rotations to the local GLB asset (44 mapped bones). This improves the standing silhouette without a browser VRM runtime; the native asset currently contains no animation clips.
