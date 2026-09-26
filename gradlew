#!/usr/bin/env bash
set -euo pipefail

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

GRADLE_VERSION="8.10.2"
GRADLE_HOME="${GRADLE_HOME:-/tmp/gradle-$GRADLE_VERSION}"
if [ ! -x "$GRADLE_HOME/bin/gradle" ]; then
  echo "Gradle $GRADLE_VERSION not found at $GRADLE_HOME. Downloading..."
  ZIP_PATH="$(mktemp -u /tmp/gradle-XXXXXX-bin.zip)"
  if command -v curl >/dev/null 2>&1; then
    curl -L --fail -o "$ZIP_PATH" "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"
  else
    wget -q -O "$ZIP_PATH" "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip"
  fi
  mkdir -p "$(dirname "$GRADLE_HOME")"
  unzip -q -o "$ZIP_PATH" -d "$(dirname "$GRADLE_HOME")"
  rm -f "$ZIP_PATH"
fi

exec "$GRADLE_HOME/bin/gradle" "$@"
