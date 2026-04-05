#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$ROOT_DIR"

if [[ ! -d "node_modules" ]]; then
    npm install
fi

echo "Starting frontend dev server..."
echo "URL: http://localhost:5173"
npm run dev -- --host 0.0.0.0 --port 5173
