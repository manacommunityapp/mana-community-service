#!/bin/bash
# Startup script for mana-community-service on AWS Lightsail.
#
# Usage:
#   chmod +x scripts/start.sh
#   LOG_DIR=/var/log/mana-community SPRING_PROFILES_ACTIVE=prod ./scripts/start.sh
#
# Required env vars (set in Lightsail environment or .env):
#   DB_URL, DB_USERNAME, DB_PASSWORD, JWT_SECRET, CORS_ALLOWED_ORIGINS
#
# Optional:
#   LOG_DIR  — absolute path for all log files (default: /var/log/mana-community)
#   JAVA_OPTS — extra JVM flags (default: -Xms256m -Xmx512m)

set -euo pipefail

APP_DIR="$(cd "$(dirname "$0")/.." && pwd)"
LOG_DIR="${LOG_DIR:-/var/log/mana-community}"
JAVA_OPTS="${JAVA_OPTS:--Xms256m -Xmx512m}"
PROFILE="${SPRING_PROFILES_ACTIVE:-dev}"
JAR=$(find "$APP_DIR/target" -maxdepth 1 -name "*.jar" -not -name "*-sources.jar" | head -1)

if [ -z "$JAR" ]; then
  echo "ERROR: No JAR found in $APP_DIR/target. Run 'mvn package -DskipTests' first."
  exit 1
fi

# Ensure log directory exists and is writable
mkdir -p "$LOG_DIR/archive"
echo "Log directory: $LOG_DIR"

# Export LOG_DIR so logback-spring.xml picks it up
export LOG_DIR
export SPRING_PROFILES_ACTIVE="$PROFILE"

echo "Starting mana-community-service (profile=$PROFILE)..."
echo "JAR: $JAR"
echo "JVM: $JAVA_OPTS"

exec java $JAVA_OPTS \
  -DLOG_DIR="$LOG_DIR" \
  -jar "$JAR"
