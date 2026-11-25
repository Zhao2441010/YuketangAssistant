#!/usr/bin/env bash
# 本机（Windows Git Bash / Linux / macOS）执行：把服务器日志拉回本地 logs-backup/<时间戳>/
#
#   bash deploy/fetch-logs.sh             拉全部日志（app + audit，含历史轮转包）
#   ONLY=audit bash deploy/fetch-logs.sh  只拉审计日志（体积小，查"谁登录了"够用）
#   HOST=root@1.2.3.4 bash deploy/fetch-logs.sh
#
# 落盘位置：<项目根>/logs-backup/YYYYMMDD-HHMMSS/{app.log,audit.log,...}
set -euo pipefail

HOST="${HOST:-root@8.148.83.188}"
REMOTE_DIR="${REMOTE_DIR:-/opt/ykt-backend/logs}"
DEST_ROOT="${DEST_ROOT:-logs-backup}"
ONLY="${ONLY:-all}"
TS="$(date +%Y%m%d-%H%M%S)"
DEST="$DEST_ROOT/$TS"
REMOTE_PKG="/tmp/ykt-logs-$TS.tar.gz"

mkdir -p "$DEST"

echo "==> 1/3 服务器打包：$HOST:$REMOTE_DIR"
if [ "$ONLY" = "audit" ]; then
  ssh "$HOST" "tar -czf $REMOTE_PKG -C $REMOTE_DIR audit.log 2>/dev/null || tar -czf $REMOTE_PKG -C $REMOTE_DIR audit.log"
else
  ssh "$HOST" "tar -czf $REMOTE_PKG -C $REMOTE_DIR ."
fi

echo "==> 2/3 下载到 $DEST"
scp -q "$HOST:$REMOTE_PKG" "$DEST/"
ssh "$HOST" "rm -f $REMOTE_PKG" || true

echo "==> 3/3 本地解压"
tar -xzf "$DEST/$(basename "$REMOTE_PKG")" -C "$DEST"
rm -f "$DEST/$(basename "$REMOTE_PKG")"

echo "✓ 完成，日志目录："
echo "  $(cd "$DEST" && pwd)"
ls -lh "$DEST" | tail -n +2
echo
echo "常用查看："
echo "  grep LOGIN_OK \"$DEST/audit.log\" | tail -20"
