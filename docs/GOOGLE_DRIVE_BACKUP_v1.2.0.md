# BetterSeed v1.2.0 — Google Drive Backup and Restore

## What works

- Local habits, reminder time preferences, water/XP, credited completion dates, owned Moonlight pack and active theme remain the source of truth.
- Settings → Google Drive Backup → **Connect Google Drive** (explicit consent), **Backup now**, **Restore**, **Disconnect**.
- The newest file named `betterseed_backup_v1.json` is stored in the user's private **appDataFolder**, invisible in normal Drive UI. Only the app's `drive.appdata` scope is requested.
- Restoring shows the backup date and counts before allowing replacement, and first writes a local emergency checkpoint. The **Undo last cloud restore** option in Settings reverts from that checkpoint.
- Device-wide notification permission/master switch and audio preferences remain local. On another phone the reminder master must be enabled explicitly.
- Internet is needed for Drive interactions, but offline tracking is unaffected.

**This is manual cloud backup/restore, not bidirectional realtime sync**. The latest manual **Backup now** overwrites the existing Drive backup file. Do not use multiple devices concurrently without restoring the latest backup before uploading again. Automatic overwrite was intentionally not enabled for safety.

## Google Cloud configuration REQUIRED before sign-in works

1. Create a Google Cloud project and enable the **Google Drive API**.
2. Configure OAuth consent screen / Google Auth Platform. Add the `https://www.googleapis.com/auth/drive.appdata` scope. If the app is in **Testing**, add Google accounts as test users.
3. Create an **Android OAuth client** for the exact package ID `com.example.betterseed` and the SHA-1 of the certificate signing the installed APK. Register fingerprints for your stable signing certificate, and any debug signing certificate used for tests.
4. Create a **Web application OAuth client** in the same Cloud project. The **Web client ID** is used as the `serverClientId` in Google Sign-In Android when not relying on a `google-services.json` configuration file. **Do not embed a client secret**.
5. Add the Web client ID to the GitHub repository **Actions secret** named `BETTERSEED_GOOGLE_WEB_CLIENT_ID`. Workflow passes it as `--dart-define=BETTERSEED_GOOGLE_SERVER_CLIENT_ID=...` (blank if no secret).
6. **From v1.2.1 onward**, the GitHub workflow builds a **release APK signed with a persistent key** from GitHub Actions Secrets (see `docs/STABLE_ANDROID_SIGNING_v1.2.1.md`). Use the SHA-1 shown in the signed build log for the Android OAuth client. This only works after completing the one-time keystore setup; no private key is included in the ZIP.
7. Test on an Android device with Google Play Services, internet access, and a Google account permitted by the consent screen.

Without steps 1–7, the app still builds and works offline, but its Drive Sign-In cannot be expected to succeed. Google configuration is external to the source ZIP.

## Scope and privacy

- Only `drive.appdata` is requested. No full Drive access, no read access to other documents.
- App Data is hidden, **not end-to-end encrypted** by BetterSeed. Avoid saving highly sensitive secrets or passwords in habit titles.
- Cloud backup is not Play Billing purchase restoration or Google account data migration.
- Google OAuth permission can be revoked from Google Account permissions.

## Test plan

- [ ] `flutter pub get`, `flutter analyze`, `flutter test` and `flutter build apk --debug`.
- [ ] With Notifications master OFF, turn on an individual habit reminder: warning and Enable master option should appear; toggling on should request Android permission.
- [ ] Reject notification permission: save remains possible but shows paused state.
- [ ] Connect Google account (Cloud OAuth configured).
- [ ] Backup, change XP/habits, Restore: confirmation appears and data returns.
- [ ] Undo last cloud restore: local checkpoint returns.
- [ ] Change phones (same account), sign in and restore, verify date rollover and reminder master remains local.
- [ ] Disconnect Google: all local progress should remain.
- [ ] With internet off, Garden, Checklist, Shop, reminders and widget still work; cloud operations show a non-destructive error.
- [ ] Verify package version and identity unchanged across upgrades.
