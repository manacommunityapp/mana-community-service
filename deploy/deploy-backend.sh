#!/usr/bin/env bash
# ==============================================================================
# Mana Community - Automated Deployment & Self-Healing Rollback Script
#
# Supports zero-downtime protection:
# 1. Backs up current running JAR before touching anything.
# 2. Deploys the candidate JAR.
# 3. Actively checks application health (systemd status & /actuator/health).
# 4. If new build fails: AUTOMATICALLY restores the previous working JAR,
#    restarts the service, logs the crash reason, and exits with code 1.
# ==============================================================================
set -eo pipefail

ENV_TYPE="${1:-prod}"
NEW_JAR_SRC="${2:-/tmp/target/*.jar}"

if [ "$ENV_TYPE" = "prod" ]; then
  SERVICE_NAME="backend"
  APP_DIR="/opt/mana-community/backend"
  LOG_DIR="/var/log/mana-community"
  ENV_FILE="/etc/mana-community/env"
  PORT=8081
else
  SERVICE_NAME="backend-dev"
  APP_DIR="/opt/mana-community/backend-dev"
  LOG_DIR="/var/log/mana-community-dev"
  ENV_FILE="/etc/mana-community/env-dev"
  PORT=8081
fi

ACTIVE_JAR="$APP_DIR/manacommunity.jar"
BACKUP_PREV="$APP_DIR/manacommunity.jar.prev"
BACKUP_DIR="$APP_DIR/backups"

echo "=================================================================="
echo "🚀 Starting Automated Deployment for [$SERVICE_NAME] ($ENV_TYPE)"
echo "   Timestamp: $(date '+%Y-%m-%d %H:%M:%S %Z')"
echo "=================================================================="

# 1. Resolve candidate JAR file
CANDIDATE_JAR=$(ls -1 $NEW_JAR_SRC 2>/dev/null | head -n 1 || true)
if [ -z "$CANDIDATE_JAR" ] || [ ! -f "$CANDIDATE_JAR" ]; then
  echo "❌ ERROR: No candidate JAR found matching: $NEW_JAR_SRC"
  exit 1
fi
echo "📦 Candidate JAR found: $CANDIDATE_JAR ($(du -h "$CANDIDATE_JAR" | cut -f1))"

# 2. Ensure target and log directories exist
sudo mkdir -p "$APP_DIR" "$BACKUP_DIR" "$LOG_DIR/archive"

# 3. Check environment file
if [ ! -f "$ENV_FILE" ]; then
  echo "⚠️ WARNING: Environment file $ENV_FILE does not exist!"
  echo "Ensure JASYPT_ENCRYPTOR_PASSWORD and required credentials are set."
fi

# 4. Backup existing working JAR
if [ -f "$ACTIVE_JAR" ]; then
  echo "💾 Backing up active working JAR to $BACKUP_PREV..."
  sudo cp -p "$ACTIVE_JAR" "$BACKUP_PREV"
  # Also store a timestamped copy in backups/ (keep last 5)
  TIMESTAMP=$(date '+%Y%m%d_%H%M%S')
  sudo cp -p "$ACTIVE_JAR" "$BACKUP_DIR/manacommunity-${TIMESTAMP}.jar"
  # Prune older backups keeping only the 5 most recent
  sudo find "$BACKUP_DIR" -type f -name "manacommunity-*.jar" | sort -r | tail -n +6 | xargs -r sudo rm -f
else
  echo "ℹ️ No existing active JAR found at $ACTIVE_JAR (first deployment)."
fi

# 5. Deploy new candidate JAR
echo "📥 Deploying candidate JAR to $ACTIVE_JAR..."
sudo cp "$CANDIDATE_JAR" "$ACTIVE_JAR"
sudo chmod 755 "$ACTIVE_JAR"

# 6. Restart the service
echo "🔄 Reloading systemd and restarting $SERVICE_NAME..."
sudo systemctl daemon-reload
sudo systemctl restart "$SERVICE_NAME"

