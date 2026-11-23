# 工小湖后端 · 第一版

对应开发者博客技术路线：**扫码登录（45 秒长轮询校验）→ 免扫码秒进班 → 超时补答**。
基于 Java 21 / Spring Boot 3.4.5 / Maven，可用 IDEA 2026.1 直接导入。

---

## 1. 用 IDEA 导入并运行

1. 打开 IDEA → `File` → `Open` → 选择目录 `D:\workspace\tools\YuketangAssistant\backend`
2. IDEA 识别为 Maven 项目后，等待依赖下载完成（右下角进度条）
3. 定位到 `src/main/java/com/jiclub/gongxiaohu/GongXiaoHuApplication.java`，右键 `Run 'GongXiaoHuApplication'`
4. 服务启动在 `http://localhost:8080`

> 命令行方式（IDEA 自带 Maven）：
> `D:\app\idea\IntelliJ IDEA 2026.1\plugins\maven\lib\maven3\bin\mvn.cmd spring-boot:run`

默认 `gongxiaohu.mode: mock`，无需任何雨课堂账号或网络即可跑通全流程。

---

## 2. 已实现功能与接口

### 功能零：扫码登录（博客第 2 节）—— 用户端 → 后端 → 雨课堂端

| 接口 | 方法 | 说明 |
|---|---|---|
| `/api/v1/auth/qr` | GET | 获取二维码（ticket + data URL）；mock 模式本地生成二维码 |
| `/api/v1/auth/poll` | POST | **45 秒阻塞长轮询**：手机确认瞬间立即返回 token（二维码有效期 60 秒，留 15 秒缓冲） |
| `/api/v1/auth/mock-confirm` | POST | 仅 mock 模式：模拟手机端点击「确认登录」 |
| `/api/v1/auth/session` | GET | 查询本地 token 是否有效（前端刷新时校验） |
| `/api/v1/auth/logout` | POST | 退出登录，token 立即失效 |
| `/api/v1/auth/mock-login` | POST | 演示用一步登录（建票据→模拟确认→返回 token） |
| `/api/v1/auth/import-session` | POST | 真实模式：用已有 sessionid 直接导入会话（免扫码），body `{"sessionId":"...","userId":"..."}` |
| `/api/v1/auth/renew` | POST | 主动续期：延长本地会话有效期（剩余不足 1 小时也会在业务请求时自动滑动续期） |
| `/api/v1/auth/password-login` | POST | 账号密码登录：body `{account,password,type,ticket,randstr}`，type=PP手机号/E邮箱；密码后端 RSA 加密，ticket/randstr 为腾讯滑块结果 |
| `/api/v1/preview/quizzes?courseId=` | GET | 提前获取题目：老师发题前预览课件里已下发的题（博客第 5 节），无进行中课堂返回 409 |
| `/api/v1/inclass/quizzes?courseId=` | GET | 随堂题：签到换 jwt → 拉课件 slides[].problem，含未发布/已超时题 |
| `/api/v1/inclass/answers` | POST | 随堂超时补答：body `{courseId,problemId,problemType,answer}`，重复提交幂等覆盖 |
| `/api/v1/content/activities?courseId=` | GET | 课程的全部活动/课时列表 |
| `/api/v1/content/lesson?lessonId=` | GET | 课时元信息（含 presentationIds） |
| `/api/v1/content/slides?lessonId=` | GET | 课件 PPT 页（cover 图片直链 + problem 教师题目） |

实现要点：
- **Mock 模式**：本地生成二维码与凭据，一键演示。
- **Real 模式（实测可用）**：走 WebSocket 协议 `wss://www.yuketang.cn/wsapp/`，发送 `requestlogin` 拿到真实微信二维码（JPG），用户扫码确认后收 `loginsuccess`(UserID/Auth)，再 `GET /v/course_meta/user_info` 取 csrftoken → `POST /pc/web_login` 换 `sessionid`。默认 standard 区域，四区域会话不通用。完整协议与实测结果见工作区根目录 `雨课堂扫码协议-调研记录.md`。
  > 注意：博客里 `changjiang.ykt.io/custom/auth/v1/auth/status` 实测返回 404，故改用此 WebSocket 方案。
- 后端用本地随机 token（`GXH-*`）绑定 `session_key`/`sessionid`，会话只存内存、2 小时有效，不落库不写日志。
- 业务接口统一接受 `X-Session-Token` 请求头；`SessionResolver` 解析出 session_key 再访问雨课堂；无 token 返回 401。兼容旧 `X-Session-Key`。

> 前端联调：访问 `http://localhost:3000/makeup.html` → 点「获取登录二维码」→ 点「模拟手机端确认登录」→ 进入课程列表（token 自动保存在 localStorage）。

### 功能一：免扫码秒进班（博客第 1 节）

| 接口 | 方法 | 说明 |
|---|---|---|
| `/api/v1/courses` | GET | 拉取课表；`liveCourses` 即 `status == 1` 的进行中课堂 |
| `/api/v1/lessons/join` | POST | 用 `session_key` 换取凭据，签发带 `sign` + `ts` 的临时进班链接（5 分钟有效） |
| `/api/v1/lessons/verify-sign` | GET | 校验临时签名链接是否被篡改 / 过期 |

实现要点：
- 服务端用 HMAC-SHA256 对 `lessonId|ts` 签名，链接只在不过期时有效；
- 凭据缓存在内存（进程重启即失效），不落库、不记录明文；
- 未开课（status≠1）课程调用 join 直接返回业务码 409。

