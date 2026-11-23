> # ⚠️ 重要声明
>
> **本项目的前端与后端均由 AI 开发完成。**
>
> **仅供个人学习与使用，不进行任何商业化，不对外提供服务，也不用于任何扰乱教学秩序的场景。**
>
> 请遵守你所在学校与平台的相关规定，因不当使用产生的后果由使用者自行承担。

# YuketangAssistant

一个本地运行的雨课堂辅助工具：登录后可查看课程与课件、提前获取课件内嵌题目、在老师未收题时完成随堂补答。

本项目**受工小湖项目启发**，参考其公开技术博客中关于抓包分析与功能设计的思路，独立实现了本项目的前后端。

---

## 功能特性

- **两种登录方式**
  - 微信扫码登录（45 秒阻塞长轮询）
  - 账号密码登录（密码 RSA 加密 + 腾讯滑块验证）
  - 会话自动续期，token 过期无感重连
- **免扫码进班**
  - 自动识别 `status=1` 的正在上课课程
  - HMAC-SHA256 签名进班链接
- **随堂超时补答**
  - 支持单选、多选、填空、图片上传等题型
  - 倒计时结束但老师未收题时仍可补交，重复提交幂等覆盖
  - 草稿本地持久化、最多 9 张图片直传
- **课程中心（三级下钻）**
  - 全部课程 → 课时列表 → 课件与题目
  - 历史课件获取（按 `courseware_id` 定位课件）
  - 课件与测验分层解耦：教师题目集中在顶部测验区，点击题目跳转对应课件页
  - PPT 浏览：缩略图侧边预览快速跳转、滚轮翻页（不滚动页面）、页码输入框跳转
- **题目预测**
  - 从课件页的 `problem` 字段提前提取题目
  - 展示题干、选项、参考答案，并高亮正确选项

---

## 技术栈

| 层次   | 选型                                               |
| ---- | ------------------------------------------------ |
| 前端   | React 19 + Vite 8 + TypeScript + Tailwind CSS v4 |
| 后端   | Java 21 + Spring Boot 3.4.5 + Maven              |
| 运行模式 | `mock`（内置演示数据）/ `real`（真实雨课堂接口）                  |

---

## 快速开始

### 1. 启动后端

用 IDE 打开 `backend/` 目录，运行 `GongXiaoHuApplication`（默认 `http://localhost:8080`）。

或使用命令行：

```bash
cd backend
mvn -DskipTests spring-boot:run
```

配置见 `backend/src/main/resources/application.yml`：

- `gongxiaohu.mode: mock` —— 无需账号即可体验完整流程
- `gongxiaohu.mode: real` —— 使用真实雨课堂接口

### 2. 启动前端

```bash
pnpm install
pnpm dev
```

浏览器打开 <http://localhost:3000/>。

### 3. 运行测试

```bash
cd backend && mvn test     # 后端测试
cd .. && pnpm build        # 前端构建
```

---

## 项目结构

```
.
├── index.html / makeup.html   # 前端入口（均指向补答应用）
├── src/
│   └── makeup/
│       ├── main.tsx           # 渲染入口（支持 ?token=&sid= 免扫码）
│       ├── api.ts             # 后端接口封装与类型定义
│       ├── LoginView.tsx      # 扫码 / 账号密码登录页
│       ├── MakeupApp.tsx      # 上课课程列表 + 随堂补答页
│       └── CourseCenter.tsx   # 课程中心（课程→课时→课件/题目）
├── backend/
│   └── src/main/java/com/jiclub/gongxiaohu/
│       ├── config/            # 配置绑定与 CORS
│       ├── gateway/           # 网关抽象 + Mock/Real 实现 + 登录/课件/随堂题协议
│       ├── service/           # 会话、进班、补答、随堂题业务
│       ├── controller/        # /api/v1 REST 接口
│       ├── model/             # 领域模型（record）
│       └── util/              # 签名工具
└── 技术说明.md                # 开发交接文档（含实现细节与验证记录）
```

---

## 主要接口

| 接口                                                                       | 说明               |
| ------------------------------------------------------------------------ | ---------------- |
| `GET /api/v1/auth/qr` · `POST /auth/poll`                                | 扫码登录             |
| `POST /api/v1/auth/password-login`                                       | 账号密码登录           |
| `POST /api/v1/auth/import-session`                                       | 用 sessionid 导入会话 |
| `GET /api/v1/courses`                                                    | 课程列表与上课状态        |
| `GET /api/v1/inclass/quizzes` · `POST /inclass/answers`                  | 随堂题获取与补交         |
| `GET /api/v1/content/activities` · `/content/lesson` · `/content/slides` | 课程中心三级数据         |
| `GET /api/v1/preview/quizzes`                                            | 课件题目预览           |

完整接口与 curl 示例见 `backend/README.md`。

---

## 隐私说明

- 登录凭据与 token 仅保存在浏览器 localStorage 与后端内存中，不写入数据库；
- 日志中的学号、token 均做掩码处理；
- 关闭后端进程后内存会话即失效。

---

## 参考资料

- 工小湖技术博客：《工小湖：给雨课堂和学校教务做了个小程序》<https://blog.jiclub.site/2026/yuketang-deep-dive>
