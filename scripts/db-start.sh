#!/bin/bash
# 启动本地 PostgreSQL（用户级集群，端口 5433，数据库 manhole）
# 不依赖系统 postgres 服务与 root 权限。
set -e
PGBIN=/usr/lib/postgresql/15/bin
PGDATA="$(cd "$(dirname "$0")/.." && pwd)/.pgdata"
PGLOG="$(cd "$(dirname "$0")/.." && pwd)/.pgdata.log"
PORT=5433

if [ ! -d "$PGDATA" ]; then
  echo "[db] 初始化数据库集群: $PGDATA"
  "$PGBIN/initdb" -D "$PGDATA" -U postgres --auth=trust --encoding=UTF8
fi

if "$PGBIN/pg_ctl" -D "$PGDATA" status > /dev/null 2>&1; then
  echo "[db] PostgreSQL 已在运行"
else
  "$PGBIN/pg_ctl" -D "$PGDATA" -l "$PGLOG" -o "-p $PORT -k /tmp" start
fi

# 创建业务库（不存在时）
if ! psql -h 127.0.0.1 -p $PORT -U postgres -lqt | cut -d '|' -f 1 | grep -qw manhole; then
  echo "[db] 创建数据库 manhole"
  psql -h 127.0.0.1 -p $PORT -U postgres -c "CREATE DATABASE manhole;"
fi
echo "[db] 就绪: jdbc:postgresql://localhost:$PORT/manhole (user=postgres, 免密 trust)"
