#!/bin/bash
# 停止本地 PostgreSQL 集群
PGBIN=/usr/lib/postgresql/15/bin
PGDATA="$(cd "$(dirname "$0")/.." && pwd)/.pgdata"
if [ -d "$PGDATA" ]; then
  "$PGBIN/pg_ctl" -D "$PGDATA" stop
else
  echo "集群目录不存在: $PGDATA"
fi
