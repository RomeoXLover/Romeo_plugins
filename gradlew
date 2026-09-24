#!/usr/bin/env bash
set -euo pipefail

export JAVA_HOME="/usr/local/sdkman/candidates/java/21.0.12+1-ms"
export PATH="$JAVA_HOME/bin:$PATH"

GRADLE_HOME="/tmp/gradle-8.10.2"
if [ ! -x "$GRADLE_HOME/bin/gradle" ]; then
  echo "Gradle 8.10.2 is not available at $GRADLE_HOME. Please download it first:"
  echo "  cd /tmp && curl -L --fail -o gradle-8.10.2-bin.zip https://services.gradle.org/distributions/gradle-8.10.2-bin.zip && unzip -q -o gradle-8.10.2-bin.zip"
  exit 1
fi

exec "$GRADLE_HOME/bin/gradle" "$@"
