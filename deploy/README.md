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
scp deploy/logs.sh root@8.148.83.188:/opt/ykt-backend/logs.sh
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
tail -f /opt/ykt-backend/logs/app.log            # 运行日志
```

本机（外网）也测一下：浏览器打开 `http://8.148.83.188:8080/api/v1/auth/qr` 能看到 JSON 即通。

### 5. 以后更新版本（常用）

```bash
# 本地：改完代码重新打包
cd backend && mvn -q -DskipTests clean package && cd ..
scp backend/target/ykt-backend-0.1.0-SNAPSHOT.jar root@8.148.83.188:/opt/ykt-backend/ykt-backend.jar
scp deploy/logs.sh root@8.148.83.188:/opt/ykt-backend/logs.sh   # 首次或脚本有更新时

# 服务器
systemctl restart ykt-backend
systemctl status ykt-backend
```

> ⚠️ **每次改后端代码都必须重新上传 jar 并 restart**，否则新接口（`/lessons/entry-cookie`、`?enhance`、`/render`、`/diagnostics`）会 404。
>
> ⚠️ 2026-11-24 起日志改为 logback 落盘（`logs/app.log` + `logs/audit.log`）。**老服务器要重跑一次 `bash /tmp/setup-ip-only.sh`**，它会重建 systemd 单元（`LOG_DIR=/opt/ykt-backend/logs`、`TZ=Asia/Shanghai`）并创建日志目录；只想改单元不重装 Java 的话，手动执行：

```bash
mkdir -p /opt/ykt-backend/logs && chmod 750 /opt/ykt-backend/logs
sed -i 's|StandardOutput=append:/opt/ykt-backend/app.log|Environment=LOG_DIR=/opt/ykt-backend/logs\nEnvironment=TZ=Asia/Shanghai\nStandardOutput=journal|; s|StandardError=append:/opt/ykt-backend/app.log|StandardError=journal|' /etc/systemd/system/ykt-backend.service
systemctl daemon-reload && systemctl restart ykt-backend
```

### 6. 日志在哪、怎么拿（不需要后端接口）

| 文件 | 内容 | 学号 |
|---|---|---|
| `/opt/ykt-backend/logs/app.log` | 运行日志 + 每请求一行访问记录 | 脱敏 `20****23` |
| `/opt/ykt-backend/logs/audit.log` | 登录 / 登出 / 进课堂审计 | **明文** |
| `journalctl -u ykt-backend` | 进程启动失败、崩溃栈 | — |

```bash
# 服务器上速查
grep LOGIN_OK /opt/ykt-backend/logs/audit.log | tail -20      # 最近 20 次登录，含学号
grep 'userId=2023012345' /opt/ykt-backend/logs/audit.log      # 某学号全部行为
tail -f /opt/ykt-backend/logs/app.log                          # 实时运行日志
grep -E ' (ERROR|WARN) ' /opt/ykt-backend/logs/app.log|tail -50

# 用脚本（已上传 logs.sh 时）
bash /opt/ykt-backend/logs.sh login 20
bash /opt/ykt-backend/logs.sh user 2023012345
bash /opt/ykt-backend/logs.sh pack        # 打包到 /tmp/ykt-logs-*.tar.gz

# 打包回本地（在本机项目根目录执行）
bash deploy/fetch-logs.sh            # → logs-backup/20261124-181630/{app.log,audit.log,...}
ONLY=audit bash deploy/fetch-logs.sh # 只拉审计日志（小，查学号够用）
```

日志自动按天 + 大小轮转并 gzip：app 保留 14 天、audit 保留 180 天，不会无限增长。

**拉到本地放在哪**：`scp ... .` 里的 `.` 就是"你执行命令时所在的当前目录"（不是项目目录、也不是下载文件夹）。
用 `bash deploy/fetch-logs.sh` 则固定落到 `D:/workspace/tools/YuketangAssistant/logs-backup/<时间戳>/`，
已加入 `.gitignore`（含学号明文，不进仓库）。想放到别处就加目标路径或改 `DEST_ROOT`：

