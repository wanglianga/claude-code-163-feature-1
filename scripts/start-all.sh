#!/bin/bash
# 一键启动：数据库 + 后端 + 前端（后台运行，日志在 /tmp）
ROOT="$(cd "$(dirname "$0")/.." && pwd)"

bash "$ROOT/scripts/db-start.sh"

echo "[app] 启动后端 (http://localhost:8080) ..."
cd "$ROOT/backend" && nohup mvn spring-boot:run > /tmp/manhole-backend.log 2>&1 &

echo "[app] 启动前端 (http://localhost:5173) ..."
cd "$ROOT/frontend"
[ -d node_modules ] || npm install --cache /tmp/npm-cache
nohup npm run dev > /tmp/manhole-frontend.log 2>&1 &

echo "[app] 等待后端就绪 ..."
for i in $(seq 1 60); do
  sleep 3
  if curl -s -o /dev/null -w "%{http_code}" -X POST http://localhost:8080/api/auth/login \
      -H 'Content-Type: application/json' -d '{"username":"admin","password":"admin123"}' | grep -q 200; then
    echo "[app] 后端就绪"
    break
  fi
done
echo "[app] 完成。前端: http://localhost:5173  后端: http://localhost:8080"
echo "[app] 日志: /tmp/manhole-backend.log, /tmp/manhole-frontend.log"
