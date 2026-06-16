#!/usr/bin/env bash
set -euo pipefail

POSTGRES_USER="${POSTGRES_USER:-postgres}"
POSTGRES_PASSWORD="${POSTGRES_PASSWORD:-}"
APP_DB="${APP_DB:-examdb}"
APP_USER="${APP_USER:-ksu}"
APP_PASSWORD="${APP_PASSWORD:-}"

if [ -n "$POSTGRES_PASSWORD" ]; then
  export PGPASSWORD="$POSTGRES_PASSWORD"
fi

if [ -z "$APP_PASSWORD" ]; then
  APP_PASSWORD="$(LC_ALL=C tr -dc 'A-Za-z0-9' < /dev/urandom | head -c 32 || true)"
fi

psql -U "$POSTGRES_USER" -d postgres -v ON_ERROR_STOP=1 -c "DO \$\$ BEGIN IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = '$APP_USER') THEN CREATE ROLE $APP_USER LOGIN PASSWORD '$APP_PASSWORD'; ELSE ALTER ROLE $APP_USER WITH LOGIN PASSWORD '$APP_PASSWORD'; END IF; END \$\$;"

if ! psql -U "$POSTGRES_USER" -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='$APP_DB';" | grep -q 1; then
  createdb -U "$POSTGRES_USER" -O "$APP_USER" "$APP_DB"
fi

psql -U "$POSTGRES_USER" -d postgres -v ON_ERROR_STOP=1 -c "GRANT ALL PRIVILEGES ON DATABASE $APP_DB TO $APP_USER;"

export PGPASSWORD="$APP_PASSWORD"
psql -U "$APP_USER" -d "$APP_DB" -v ON_ERROR_STOP=1 -c "SELECT current_database(), current_user;"

cat > .env <<EOF
spring.datasource.username=$APP_USER
spring.datasource.password=$APP_PASSWORD
EOF

echo "Local .env has been written for Spring Boot."
