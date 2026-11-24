# 部署指南 · 后端自渲染方案（2026-11-24）

> 当前方案：后端仅凭 L1 登录态（sessionid）走 `lesson-summary` 拉课件与题目，前端自绘。
> **不调用 checkin、不依赖 pptJwt、不经过动态码签到**。
> 只有「提交随堂答案」需要本人在雨课堂扫码确认，看课件/看题不受签到状态影响。

---

## 零、一句话说清要部署什么

| 产物 | 部署到哪 | 是否必须 |
|---|---|---|
| `releases/ykt-backend-scheme4.jar` | 阿里云服务器 8080 端口 | ✅ **必须**，所有端都依赖它 |
| `releases/gongxiaohu-v1.0.apk` | 手机直接安装 | 可选，安卓端 |
| `dist/`（Web 前端） | 任意静态托管 / Nginx | 可选，浏览器端 |
| `miniprogram/` | 微信开发者工具导入 | 可选，小程序端 |

**最简路径：只部署后端 jar + 装 APK 就能用。** 前端 Web / 小程序按需。

---

## 一、部署后端（必须）

### 1. 阿里云控制台

- ECS 安全组放行 **8080 / TCP**（来源 `0.0.0.0/0`）
- 记下公网 IP（本文档示例用 `8.148.83.188`，请替换成你的）

### 2. 本地打包并上传（Windows / Git Bash）

```bash
cd D:/workspace/tools/YuketangAssistant

# 打包（若已有 releases/ykt-backend-scheme4.jar 可跳过）
cd backend && "/d/app/idea/IntelliJ IDEA 2026.1/plugins/maven/lib/maven3/bin/mvn.cmd" -q -DskipTests clean package && cd ..
cp backend/target/ykt-backend-0.1.0-SNAPSHOT.jar releases/ykt-backend-scheme4.jar

# 上传 jar + 部署脚本
scp releases/ykt-backend-scheme4.jar root@8.148.83.188:/opt/ykt-backend/ykt-backend.jar
scp deploy/setup-ip-only.sh root@8.148.83.188:/tmp/setup-ip-only.sh
```

> 首次 `scp` 会问 `Are you sure...` → 输 `yes`。用密钥登录加 `-i 你的私钥路径`。
> 若 `/opt/ykt-backend` 目录不存在，先 `ssh root@8.148.83.188 "mkdir -p /opt/ykt-backend"`。

### 3. 服务器执行

```bash
ssh root@8.148.83.188
bash /tmp/setup-ip-only.sh          # 首次部署：装 Java 21 + 建 systemd 服务 + 启动
```

脚本做的事：安装 `openjdk-21-jdk` → 创建 `/etc/systemd/system/ykt-backend.service`（开机自启、崩溃重启）→ 启动 → 探活。

### 4. 验证

```bash
systemctl status ykt-backend
curl http://127.0.0.1:8080/api/v1/auth/qr        # 应返回 JSON，含 qrDataUrl
tail -f /opt/ykt-backend/app.log
```

本机（外网）也测一下：浏览器打开 `http://8.148.83.188:8080/api/v1/auth/qr` 能看到 JSON 即通。

### 5. 以后更新版本（常用）

```bash
# 本地：改完代码重新打包
cd backend && mvn -q -DskipTests clean package && cd ..
scp backend/target/ykt-backend-0.1.0-SNAPSHOT.jar root@8.148.83.188:/opt/ykt-backend/ykt-backend.jar

# 服务器
systemctl restart ykt-backend
systemctl status ykt-backend
```

> ⚠️ **每次改后端代码都必须重新上传 jar 并 restart**，否则新接口（`/lessons/entry-cookie`、`?enhance`、`/render`、`/diagnostics`）会 404。

---

## 二、部署 APK（可选）

APK 里已写死后端地址 `http://8.148.83.188:8080/api/v1`（打包时由 `VITE_API_BASE_URL` 注入），**只要后端部署好就能用**，无需额外配置。

