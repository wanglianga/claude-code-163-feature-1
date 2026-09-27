#!/bin/bash
# 启动前端（Vite 开发服务器，端口 5173，代理 /api 与 /uploads 到 8080）
cd "$(dirname "$0")/../frontend"
if [ ! -d node_modules ]; then
  npm install --cache /tmp/npm-cache
fi
npm run dev
