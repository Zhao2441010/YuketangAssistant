cd D:/workspace/tools/YuketangAssistant
HOST=root@8.148.83.188
scp releases/ykt-backend-scheme4.jar "$HOST":/opt/ykt-backend/ykt-backend.jar \
  && ssh "$HOST" "systemctl restart ykt-backend && systemctl is-active ykt-backend"