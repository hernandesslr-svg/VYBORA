#!/usr/bin/env bash
set -Eeuo pipefail
# Requires pg_dump, age, and optionally AWS CLI. Database dump is encrypted before it leaves this host.
: "${DATABASE_URL:?Set DATABASE_URL}"
: "${AGE_RECIPIENT:?Set AGE_RECIPIENT to the public age recipient key; never put a private key here}"
BACKUP_DIR="${BACKUP_DIR:-./backups}"
mkdir -p "$BACKUP_DIR"
chmod 700 "$BACKUP_DIR"
STAMP="$(date -u +%Y%m%dT%H%M%SZ)"
TMP="$BACKUP_DIR/vybora-$STAMP.dump"
ENC="$TMP.age"
trap 'rm -f "$TMP"' EXIT
pg_dump "$DATABASE_URL" --format=custom --no-owner --no-acl --file="$TMP"
age -r "$AGE_RECIPIENT" -o "$ENC" "$TMP"
sha256sum "$ENC" > "$ENC.sha256"
chmod 600 "$ENC" "$ENC.sha256"
echo "Backup criptografado criado: $ENC"
echo "Verifique o SHA-256 e teste restauração periodicamente em um banco isolado."
if [[ -n "${BACKUP_S3_URI:-}" ]]; then
  command -v aws >/dev/null || { echo 'AWS CLI não encontrado; backup local preservado.' >&2; exit 2; }
  aws s3 cp "$ENC" "$BACKUP_S3_URI/$(basename "$ENC")" --sse AES256
  aws s3 cp "$ENC.sha256" "$BACKUP_S3_URI/$(basename "$ENC.sha256")" --sse AES256
  echo "Cópia remota enviada para $BACKUP_S3_URI"
fi