1. 把 `releases/gongxiaohu-v1.0.apk` 传到手机，覆盖安装
2. 打开 → 微信扫码登录（或账密 / Cookie 导入）
3. 主页「进入课堂」→ 后端自渲染课件与题目

**如果后端 IP 变了**，必须重新打包 APK：

```bash
cd D:/workspace/tools/YuketangAssistant
VITE_API_BASE_URL="http://新IP:8080/api/v1" pnpm build
cp -r dist/. android/app/src/main/assets/public/
cd android
export ANDROID_HOME="/d/app/AndroidSdk"
export ANDROID_SDK_ROOT="$ANDROID_HOME"
./gradlew assembleDebug --no-daemon
cd ..
cp android/app/build/outputs/apk/debug/app-debug.apk releases/gongxiaohu-v1.0.apk
```

> Debug 签名仅可安装，不可上架应用商店。

---

## 三、部署 Web 前端（可选）

```bash
cd D:/workspace/tools/YuketangAssistant
VITE_API_BASE_URL="http://8.148.83.188:8080/api/v1" pnpm build
```

把 `dist/` 整个目录丢到任意静态服务器（Nginx / 对象存储 / `python -m http.server`）。

**跨域说明**：后端 `WebConfig` 已配 CORS，Web 前端可直接调 `http://8.148.83.188:8080/api/v1`。
但 Web 端**无法给官方域注入 Cookie**，所以「用官方课堂页打开」按钮只在浏览器已登录雨课堂时有效；
主路径「后端自渲染」不受影响——这正是选这个方案的原因。

---

## 四、部署小程序（可选）

1. 微信开发者工具导入 `miniprogram/`
2. 登录页底部「⚙ 后端地址设置」填 `http://8.148.83.188:8080`（代码自动补 `/api/v1`）
3. 详情 → 本地设置 → 勾选 **「不校验合法域名、web-view、TLS 版本以及 HTTPS 证书」**
4. 预览 / 真机调试

小程序端同样以后端自渲染为主路径；官方课堂页链接走「复制到剪贴板，在浏览器打开」。

---

## 五、部署后自检清单

```bash
# 1. 服务活着
systemctl is-active ykt-backend

# 2. 二维码接口（无需登录）
curl -s http://127.0.0.1:8080/api/v1/auth/qr | head -c 200

# 3. 诊断接口需登录（未带 token 应返回 401，说明鉴权正常）
curl -s http://127.0.0.1:8080/api/v1/diagnostics

# 4. 日志无异常
tail -50 /opt/ykt-backend/app.log
```

前端登录后点「进入课堂」，应看到课件与题目；展开「验证诊断」显示「自渲染主方案不调用 checkin，没有记录是正常的」。

---

## 六、常见问题

| 现象 | 原因 / 处理 |
|---|---|
| 登录二维码不出来 | jar 没更新 / 服务没起来；`systemctl restart ykt-backend` 后看日志 |
| 点「进入课堂」报 404 | 服务器还是旧 jar，缺 `/lessons/{entryId}/render`；重新上传 + restart |
| 能登录但课件/题目为空 | 该节课 `lesson-summary` 尚未生成（进行中课堂可能未归档）；换历史课时试，或等下课 |
| 提交答案提示需扫码 | 符合预期：签到/提交需本人当场扫码，看课件看题不受影响 |
| 手机能连、浏览器连不上 | 安全组 8080 未放行，或本机防火墙 |
| 改了前端但 APK 没变 | 前端改完要 `cp -r dist/. android/app/src/main/assets/public/` 再 `gradlew assembleDebug` |

---

## 七、正式发布（后续，非当前阶段）

1. 域名 + ICP 备案（10~20 天）
2. A 记录解析到服务器 IP
3. Nginx + Let's Encrypt 配 HTTPS（`deploy/setup-server.sh`）
4. 小程序后台配 request 合法域名；APK 打包地址改成 `https://`
5. 生成正式签名替换 debug 签名

> 当前 HTTP 明文传输 token/sessionid，仅个人调试可接受；正式使用务必上 HTTPS。
