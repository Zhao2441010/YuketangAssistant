#!/usr/bin/env bash
# 日志速查工具（放到服务器 /opt/ykt-backend/logs.sh 使用）
#
#   bash logs.sh login 20     最近 20 次登录（学号明文）
#   bash logs.sh today        今天的审计日志
#   bash logs.sh user 2023012345   某学号的全部行为
#   bash logs.sh tail         跟随运行日志
#   bash logs.sh errors       最近的错误行
#   bash logs.sh journal 200  启动期/崩溃日志（logback 初始化前的输出）
#   bash logs.sh pack         打包全部日志到 /tmp，供 scp 下载
set -uo pipefail

LOG_DIR="${LOG_DIR:-/opt/ykt-backend/logs}"
AUDIT="$LOG_DIR/audit.log"
APP="$LOG_DIR/app.log"

case "${1:-help}" in
  login)
    n="${2:-20}"
    echo "== 最近 $n 次登录 =="
    grep -hE "LOGIN_(OK|FAIL)" "$AUDIT" 2>/dev/null | tail -n "$n"
    ;;
  today)
    d=$(date +%F)
    echo "== $d 审计日志 =="
    grep -h "^$d" "$AUDIT" 2>/dev/null | tail -n 200
    ;;
  user)
    [ -z "${2:-}" ] && { echo "用法: bash logs.sh user <学号>"; exit 1; }
    echo "== 学号 $2 的行为记录 =="
    grep -hE "userId=$2( |$)" "$AUDIT" 2>/dev/null | tail -n 200
    echo
    echo "== 该学号的访问记录（学号已脱敏，仅按时间参考） =="
    grep -hE "user=$(echo "$2" | cut -c1-2)\\*\\*\\*\\*" "$APP" 2>/dev/null | tail -n 50
    ;;
  tail)
    tail -n 100 -f "$APP"
    ;;
  errors)
    grep -hE " (ERROR|WARN) " "$APP" 2>/dev/null | tail -n 100
    ;;
  journal)
    journalctl -u ykt-backend -n "${2:-200}" --no-pager
    ;;
  pack)
    out="/tmp/ykt-logs-$(date +%Y%m%d-%H%M%S).tar.gz"
    tar -czf "$out" -C "$LOG_DIR" . 2>/dev/null
    chmod 600 "$out"
    echo "已打包：$out"
    ls -lh "$out"
    ;;
  *)
    sed -n '2,14p' "$0" | sed 's/^# \{0,1\}//'
    ;;
esac
