#!/usr/bin/env sh
# Small Gradle bootstrapper for environments where Gradle Wrapper JAR is not bundled.
# Downloads the pinned Gradle distribution once and validates its published SHA-256.
set -eu
VERSION="8.14.4"
DIST="gradle-${VERSION}-bin.zip"
BASE="https://services.gradle.org/distributions"
CACHE="${GRADLE_USER_HOME:-$HOME/.gradle}/nihongo-bootstrap"
INSTALL="$CACHE/gradle-${VERSION}"

if [ -x "$INSTALL/bin/gradle" ]; then
  exec "$INSTALL/bin/gradle" "$@"
fi
mkdir -p "$CACHE"
TMP="$CACHE/$DIST.tmp"
SUM="$CACHE/$DIST.sha256"
ZIP="$CACHE/$DIST"
command -v curl >/dev/null 2>&1 || { echo 'curl diperlukan untuk mengunduh Gradle.' >&2; exit 1; }
command -v unzip >/dev/null 2>&1 || { echo 'unzip diperlukan untuk mengekstrak Gradle.' >&2; exit 1; }
curl -fsSL "$BASE/$DIST" -o "$TMP"
curl -fsSL "$BASE/$DIST.sha256" -o "$SUM"
EXPECTED="$(tr -d '[:space:]' < "$SUM")"
ACTUAL="$(sha256sum "$TMP" | awk '{print $1}')"
[ "$EXPECTED" = "$ACTUAL" ] || { rm -f "$TMP"; echo 'Checksum Gradle tidak cocok; unduhan dibatalkan.' >&2; exit 1; }
mv "$TMP" "$ZIP"
mkdir -p "$INSTALL"
unzip -q "$ZIP" -d "$CACHE"
# unzip creates gradle-8.13 directly; keep INSTALL as the actual Gradle home.
if [ ! -x "$INSTALL/bin/gradle" ]; then
  echo 'Distribusi Gradle selesai diunduh tetapi executable tidak ditemukan.' >&2
  exit 1
fi
exec "$INSTALL/bin/gradle" "$@"
