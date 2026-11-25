cd D:/workspace/tools/YuketangAssistant
scp releases/ykt-backend-scheme4.jar root@8.148.83.188:/opt/ykt-backend/ykt-backend.jar
ssh root@8.148.83.188 "systemctl restart ykt-backend && systemctl is-active ykt-backend"
