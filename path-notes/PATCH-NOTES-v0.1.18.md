# NihonGo Native v0.1.18 — Avatar Framing & Procedural Anime Idle

**Base:** v0.1.17 LipSync-ScrollFix. **Scope:** avatar in Home hero ONLY.

## Visual

- Camera target moved to `y=1.05` and distance to `z=1.42` (was `y=0.82/z=1.92`).
- Avatar container wider (`76%` vs `66%`) and taller (`470dp` vs `432dp`), with top at `30dp` vs `50dp`. This lets the torso fill the right-side hero and crops lower body naturally behind the learning card.
- Text remains a higher-z Compose overlay, background assets and other card/sections untouched.

## Native animation (GPU skinning)

- GLB has **zero preauthored animation clips** but a VRM rig and 49 Face morph targets.
- Procedural joint poses for spine, upper chest, neck, head, L/R shoulders, upper/lower arms and hands. Rest rotations preserved; damp sine motion for breathing, subtle sway, gentle head turn, 2.2s greeting, speech gestures, occasional idle gesture.
- Morph A/I/U/E/O lip motion maintained and blink extended to silent idle.
- **Crucial SceneView 4.34 fix:** each borrowed bone `Node` seeds cached translation/quaternion/scale with `bone.transform = bone.transform` exactly once before per-frame rotations. This preserves original GLB rest-pose offsets and prevents detached/collapsed limbs.
- Bone transforms update up to 60 times/second during a spoken phrase or greeting, and up to ~30 times/second during calm idle (actual rendering follows device v-sync). The built-in SceneView/Filament `ModelNode` updates skin bone matrices after the `onFrame` callback.
- No external gesture assets, engine, WebView, or network downloads added.

## Performance/behavior

- Filament is kept in composition through Home scroll to prevent GPU asset recreation.
- Rendering parks during user scroll/offscreen and resumes when stationary Hero is visible, allowing continuous subtle idle movement without burdensome rendering during Reward/Stats scrolling.
- No changes to auth, Home API, Rewards, Settings, or Admin.

## Must test on device

- `./gradlew :app:assembleDebug` (CI) and installation.
- Check avatar head remains in frame at device widths 320–430dp with font scaling 1.0–1.3x, and that it doesn't cover settings button.
- Check waving/gestures for natural arm articulation and model skin stability; adjust joint signs if a different GLB revision uses a changed coordinate system.
- Scroll Reward to Hero repeatedly, monitor frame drops and renderer reinitializations. Check that the avatar pauses while scrolling then resumes when idle.
- Verify TTS visemes + blink remain correct during Japanese speech.

**Build and FPS unverified in this environment**, requiring GitHub Actions / device.
