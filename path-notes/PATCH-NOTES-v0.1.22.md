# NihonGo Native v0.1.22 — Credit Purchase and History Web Parity

Base: verified **v0.1.21 Global Loading** ZIP. Scope: Beli Kredit, Riwayat Kredit & XP, and payment detail required for safe checkout. Nothing pushed to GitHub.

## Web references and parity

- `client/src/screens/CreditTopupScreen.jsx`: wallet banner, responsive two-column packages (single-column below 370dp), Premium Materi, pending gateway/maintenance, estimated QRIS fee confirmation, last five orders, order-ID lookup, link to history.
- `client/src/screens/CreditHistoryScreen.jsx`: wallet/XP/record counters, all/credit/xp filters, today/7d/30d/custom/all date selectors, 20 item pagination, authoritative ledger directions and localized messages, latest orders linking to payment.
- `client/src/screens/CreditPaymentScreen.jsx`: order detail (including Premium Materi success), QRIS status, 5-second foreground refresh, expiry countdown, QR image with fallback QR generation from **server-provided QR string**, safe save to Android file picker, copy ID, manual status refresh, cancel confirmation, new order link.

## Security and lifecycle

- All pricing, balance, ledger, premium membership, QRIS strings, and order state come from existing authenticated API routes. Never simulate successful payment or credit top-ups.
- Payment POST is only triggered on confirmed user action; no background order creation. QR is fetched from authenticated first-party proxy, never sends authorization token to payment-provider hosts.
- Payment status refresh only occurs while active and pending; all networking is off the main thread.
- QR fallback uses ZXing to render the **exact payment string returned by the server**. No fake order details.
- Manual order lookup and cancellation require explicit action; cancellation has a second confirmation.
- Existing Home, Sensei animations, Missions, Settings, Login, GlobalLoading were kept from v0.1.21. Native UI only; no WebView.

## Validation checklist for GitHub Actions / device

1. GitHub Actions `assembleDebug` succeeds; install and authenticate with an existing test account.
2. Open Reward Center > Beli Kredit. Compare wallet balance and all packages with website, test narrow screens / scroll and maintenance mode.
3. Press a package: confirm price, estimated fee, total. **Do not trigger actual payment unless intending to purchase.**
4. Confirm purchase on a disposable test order, verify QRIS image or QR string fallback appears and can be scanned by separate banking app. Check order ID and status; do not claim success locally.
5. Open Riwayat, test filters and date selections, XP and credit in/out formatting, 20-item pagination and order links.
6. Test order status polling, QR save, copy ID, cancel confirmation, expired/success states. Leave app background and return, verify refresh.
7. Check Home animation and scroll remain unchanged.

**Not verified here:** Android SDK is unavailable; APK compilation and payment-gateway integration on a real device must be verified in Actions and with server sandbox/test environment. Do not send real money based on an untested build.

Additional parity: refresh/reset icon in history header, server-clamped pagination display, compact-screen payment controls, countdown hh:mm:ss when needed, and reduced QR bitmap memory.
