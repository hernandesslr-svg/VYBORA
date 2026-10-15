#!/usr/bin/env sh
set -eu
GRADLE_VERSION="8.9"
if command -v gradle >/dev/null 2>&1; then
  VERSION="$(gradle --version 2>/dev/null | grep -m1 '^Gradle ' || true)"
  if [ "$VERSION" = "Gradle $GRADLE_VERSION" ]; then
    exec gradle "$@"
  fi
fi
GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"
DIST_DIR="$GRADLE_USER_HOME/qylara-wrapper/gradle-$GRADLE_VERSION"
GRADLE_BIN="$DIST_DIR/bin/gradle"
if [ ! -x "$GRADLE_BIN" ]; then
  mkdir -p "$GRADLE_USER_HOME/qylara-wrapper"
  ZIP="$GRADLE_USER_HOME/qylara-wrapper/gradle-$GRADLE_VERSION-bin.zip"
  if command -v curl >/dev/null 2>&1; then
    curl -fL --retry 3 "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" -o "$ZIP"
  elif command -v wget >/dev/null 2>&1; then
    wget -O "$ZIP" "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"
  else
    echo "ERROR: curl or wget is required to download Gradle $GRADLE_VERSION." >&2
    exit 1
  fi
  command -v unzip >/dev/null 2>&1 || { echo "ERROR: unzip is required." >&2; exit 1; }
  unzip -q -o "$ZIP" -d "$GRADLE_USER_HOME/qylara-wrapper"
  rm -f "$ZIP"
fi
exec "$GRADLE_BIN" "$@"
