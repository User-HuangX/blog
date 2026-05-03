#!/usr/bin/env bash
# 为 Docker Engine 配置 registry-mirrors（拉取镜像加速），写入 /etc/docker/daemon.json
# 需要 root；执行后建议: sudo systemctl restart docker
#
# 用法:
#   sudo ./scripts/setup-docker-mirror.sh
#   sudo REGISTRY_MIRRORS="https://a,https://b" ./scripts/setup-docker-mirror.sh
#
# 阿里云等控制台会给出专属加速地址，可替换为逗号分隔的多条 URL。
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

if [[ "${EUID:-$(id -u)}" -ne 0 ]]; then
    exec sudo -E env "REGISTRY_MIRRORS=${REGISTRY_MIRRORS:-}" bash "$SCRIPT_DIR/setup-docker-mirror.sh" "$@"
fi

# 默认：DaoCloud 公共镜像；可自行改为阿里云专属地址等
DEFAULT_MIRRORS="https://docker.m.daocloud.io"
export REGISTRY_MIRRORS="${REGISTRY_MIRRORS:-$DEFAULT_MIRRORS}"

python3 - <<'PY'
import json
import os
import time
from pathlib import Path

path = Path("/etc/docker/daemon.json")
raw = os.environ.get("REGISTRY_MIRRORS", "")
mirrors = [m.strip() for m in raw.split(",") if m.strip()]
if not mirrors:
    raise SystemExit("错误: REGISTRY_MIRRORS 为空")

data = {}
if path.exists():
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as e:
        raise SystemExit(f"错误: 无法解析 {path}: {e}") from e

data["registry-mirrors"] = mirrors
path.parent.mkdir(parents=True, exist_ok=True)
if path.exists():
    bak = path.parent / f"{path.name}.bak.{int(time.time())}"
    bak.write_bytes(path.read_bytes())
    print(f">>> 已备份: {bak}")

text = json.dumps(data, indent=2, ensure_ascii=False) + "\n"
path.write_text(text, encoding="utf-8")
print(f">>> 已写入: {path}")
print(">>> registry-mirrors:")
for m in mirrors:
    print(f"    - {m}")
PY

if command -v systemctl >/dev/null 2>&1 && systemctl is-system-running >/dev/null 2>&1; then
    if systemctl is-active --quiet docker 2>/dev/null; then
        echo ">>> 重启 docker 服务..."
        systemctl restart docker
    else
        echo ">>> 提示: docker 未在运行，请稍后执行: sudo systemctl restart docker"
    fi
else
    echo ">>> 提示: 请手动重启 Docker（例如: sudo systemctl restart docker）"
fi