# 7. Health Verification Loop
# Checks systemd service state and Actuator health endpoint
echo "⏳ Monitoring startup and health (timeout: 180s)..."
MAX_ATTEMPTS=60
SLEEP_INTERVAL=3
IS_HEALTHY=false

for attempt in $(seq 1 $MAX_ATTEMPTS); do
  sleep $SLEEP_INTERVAL
  ELAPSED=$((attempt * SLEEP_INTERVAL))

  # Check if systemd process died or entered failed state
  if ! sudo systemctl is-active --quiet "$SERVICE_NAME"; then
    echo "⚠️ Attempt $attempt/$MAX_ATTEMPTS (${ELAPSED}s): Service is NOT active (crashed or failed to start)!"
    break
  fi

  # Probe Spring Actuator health endpoint directly on port
  HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" --max-time 2 "http://localhost:${PORT}/actuator/health" 2>/dev/null || true)
  if [ "$HTTP_STATUS" != "200" ] && [ "$HTTP_STATUS" != "401" ] && [ "$HTTP_STATUS" != "403" ]; then
    # Fallback probe via context-prefixed path
    HTTP_STATUS=$(curl -s -o /dev/null -w "%{http_code}" --max-time 2 "http://localhost:${PORT}/api/actuator/health" 2>/dev/null || true)
  fi

  if [ "$HTTP_STATUS" = "200" ] || [ "$HTTP_STATUS" = "401" ] || [ "$HTTP_STATUS" = "403" ]; then
    echo "✅ Attempt $attempt/$MAX_ATTEMPTS (${ELAPSED}s): Service is healthy and responding (HTTP $HTTP_STATUS)!"
    IS_HEALTHY=true
    break
  fi

  if [ $((attempt % 5)) -eq 0 ]; then
    echo "   Still waiting for application to initialize... (${ELAPSED}s elapsed)"
  fi
done

# 8. Post-verification Evaluation & Auto-Rollback
if [ "$IS_HEALTHY" = true ]; then
  echo "=================================================================="
  echo "🎉 SUCCESS: Service [$SERVICE_NAME] is ACTIVE and HEALTHY!"
  echo "=================================================================="
  echo ""
  echo "=== Last 25 lines of application log ==="
  tail -n 25 "$LOG_DIR/mana-service.log" 2>/dev/null || true
  exit 0
else
  echo "=================================================================="
  echo "🚨 CRITICAL ERROR: Candidate build failed health verification!"
  echo "=================================================================="
  echo ""
  echo "=== Application Crash Logs (Last 60 lines) ==="
  tail -n 60 "$LOG_DIR/mana-service.log" 2>/dev/null || true
  echo ""
  echo "=== Systemd Service Status & Journal ==="
  sudo systemctl status "$SERVICE_NAME" --no-pager || true
  sudo journalctl -u "$SERVICE_NAME" -n 40 --no-pager || true

  # ── AUTOMATIC ROLLBACK ──────────────────────────────────────────────────────
  if [ -f "$BACKUP_PREV" ]; then
    echo ""
    echo "=================================================================="
    echo "⏪ ROLLING BACK: Restoring previous stable JAR ($BACKUP_PREV)..."
    echo "=================================================================="
    sudo cp -p "$BACKUP_PREV" "$ACTIVE_JAR"
    sudo systemctl daemon-reload
    sudo systemctl restart "$SERVICE_NAME"
    sleep 10

    if sudo systemctl is-active --quiet "$SERVICE_NAME"; then
      echo "✅ ROLLBACK COMPLETE: Service [$SERVICE_NAME] successfully restored to previous stable build!"
      echo "   The application is ONLINE and healthy on the fallback version."
    else
      echo "❌ CRITICAL: Rollback failed to restore previous version!"
      sudo systemctl status "$SERVICE_NAME" --no-pager || true
    fi
  else
    echo "⚠️ ROLLBACK SKIPPED: No previous backup JAR ($BACKUP_PREV) was found."
  fi

  echo ""
  echo "❌ Exiting with error code 1 to fail the CI deployment job."
  exit 1
fi
