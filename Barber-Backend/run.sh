#!/bin/bash
# =============================================================================
# BarberCraft Express API Launcher
# =============================================================================

DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" >/dev/null 2>&1 && pwd)"
export NODE_PATH="$DIR/node_modules"

echo "Starting BarberCraft API Server..."
node "$DIR/server.js"
