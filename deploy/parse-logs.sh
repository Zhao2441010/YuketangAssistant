#!/usr/bin/env bash
# 日志解析：把 audit.log / app.log（或 .gz）转成人能看的表
#
#   bash deploy/parse-logs.sh logs-backup/xxx/audit.log events           全部事件明细（默认）
#   bash deploy/parse-logs.sh logs-backup/xxx/audit.log users            按学号汇总：登录次数/首次/末次/渠道/IP
#   bash deploy/parse-logs.sh logs-backup/xxx/audit.log user 2023012345  某学号时间线
#   bash deploy/parse-logs.sh logs-backup/xxx/app.log  access            接口访问汇总：次数/错误/平均耗时
#   gzip -dc audit.2026-11-23.log.gz | bash deploy/parse-logs.sh - users 直接读压缩包（或传 .gz 路径也行）
#
# 输出为 TSV；本机有 column 时自动对齐成表格，可直接复制到 Excel。
set -uo pipefail

INPUT="${1:-}"
MODE="${2:-events}"
ARG="${3:-}"

if [ -z "$INPUT" ]; then
  sed -n '2,12p' "$0" | sed 's/^# \{0,1\}//'
  exit 0
fi

read_input() {
  if [ "$INPUT" = "-" ]; then
    cat
  elif [ "${INPUT##*.}" = "gz" ]; then
    gzip -dc "$INPUT"
  else
    cat "$INPUT"
  fi
}

read_input | awk -v mode="$MODE" -v arg="$ARG" '
function kv(s, k,   p, v) {
  p = index(s, k "=")
  if (p == 0) return "-"
  v = substr(s, p + length(k) + 1)
  sub(/[ \t].*/, "", v)
  if (v == "") v = "-"
  return v
}
function event_of(s,   p) {
  p = index(s, "AUDIT - ")
  if (p > 0) { s = substr(s, p + 8); split(s, a, " "); return a[1] }
  p = index(s, "ACCESS - ")
  if (p > 0) return "HTTP"
  return "-"
}
function after_client(s,   p) {
  p = index(s, "client=")
  if (p == 0) return "-"
  s = substr(s, p + 7)
  gsub(/[ \t\r\n]+$/, "", s)
  return s == "" ? "-" : s
}
function path_of(s,   m) {
  if (match(s, /(GET|POST|PUT|DELETE|OPTIONS|HEAD)[ ]+\/api[^ ]*/)) {
    m = substr(s, RSTART, RLENGTH); sub(/^[A-Z]+ +/, "", m); return m
  }
  return "-"
}
function cost_of(s,   m) {
  if (match(s, /耗时=[0-9]+ms/)) { m = substr(s, RSTART + 3, RLENGTH - 5); return m + 0 }
  return 0
}

/ (AUDIT|ACCESS) - / {
  ts = $1 " " $2
  ev = event_of($0)
  u  = kv($0, "userId")
  ip = kv($0, "ip")

  if (ev == "HTTP") {
    st = kv($0, "status")
    p  = path_of($0)
    ms = cost_of($0)
    au = kv($0, "user")          # 访问日志用的是 user=（已脱敏），不是 userId=
    cnt[p]++; sum[p] += ms
    if (st ~ /^[45]/) err[p]++
    if (ms > mx[p]) mx[p] = ms
    if (au != "-") seen[p, au] = 1
    if (mode == "events")
      printf "%s\tHTTP %s\t%s\t%s\t%s\t%s\t%s\n", ts, st, au, p, ms "ms", ip, "-"
    next
  }

  if (mode == "events" || (mode == "user" && (u == arg || kv($0,"account") ~ substr(arg,1,4)))) {
    res = kv($0, "credential")
    if (res == "-") res = kv($0, "reason")
    if (res == "-") res = "ok"
    printf "%s\t%s\t%s\t%s\t%s\t%s\t%s\n", ts, ev, u, kv($0, "method"), res, ip, after_client($0)
  }

  if (mode == "users" && ev == "LOGIN_OK") {
    if (!(u in first)) first[u] = ts
    last[u] = ts; times[u]++
    m = kv($0, "method")
    if (m != "-" && index(methods[u], m) == 0)
      methods[u] = (methods[u] == "" ? m : methods[u] "," m)
    ips[u] = ip
  }
  if (mode == "users" && (ev == "ENTER_LESSON" || ev == "LOGOUT")) {
    if (u != "-" && (u in first)) { last[u] = ts; if (ips[u] == "-") ips[u] = ip }
  }
}

END {
  if (mode == "users") {
    print "学号\t登录次数\t首次登录\t末次活动\t渠道\tIP"
    for (u in first)
      printf "%s\t%d\t%s\t%s\t%s\t%s\n", u, times[u], first[u], last[u], methods[u], ips[u]
  }
  if (mode == "access") {
    print "路径\t次数\t错误数\t平均耗时ms\t最大耗时ms\t用户数"
    for (p in cnt) {
      n = 0
      for (k in seen) { split(k, kk, SUBSEP); if (kk[1] == p) n++ }
      printf "%s\t%d\t%d\t%d\t%d\t%d\n", p, cnt[p], err[p] + 0, sum[p] / cnt[p], mx[p], n
    }
  }
}
' | {
  if [ "$MODE" = "users" ]; then
    { read -r h; printf '%s\n' "$h"; sort -t "$(printf '\t')" -k2,2nr; }
  elif [ "$MODE" = "access" ]; then
    { read -r h; printf '%s\n' "$h"; sort -t "$(printf '\t')" -k2,2nr; }
  else
    cat
  fi
} | {
  if command -v column >/dev/null 2>&1; then column -ts "$(printf '\t')"; else cat; fi
}
