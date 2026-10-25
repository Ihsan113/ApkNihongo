> **v1.2.3 signing clarity:** The MT Manager *file checksum SHA-1* is not the Google OAuth *signing certificate SHA-1*. GitHub Actions verifies signing before publishing and now bundles `CERTIFICATE_SHA1_README.txt` next to the release APK. See `docs/CERTIFICATE_VS_APK_FILE_SHA1_v1.2.3.md`.

## BetterSeed v1.2.2 — Signing verification fix

This update fixes false failures in the post-build APK certificate fingerprint check while preserving strict pinned SHA-1 enforcement. See `docs/APK_SIGNATURE_VERIFIER_HOTFIX_v1.2.2.md`. Reuse your existing release keystore.

## v1.2.1 — Stable APK signing / SHA-1

- GitHub Actions now produces a **release APK signed by a persistent keystore** from repository secrets.
- On every build, the signing certificate SHA-1 is checked against the resulting APK. Missing signing secrets **fail** rather than produce a new debug SHA-1.
- **One-time setup required:** [docs/STABLE_ANDROID_SIGNING_v1.2.1.md](docs/STABLE_ANDROID_SIGNING_v1.2.1.md).
- No keystore or password is included with this project.

## v1.2.0 — Reminder guard + Google Drive (manual) backup

- Checklist and Habit Composer now explain when reminder master is OFF and provide an **Enable** action (which may request Android notification permission).
- Google Drive private `appDataFolder` backup/restore, user-approved replacement and one-level local Undo.
- Google Sign-In requires external OAuth project, Android package certificate SHA, Drive API, and optional CI Web client ID secret before usable. Setup: [docs/GOOGLE_DRIVE_BACKUP_v1.2.0.md](docs/GOOGLE_DRIVE_BACKUP_v1.2.0.md).
- Cloud backup is manual, not automatic/bidirectional; offline tracking never depends on a Google account.

## Latest update

- v1.1.7: Moonlight tree sprites normalized to the actual visible dimensions of the Default stages; focus mode with immersive fullscreen controls added.
- v1.1.6: increased the Moonlight tree presentation so it matches the default tree scale better in the garden and widget.
- v1.1.5: enlarged the Moonlight tree stages to match the visual scale of the default tree in both the garden and Android widget.
- v1.1.4: restored full-screen garden backgrounds with no blur, using direct cover rendering for a cleaner immersive look.
- v1.1.3: replaced Moonlight dark theme background with the selected premium moonlit landscape and kept the repository clean with only active visual assets.

## Update v1.1.2 — Adaptive Garden & aligned planting spot

See `docs/ADAPTIVE_GARDEN_v1.1.2.md`. Moonlight soil clearing is enhanced; both themes show uncropped sharp backdrops with soft edge extension. Android widget keeps the simple gradient background and themed trees.

# BetterSeed v1.1.1 — Garden Pack CI hotfix

A focused patch built on **v1.1.0**. Retains the Shop / My Collection
bundle system, Free Moonlight Pack, dynamic garden, stage celebrations,
and Android widget theme integration.

## Corrections

1. `BetterSeedShopPage` now renders its two pack cards eagerly inside a
   `SingleChildScrollView + Column`, rather than letting `ListView` lazily
   omit cards that are below the fold. The Shop remains scrollable on compact
   phones and at 1.6x–2.0x text sizes.
2. The Android widget channel regression test now checks the newly added
   `theme` field for both `default` and `moonlight` values. The native app
   code already transmitted this correctly.
3. Version advanced to **1.1.1+24** so that it can update v1.1.0+23.

## Important

The failures in run 40146999007 were **four test failures**, not analyzer
errors. `flutter analyze` passed there. This patch retains all runtime
features; no feature tests were disabled or deleted.

Run the GitHub workflow to validate Flutter tests and build an installable
APK. Flutter is not available in this export environment, so local Flutter
analyze and tests are not claimed as passing. `android/` is normally generated
by Flutter and is excluded from this source ZIP; `native/android/` is included.

For updating GitHub, extract into the repository root, run `git add -A`,
commit and push. Do not uninstall the existing app if you want to keep habits
and progress.
