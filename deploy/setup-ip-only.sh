#!/usr/bin/env bash
# 阿里云 Ubuntu：仅用公网 IP + HTTP 部署后端（无域名）
# 用法：bash setup-ip-only.sh
set -euo pipefail

echo "==> 1/4 安装 Java 21"
apt-get update -y
DEBIAN_FRONTEND=noninteractive apt-get install -y openjdk-21-jdk curl

echo "==> 2/4 准备应用目录"
mkdir -p /opt/ykt-backend
if [ ! -f /opt/ykt-backend/ykt-backend.jar ]; then
  echo "错误：请先把 jar 上传到 /opt/ykt-backend/ykt-backend.jar"; exit 1
fi

echo "==> 3/4 创建 systemd 服务"
cat > /etc/systemd/system/ykt-backend.service <<'EOF'
[Unit]
Description=Yuketang Assistant Backend
After=network.target

[Service]
Type=simple
User=root
WorkingDirectory=/opt/ykt-backend
ExecStart=/usr/bin/java -jar /opt/ykt-backend/ykt-backend.jar --server.port=8080
Restart=always
RestartSec=10
StandardOutput=append:/opt/ykt-backend/app.log
StandardError=append:/opt/ykt-backend/app.log

[Install]
WantedBy=multi-user.target
EOF
systemctl daemon-reload
systemctl enable ykt-backend
systemctl restart ykt-backend

echo "==> 4/4 等待启动并验证"
for i in $(seq 1 20); do
  if curl -s -o /dev/null http://127.0.0.1:8080/api/v1/auth/qr; then break; fi
  sleep 1
done
if systemctl is-active ykt-backend >/dev/null; then
  echo "✓ 后端已启动"
  PUBLIC_IP=$(curl -s ifconfig.me || curl -s ipinfo.io/ip || echo '<公网IP>')
  echo "小程序后端地址填：http://${PUBLIC_IP}:8080"
  echo "记得在阿里云安全组放行 8080 端口！"
else
  echo "✗ 后端启动失败，日志："; tail -50 /opt/ykt-backend/app.log; exit 1
fi
