#!/usr/bin/env bash
set -euo pipefail

# Required Gradle version comes from the wrapper properties so the launcher
# always matches the version the project was built with.
GRADLE_VERSION="$(sed -n 's/^distributionUrl=.*gradle-\([0-9][0-9.]*\)-.*zip$/\1/p' \
  "$(dirname "$0")/gradle/wrapper/gradle-wrapper.properties" 2>/dev/null | head -1)"
GRADLE_VERSION="${GRADLE_VERSION:-8.10.2}"

# Locate a usable JDK. Preference order:
#   1. An existing, working JAVA_HOME from the environment
#   2. The original SDKMAN Java 21 install used when this project was set up
#   3. Any JDK found in /usr/lib/jvm (Debian/Ubuntu packages)
if [ -z "${JAVA_HOME:-}" ] || [ ! -x "$JAVA_HOME/bin/java" ]; then
  JAVA_HOME=""
  for candidate in "/usr/local/sdkman/candidates/java/21.0.12+1-ms" /usr/lib/jvm/*; do
    if [ -x "$candidate/bin/java" ]; then
      JAVA_HOME="$candidate"
      break
    fi
  done
fi
if [ -n "$JAVA_HOME" ]; then
  export JAVA_HOME
  export PATH="$JAVA_HOME/bin:$PATH"
fi

# Use an inherited GRADLE_HOME only when it actually provides the required
# version (CI images often predefine GRADLE_HOME for a different Gradle).
GRADLE_DIR="/tmp/gradle-$GRADLE_VERSION"
if [ -n "${GRADLE_HOME:-}" ] && [ -x "$GRADLE_HOME/bin/gradle" ]; then
  RUNNING_VERSION="$("$GRADLE_HOME/bin/gradle" --version 2>/dev/null | sed -n 's/^Gradle \(.*\)$/\1/p' | head -1)"
  if [ "$RUNNING_VERSION" = "$GRADLE_VERSION" ]; then
    GRADLE_DIR="$GRADLE_HOME"
  fi
fi

if [ ! -x "$GRADLE_DIR/bin/gradle" ]; then
  echo "Gradle $GRADLE_VERSION not found at $GRADLE_DIR. Downloading..."
  ZIP_PATH="$(mktemp -u /tmp/gradle-XXXXXX-bin.zip)"
  if command -v curl >/dev/null 2>&1; then
    curl -L --fail -o "$ZIP_PATH" "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"
  else
    wget -q -O "$ZIP_PATH" "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"
  fi
  mkdir -p "$(dirname "$GRADLE_DIR")"
  unzip -q -o "$ZIP_PATH" -d "$(dirname "$GRADLE_DIR")"
  rm -f "$ZIP_PATH"
fi

exec "$GRADLE_DIR/bin/gradle" "$@"