### 功能二：超时补答（博客第 3 节）

| 接口 | 方法 | 说明 |
|---|---|---|
| `/api/v1/quizzes?courseId=net` | GET | 按课程拉取可补交题目（含题型、选项、填空位） |
| `/api/v1/quizzes/{quizId}` | GET | 查询题目与作答状态 |
| `/api/v1/quizzes/{quizId}/answers` | POST | 提交 / 补交答案；超时仍接收，老师结束作答后拒收；同题重复提交幂等覆盖 |
| `/api/v1/quizzes/{quizId}/draft` | PUT / GET | 草稿暂存与读取（与正式提交解耦） |
| `/api/v1/uploads/draft-image` | POST | 草稿图上传，最多 9 张，本地磁盘存储 |
| `/api/v1/uploads/{filename}` | GET | 读取上传的图片 |

实现要点：
- 只有 `QuizOpenState.CLOSED`（老师结束作答）才拒收；`TIMEOUT_MAKEUP` 正常接收并标记 `makeup=true`；
- 同一 `(quizId, sessionKey 哈希)` 重复提交覆盖旧答案，`submitCount` 自增（幂等）。

**题型适配**（`Quiz.questionType`）：
- `SINGLE_CHOICE` / `MULTIPLE_CHOICE`：`options` 为选项文本数组，前端提交选项字母（多选逗号拼接）；
- `FILL_BLANK`：`blanks` 数组长度即空位数（1=单空、N=多空），前端提交各空内容逗号拼接；
- `IMAGE_UPLOAD`：前端先调 `/uploads/draft-image` 上传（最多 9 张），提交时带 `imageUrls`；
- 主观题按单空填空渲染。Mock 数据 `courseId=net` 下 6 道题覆盖全部题型，可直接联调前端。

---

## 3. 快速验证（curl）

```bash
# 0. 先登录（mock 模式一步登录，返回 token）
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/mock-login \
  | python -c "import sys,json;print(json.load(sys.stdin)['data']['token'])")

# 1. 查看课表（应返回 2 门 status=1 的课程）
curl -H "X-Session-Token: $TOKEN" http://localhost:8080/api/v1/courses

# 2. 免扫码进班（返回带 sign 的 joinUrl）
curl -X POST http://localhost:8080/api/v1/lessons/join \
  -H "X-Session-Token: $TOKEN" -H "Content-Type: application/json" \
  -d "{\"courseId\":\"net\",\"sessionKey\":\"ignored\"}"

# 3. 查看超时题目（state=TIMEOUT_MAKEUP）
curl -H "X-Session-Token: $TOKEN" "http://localhost:8080/api/v1/quizzes?courseId=net"

# 4. 补交答案（makeup=true；sessionKey 由 token 注入，body 可省略）
curl -X POST http://localhost:8080/api/v1/quizzes/q-net-204/answers \
  -H "X-Session-Token: $TOKEN" -H "Content-Type: application/json" \
  -d "{\"answer\":\"BGP-4,RIP\",\"imageUrls\":[],\"draftOnly\":false}"

# 5. 退出登录
curl -X POST -H "X-Session-Token: $TOKEN" http://localhost:8080/api/v1/auth/logout
```

> 分步体验 45 秒长轮询：先 `curl -X POST /auth/poll -d '{"ticket":"..."}'` 挂起，再 `curl -X POST /auth/mock-confirm -d '{"ticket":"..."}'`，可见第一个请求瞬间返回。

---

## 4. 切换到真实雨课堂接口

在 `application.yml` 中改：

```yaml
gongxiaohu:
  mode: real
  join-signature:
    secret: 你自己的随机密钥
  ykt-gateway:
    base-url: https://www.yuketang.cn      # 你学校所在区域域名
    ws-url: wss://www.yuketang.cn/wsapp/   # 扫码登录 WebSocket（standard 区域）
    endpoints:
      course-list: /你的抓包路径
      join-lesson: /v2/web/join_lesson
      quiz-detail: /你的抓包路径
      submit-answer: /你的抓包路径
```

`RealYktGateway` 使用 `JsonNode` 宽松解析，字段映射集中在 `text()` / `mapState()` 方法里，按你抓包得到的真实响应字段调整即可。登录部分已替换为实测可用的 WebSocket 协议（`YktWebSocketLogin`），不再依赖 `auth-qrcode`/`auth-status` 端点；其余业务端点标记「抓包待确认」，未配置时会抛明确异常。

**安全提醒**：真实凭证只通过请求头传递，服务端不落明文；生产环境务必替换 `join-signature.secret`，并启用 HTTPS。

---

## 5. 测试与目录说明

- `mvn test`：冒烟测试覆盖课表过滤、进班签名、超时补交幂等、签名防篡改、扫码登录链路；`RealLoginProtocolTest` 会联网请求真实二维码（需网络，离线可跳过）。
- `src/main/java/com/jiclub/gongxiaohu/`
  - `config/` — 配置绑定与 CORS
  - `gateway/` — 上游网关抽象 + Mock/Real 两实现 + `YktWebSocketLogin`（真实扫码，实测可用）
  - `service/` — 两大功能业务逻辑
  - `controller/` — REST 接口
  - `model/` — 领域模型（Java record，集中定义）
  - `util/SignUtil` — HMAC-SHA256 进班签名
- 上传图片默认保存在 `backend/data/draft-images/`（已被 .gitignore 忽略）
