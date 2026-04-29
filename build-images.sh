#!/usr/bin/env bash
# 一键构建镜像并部署到远程服务器（默认 hxdbk.top）
# 本地：SDKMAN Java → Maven 打包 → docker build → docker save
# 远程：scp 镜像 tar + compose → ssh docker load → docker compose up -d
#
# 环境变量：
#   DEPLOY_HOST          远程主机，默认 hxdbk.top
#   DEPLOY_USER          SSH 用户，默认 ubuntu（已配置密钥时直接 ssh）
#   DEPLOY_REMOTE_DIR    远程目录，未设置时使用远程用户家目录 $HOME
#   DEPLOY_LOCAL=1       仅本地构建并 docker compose up（不推远程）
#   SDKMAN_JAVA_IDENTIFIER / .sdkmanrc  同前
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT_DIR"

DEPLOY_HOST="${DEPLOY_HOST:-hxdbk.top}"
DEPLOY_USER="${DEPLOY_USER:-ubuntu}"
# 未设置时，在远程使用登录用户家目录（如 /home/ubuntu）
DEPLOY_REMOTE_DIR="${DEPLOY_REMOTE_DIR:-}"
SSH_TARGET="${DEPLOY_USER}@${DEPLOY_HOST}"

APP_IMAGE="my_blog_app:local"
FRONTEND_IMAGE="my_blog_frontend:local"
APP_TAR="my_blog_app_image.tar"
FRONTEND_TAR="my_blog_frontend_image.tar"

SDKMAN_INIT="${SDKMAN_DIR:-$HOME/.sdkman}/bin/sdkman-init.sh"
if [[ ! -s "$SDKMAN_INIT" ]]; then
    echo "错误: 未找到 SDKMAN（期望 \$SDKMAN_DIR 或 ~/.sdkman）。安装: https://sdkman.io/install" >&2
    exit 1
fi
# sdkman-init.sh 会引用 ZSH_VERSION 等仅在 zsh 下存在的变量；在 set -u 的 bash 中需临时关闭 nounset
set +u
# shellcheck disable=SC1091
source "$SDKMAN_INIT"

if [[ -n "${SDKMAN_JAVA_IDENTIFIER:-}" ]]; then
    echo ">>> SDKMAN: 使用 SDKMAN_JAVA_IDENTIFIER=$SDKMAN_JAVA_IDENTIFIER"
    sdk use java "$SDKMAN_JAVA_IDENTIFIER"
elif [[ -f "$ROOT_DIR/.sdkmanrc" ]]; then
    echo ">>> SDKMAN: 加载 $ROOT_DIR/.sdkmanrc"
    sdk env
else
    echo ">>> SDKMAN: 未找到 .sdkmanrc，尝试 Temurin 21（可按需 sdk install）"
    sdk use java 21.0.10-tem
fi
set -u

echo ">>> Java: $(command -v java)"
java -version 2>&1 | sed 's/^/    /'

if ! command -v docker >/dev/null 2>&1; then
    echo "错误: 未找到 docker，请先安装 Docker。" >&2
    exit 1
fi

COMPOSE=(docker compose)
if ! docker compose version >/dev/null 2>&1; then
    if command -v docker-compose >/dev/null 2>&1; then
        COMPOSE=(docker-compose)
    else
        echo "错误: 需要 Docker Compose（docker compose 或 docker-compose）。" >&2
        exit 1
    fi
fi

echo ">>> 打包后端 JAR..."
mvn -DskipTests clean package

echo ">>> 本地构建镜像..."
docker build -f Dockerfile -t "$APP_IMAGE" .
docker build -f Dockerfile.frontend -t "$FRONTEND_IMAGE" .

if [[ "${DEPLOY_LOCAL:-0}" == "1" ]]; then
    echo ">>> DEPLOY_LOCAL=1：仅在本地启动容器"
    "${COMPOSE[@]}" -f docker-compose.yml up -d --build
    echo ""
    "${COMPOSE[@]}" ps
    echo "访问: http://localhost/"
    exit 0
fi

echo ">>> 导出镜像为 tar..."
docker save -o "$ROOT_DIR/$APP_TAR" "$APP_IMAGE"
docker save -o "$ROOT_DIR/$FRONTEND_TAR" "$FRONTEND_IMAGE"

if ! command -v ssh >/dev/null 2>&1 || ! command -v scp >/dev/null 2>&1; then
    echo "错误: 需要 ssh 与 scp 才能部署到 $SSH_TARGET" >&2
    exit 1
fi

if [[ -n "$DEPLOY_REMOTE_DIR" ]]; then
    REMOTE_PATH="$DEPLOY_REMOTE_DIR"
else
    REMOTE_PATH=$(ssh -o ConnectTimeout=15 "$SSH_TARGET" 'printf %s "$HOME"')
fi

echo ">>> 上传 compose 与镜像到 $SSH_TARGET:$REMOTE_PATH ..."

ssh -o ConnectTimeout=15 "$SSH_TARGET" "mkdir -p $(printf '%q' "$REMOTE_PATH")"

scp -o ConnectTimeout=30 \
    "$ROOT_DIR/docker-compose.remote.yml" \
    "${SSH_TARGET}:${REMOTE_PATH}/docker-compose.yml"

if [[ -f "$ROOT_DIR/.env" ]]; then
    echo ">>> 上传 .env ..."
    scp -o ConnectTimeout=30 "$ROOT_DIR/.env" "${SSH_TARGET}:${REMOTE_PATH}/.env"
fi

echo ">>> 上传镜像（体积较大，请稍候）..."
scp -o ConnectTimeout=300 \
    "$ROOT_DIR/$APP_TAR" \
    "$ROOT_DIR/$FRONTEND_TAR" \
    "${SSH_TARGET}:${REMOTE_PATH}/"

echo ">>> 远程加载镜像并启动..."
ssh -o ConnectTimeout=120 "$SSH_TARGET" bash -s <<REMOTE_SCRIPT
set -euo pipefail
cd "$REMOTE_PATH"
docker load -i "$APP_TAR"
docker load -i "$FRONTEND_TAR"
if docker compose version >/dev/null 2>&1; then
  docker compose up -d --no-build
  docker compose ps
else
  docker-compose up -d --no-build
  docker-compose ps
fi
REMOTE_SCRIPT

echo ""
echo ">>> 远程部署完成: http://${DEPLOY_HOST}/"
echo "    SSH: ssh $SSH_TARGET"
echo "    目录: $REMOTE_PATH"
echo "    仅本地构建、不推远程时可设: DEPLOY_LOCAL=1 ./build-images.sh"
