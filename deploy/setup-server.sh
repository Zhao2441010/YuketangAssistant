#!/usr/bin/env bash
# 阿里云 Ubuntu 一键部署脚本：Java 21 + Spring Boot 服务 + Nginx 反代 + Let's Encrypt HTTPS
# 用法：sudo bash setup-server.sh YOUR_DOMAIN
# 例：  sudo bash setup-server.sh api.example.com
set -euo pipefail

DOMAIN="${1:-}"
if [ -z "$DOMAIN" ]; then
  echo "用法: sudo bash setup-server.sh <域名>"; exit 1
fi

echo "==> 1/6 更新系统并安装依赖"
apt-get update -y
DEBIAN_FRONTEND=noninteractive apt-get install -y openjdk-21-jdk nginx certbot python3-certbot-nginx curl unzip

echo "==> 2/6 创建应用目录"
mkdir -p /opt/ykt-backend
# jar 由本地 scp 上传，脚本不覆盖已有 jar
if [ ! -f /opt/ykt-backend/ykt-backend.jar ]; then
  echo "错误：请先把 jar 上传到 /opt/ykt-backend/ykt-backend.jar"; exit 1
fi

echo "==> 3/6 创建 systemd 服务"
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
sleep 5
systemctl is-active ykt-backend || { echo "后端启动失败，查看日志:"; tail -50 /opt/ykt-backend/app.log; exit 1; }

echo "==> 4/6 配置 Nginx 反向代理"
cat > /etc/nginx/sites-available/ykt-backend <<EOF
server {
    listen 80;
    server_name ${DOMAIN};
    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_read_timeout 60s;
    }
}
EOF
ln -sf /etc/nginx/sites-available/ykt-backend /etc/nginx/sites-enabled/ykt-backend
rm -f /etc/nginx/sites-enabled/default
nginx -t && systemctl reload nginx

echo "==> 5/6 申请 Let's Encrypt SSL 证书"
certbot --nginx -d "${DOMAIN}" --non-interactive --agree-tos --email admin@${DOMAIN} --redirect

echo "==> 6/6 验证"
curl -sS -o /dev/null -w "HTTPS 状态码: %{http_code}\n" "https://${DOMAIN}/api/v1/auth/qr"
echo "完成！小程序 apiBase 填：https://${DOMAIN}/api/v1"
echo "后端状态: systemctl status ykt-backend"
echo "查看日志: tail -f /opt/ykt-backend/app.log"
