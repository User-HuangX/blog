#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT_DIR"

if [[ -f ".env.db.dev" ]]; then
    set -a
    source ".env.db.dev"
    set +a
fi

: "${DEV_DB_NAME:=my_blog_dev}"
: "${DEV_DB_USER:=my_blog_dev}"
: "${DEV_DB_PASSWORD:=my_blog_dev}"
: "${DEV_DB_PORT:=5433}"
: "${DEV_DB_DATA_DIR:=${HOME}/blog/data/pgsql-dev}"

export DEV_DB_NAME DEV_DB_USER DEV_DB_PASSWORD DEV_DB_PORT DEV_DB_DATA_DIR

mkdir -p "${DEV_DB_DATA_DIR}"

docker compose -f docker-compose.db.dev.yml up -d

echo "Dev environment is ready (PostgreSQL only)."
echo "Host: localhost"
echo "Port: ${DEV_DB_PORT}"
echo "Database: ${DEV_DB_NAME}"
echo "Username: ${DEV_DB_USER}"
echo "Password: ${DEV_DB_PASSWORD}"
echo "JDBC URL: jdbc:postgresql://localhost:${DEV_DB_PORT}/${DEV_DB_NAME}"
echo "Data dir: ${DEV_DB_DATA_DIR}"
