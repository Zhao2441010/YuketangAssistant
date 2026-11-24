# 阿里云 Ubuntu 部署指南（只有公网 IP，无域名）

> 适用场景：先把后端部署起来，用 **HTTP + IP** 在微信开发者工具里调试小程序。
> 正式发布需要域名 + 备案 + HTTPS（见文末）。

## 一、阿里云控制台准备

1. **安全组放行端口**：进入 ECS 实例 → 安全组 → 添加规则，放行 **8080**（TCP，来源 0.0.0.0/0）
   - 若后续上 HTTPS，再放行 80/443
2. 记下实例的**公网 IP**（下面用 `<公网IP>` 代替）

## 二、本地上传（Windows）

在 Git Bash 或 PowerShell 里执行（假设 jar 已打包）：

```bash
cd D:/workspace/tools/YuketangAssistant

# 上传 jar 到服务器 /tmp
scp backend/target/ykt-backend-0.1.0-SNAPSHOT.jar root@<公网IP>:/tmp/ykt-backend.jar

# 上传部署脚本
scp deploy/setup-ip-only.sh root@<公网IP>:/tmp/setup-ip-only.sh
```

> 首次连接会提示 `Are you sure you want to continue connecting?` → 输入 `yes`
> 若用密钥登录：加 `-i 你的私钥路径`，例如 `scp -i ~/.ssh/id_rsa ...`

## 三、服务器部署

SSH 登录：

```bash
ssh root@<公网IP>
```

执行一键脚本：

```bash
bash /tmp/setup-ip-only.sh
```

脚本自动完成：安装 Java 21 → 创建 systemd 服务（开机自启/崩溃重启）→ 启动后端。

验证：

```bash
systemctl status ykt-backend          # 应显示 active (running)
curl http://127.0.0.1:8080/api/v1/auth/qr   # 应返回 JSON（含 qrDataUrl）
```

## 四、小程序连接

1. 打开小程序 → 登录页底部「⚙ 后端地址设置」→ 填入：

```
http://<公网IP>:8080
```

（末尾 `/api/v1` 可省略，代码自动补）

2. 微信开发者工具打开 `miniprogram/` → 详情 → 本地设置 → **勾选「不校验合法域名、web-view、TLS 版本以及 HTTPS 证书」**
3. 点「预览」或「真机调试」扫码，即可连上服务器后端

> ⚠️ 只有勾选了"不校验合法域名"才能用 IP；正式版必须走 HTTPS 域名。

## 五、常用运维

```bash
systemctl status ykt-backend          # 状态
systemctl restart ykt-backend         # 重启
tail -f /opt/ykt-backend/app.log      # 实时日志
```

更新部署（本地重新打包后）：

```bash
# 本地
cd backend && mvn -q -DskipTests clean package && cd ..
scp backend/target/ykt-backend-0.1.0-SNAPSHOT.jar root@<公网IP>:/opt/ykt-backend/ykt-backend.jar
# 服务器
systemctl restart ykt-backend
```

## 六、正式发布（后续）

要让小程序脱离开发者工具正式使用，需要：

1. 买域名（阿里云）并 **ICP 备案**（10~20 天）
2. 域名 A 记录解析到服务器公网 IP
3. 服务器装 Nginx + Let's Encrypt 免费 SSL 证书（`deploy/setup-server.sh` 已备好，传上去执行 `bash setup-server.sh api.你的域名.com`）
4. 小程序设置里把 `apiBase` 改成 `https://api.你的域名.com`
5. 微信公众平台 → 开发设置 → request 合法域名填 `https://api.你的域名.com`

## 附：安全组只放行 8080 的风险

HTTP 明文传输，token 和 sessionid 可能被嗅探。个人调试可接受，正式使用务必上 HTTPS。
