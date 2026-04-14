#!/usr/bin/env bash
#运行这个直接构建镜像
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT_DIR"

APP_IMAGE="my_blog_app:local"
FRONTEND_IMAGE="my_blog_frontend:local"

APP_TAR="$ROOT_DIR/my_blog_app_image.tar"
FRONTEND_TAR="$ROOT_DIR/my_blog_frontend_image.tar"

echo "Packaging backend jar..."
mvn -DskipTests clean package

echo "Building application images..."
docker build -f Dockerfile -t "$APP_IMAGE" .
docker build -f Dockerfile.frontend -t "$FRONTEND_IMAGE" .

echo "Saving images to project root..."1
docker save -o "$APP_TAR" "$APP_IMAGE"
docker save -o "$FRONTEND_TAR" "$FRONTEND_IMAGE"

echo "Done."
echo "Generated:"
echo "  - $APP_TAR"
echo "  - $FRONTEND_TAR"
