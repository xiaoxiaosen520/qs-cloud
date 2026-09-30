#!/usr/bin/env bash
# 整库重置 qs_takeout：逐表 DROP 后重建，清空全部业务数据
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
SQL="$ROOT/mvp/sql/database.sql"
CONTAINER="${QS_MYSQL_CONTAINER:-mysql-old-data}"
USER="${QS_MYSQL_USER:-root}"
PASS="${QS_MYSQL_PASSWORD:-root}"

if [[ ! -f "$SQL" ]]; then
  echo "找不到 $SQL" >&2
  exit 1
fi

echo "即将导入 $SQL（容器=$CONTAINER，会清空 qs_takeout 业务表）"
docker exec -i "$CONTAINER" mysql -u"$USER" -p"$PASS" < "$SQL"
echo "完成：仅保留种子配置（类目/轮播/优惠券/管理员占位）"
echo "启动 API 后管理员为 admin / 123456"
