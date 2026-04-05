#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT_DIR"

APP_IMAGE="my_blog_app:local"
FRONTEND_IMAGE="my_blog_frontend:local"
POSTGRES_IMAGE="postgres:16-alpine"

APP_TAR="$ROOT_DIR/my_blog_app_image.tar"
FRONTEND_TAR="$ROOT_DIR/my_blog_frontend_image.tar"
POSTGRES_TAR="$ROOT_DIR/my_blog_postgres_image.tar"

echo "Packaging backend jar..."
./mvnw -DskipTests clean package

echo "Building application images..."
docker build -f Dockerfile -t "$APP_IMAGE" .
docker build -f Dockerfile.frontend -t "$FRONTEND_IMAGE" .

echo "Pulling postgres base image..."
docker pull "$POSTGRES_IMAGE"

echo "Saving images to project root..."
docker save -o "$APP_TAR" "$APP_IMAGE"
docker save -o "$FRONTEND_TAR" "$FRONTEND_IMAGE"
docker save -o "$POSTGRES_TAR" "$POSTGRES_IMAGE"

echo "Done."
echo "Generated:"
echo "  - $APP_TAR"
echo "  - $FRONTEND_TAR"
echo "  - $POSTGRES_TAR"