```bash
scp root@8.148.83.188:/opt/ykt-backend/logs/audit.log D:/logs/audit.log
DEST_ROOT=D:/logs bash deploy/fetch-logs.sh
```

**三条命令各拉什么**：

| 命令 | 拉回的东西 | 落地形态 |
|---|---|---|
| `bash deploy/fetch-logs.sh` | `/opt/ykt-backend/logs/` **整个目录**：`app.log` + `audit.log` + 历史轮转包 `app.2026-11-23.0.log.gz`、`audit.*.log.gz` | 服务器先打 tar.gz → 下载 → 本地自动解开，目录里就是原始文件 |
| `ONLY=audit bash deploy/fetch-logs.sh` | 只有 `audit.log` 一个文件（+ 历史 `audit.*.gz`） | 同上，仍走 tar.gz 中转，本地解开后只有 audit 相关文件 |
| `scp root@...:/opt/ykt-backend/logs/audit.log .` | 只有**当前** `audit.log`，不含历史轮转包 | 单个纯文本文件，不解压，直接能打开 |

三种都只包含日志文本，不含 jar/配置/Cookie。

**怎么解析**（本地执行，`列` 已对齐）：

```bash
# 1) 谁登录了：按学号汇总（登录次数/首次/末次/渠道/IP）
bash deploy/parse-logs.sh logs-backup/20261124-181630/audit.log users

# 2) 某学号完整时间线（登录→进课堂→登出）
bash deploy/parse-logs.sh logs-backup/20261124-181630/audit.log user 2023012345

# 3) 全部事件明细（含每条 HTTP 状态/耗时）
bash deploy/parse-logs.sh logs-backup/20261124-181630/audit.log events

# 4) 接口健康度：次数/错误数/平均耗时/最大耗时/用户数
bash deploy/parse-logs.sh logs-backup/20261124-181630/app.log access

# 5) 历史轮转包直接读（.gz 会自动解，也可 gzip -dc 管道进来）
bash deploy/parse-logs.sh logs-backup/20261124-181630/audit.2026-11-23.0.log.gz users
```

不做加工也看得懂：每行是
`日期 时间 级别 [线程] AUDIT - 事件 键=值 键=值 ...`，
grep 单个学号、单个事件就能定位；要进 Excel 就把输出重定向 `> events.tsv` 再用 Excel 导入（制表符分隔）。

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
tail -50 /opt/ykt-backend/logs/app.log

# 5. 日志已落盘、权限正确（audit 含学号，只有 root 能读）
ls -l /opt/ykt-backend/logs/
```

前端登录后点「进入课堂」，应看到课件与题目；展开「验证诊断」显示「自渲染主方案不调用 checkin，没有记录是正常的」。
顺便在 `logs/audit.log` 里应该能看到刚才那条登录记录：`grep LOGIN_OK /opt/ykt-backend/logs/audit.log | tail -5`。

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
| 日志里学号是 `20****23` | 正常：app.log 脱敏。明文学号在 `logs/audit.log`（`grep LOGIN_OK`） |
| 没有 `logs/` 目录 / 日志还在老 `app.log` | 服务器 systemd 单元还是旧版，重跑 `bash /tmp/setup-ip-only.sh` 或在 README 一·5 用 sed 改单元后 `daemon-reload` |
| `journalctl` 里有日志但 `logs/app.log` 为空 | 进程工作目录不对（systemd 缺 `WorkingDirectory`/`LOG_DIR`），日志落到了别处；`ls -l /proc/$(pgrep -f ykt-backend)/cwd` 确认 |

---

## 七、正式发布（后续，非当前阶段）

1. 域名 + ICP 备案（10~20 天）
2. A 记录解析到服务器 IP
3. Nginx + Let's Encrypt 配 HTTPS（`deploy/setup-server.sh`）
4. 小程序后台配 request 合法域名；APK 打包地址改成 `https://`
5. 生成正式签名替换 debug 签名

> 当前 HTTP 明文传输 token/sessionid，仅个人调试可接受；正式使用务必上 HTTPS。

---

## 八、扫码后停在二维码页（2026-11-25 修复）

Symptoms：手机已扫码确认，二维码页不动 / 显示成功但不进首页。

**必须同时更新两端**（只更新一端仍可能卡住）：

1. 后端：`scp releases/ykt-backend-scheme4.jar root@<IP>:/opt/ykt-backend/ykt-backend.jar` → `systemctl restart ykt-backend`
2. 手机：覆盖安装 `releases/gongxiaohu-v1.0.apk`（修复了成功态跳转被丢弃的问题，并加了「进入首页」兜底按钮）

服务端日志判断（`grep -E '扫码轮询|收到登录成功凭据|换取 sessionid' logs/app.log`）：

| 日志 | 含义 |
|---|---|
| `扫码轮询暂未拿到凭据` | 微信侧还没确认；继续等即可 |
| `收到登录成功凭据` | 扫码确认已到后端，接下来换 sessionid |
| `换取 sessionid 失败，保留凭据供下次轮询重试` | 网络/官方接口抖动；新逻辑不会丢凭据，无需重新扫码 |
| `真实扫码登录成功` | 后端已完成；前端仍不动就点二维码页的「进入首页」 |

---

## 九、扫码后停住：真正的根因与最终改法（2026-11-25 09:10）

第八节那次修的是"凭据被丢弃"，但线上仍然出现 **后端 LOGIN_OK、手机停在二维码页**。
用用户提供的日志定位到了最后两个成因：

| 日志 | 真实含义 |
|---|---|
| `AUDIT - LOGIN_OK method=qrcode userId=...` | 后端全链路成功，token 已签发 |
| `ACCESS - POST /api/v1/auth/poll status=200 耗时=4ms` | **不是成功**：`BizException` 被包成 HTTP 200 + body `code=409`；4ms 表示网关里该 ticket 的流程已被 cleanup |
| 正常"等待确认"应是 `耗时≈45000ms` | 秒回只可能是"流程不存在"——成功那次响应没被前端消费 |

### 两个成因

1. **45 秒长轮询在移动端活不下来**：Android WebView / 运营商 NAT 会掐断空闲 30–40 秒的连接，
   服务端把成功响应写回已死连接，前端只看到网络错误。
2. **结果只发一次**：成功后立刻 cleanup，同一 ticket 再也取不回登录态。

### 改法（后端 + APP 都要更新）

- 后端 `POST /auth/poll` 支持 `{ticket, wait}`：`wait=0` 立即返回；**同一 ticket 重复轮询幂等返回同一 token**（缓存 10 分钟）。
- 前端每 1.2 秒短轮询一次，成功立即 `onLogin`；二维码下方显示「已检查 N 次 · 最近返回」。
- 旧客户端（小程序）不传 `wait`，仍走 45 秒长轮询，行为不变。

### 部署

```bash
cd D:/workspace/tools/YuketangAssistant
HOST=root@8.148.83.188
scp releases/ykt-backend-scheme4.jar "$HOST":/opt/ykt-backend/ykt-backend.jar \
  && ssh "$HOST" "systemctl restart ykt-backend && systemctl is-active ykt-backend"
```

手机覆盖安装 `releases/gongxiaohu-v1.0.apk`（09:13 重建，JS `main-Byktpd8l.js`），
登录页底部应显示 **「构建版本 2026-11-25 · 短轮询版（b2）」**——看不到就是没装上。

### 确认修好了

```bash
grep 扫码轮询 /opt/ykt-backend/logs/app.log | tail -5
```

- `扫码轮询暂未拿到凭据 ... 耗时=0ms`：正常等待，1.2 秒一条，可忽略；
- `扫码轮询成功 ... 耗时=xx ms`：后端已签发；页面此时应立刻进首页；
- `扫码轮询命中缓存`：前端重试取回了同一个 token——出现这条就说明"响应丢失"被兜住了。
