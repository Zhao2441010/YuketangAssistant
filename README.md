# 长江雨课堂助手

从 `course_helper` 独立出来的 **只服务长江雨课堂**（`changjiang.yuketang.cn`）的 Flutter 客户端。学习通、荷塘/黄河/主站雨课堂相关代码与入口已移除；原有雨课堂功能全部保留，并按参考项目补上了历史课件与课间题目能力。

## 与原始项目的关系

- 代码基线：`D:/workspace/tools/can/course_helper` v1.2.5。
- 协议与抓取链路参考：`D:/workspace/tools/YuketangAssistant/技术说明.md`（React + Spring Boot 版），只取其**前端格式**与**历史课件 / 课间习题解耦链路**，不引入其后端。
- 许可证：GPL v3（保留原项目 LICENSE 与版权声明）。

## 功能

### 登录与会话
- 密码登录、短信验证码登录、微信二维码登录（腾讯防水墙校验）。
- 多账号管理：切换、排序、删除、失效提示、Cookie 按 uid 持久化并自动注入 `Cookie / x-csrftoken / x-uid / sessionid`。

### 首页
- **最上方**：「所有课程」与「课间 · 测验区」两个入口。
- **下方**：当前进行中课程（每 3 秒轮询 `on-lesson`），点入即进入课堂。
- 右上角扫码：长江雨课堂动态二维码，`/api/v3/app/scan` 交给服务端换 `lessonId`，支持多账号并发。

### 课程中心（三级下钻）
1. 全部课程：`/v/course_meta/learning_list/`。
2. 课时列表：`/v2/api/web/logs/learn/{courseId}?actype=-1`，关键是取 `courseware_id`。
3. 历史课件：`/api/v3/lesson-summary/student?lesson_id={coursewareId}` → `presentation?presentation_id=&lesson_id=` → `slides[].cover / problem`。没有 `courseware_id` 时用 `activityId` 兜底。

课件页支持：多课件切换、PageView 翻页、缩略图条、点击题目跳页、一键下载全部页到相册。

### 课间 / 课件习题解耦
- 课件内嵌题目（`slides[].problem`，正文在 `content` 里）统一抽到课件页**顶部测验区**，可折叠。
- 题目显示正文、选项、参考答案并高亮正确选项；点击条目跳到对应课件页。
- 实时随堂题仍在课堂页（`pages/presentation/`）内作答，与历史课件题目互不干扰。

### 课堂能力（保留原项目）
- 免扫码进课堂：`checkin` (`source=21` + `joinIfNotIn`) → `set-auth` / `lessonToken` → `wss://changjiang.yuketang.cn/wsapp/` hello。
- 当前课堂整份 PPT（`presentation/fetch`）、位置签到自动提交、课堂答题与主观题图片上传、随机点名通知。

## 构建与运行

需要 Flutter `^3.41.8` / Dart `^3.11.5`（Android 另需 Java 17、Android SDK）。

```bash
flutter pub get
flutter run                     # debug 运行
flutter build apk --release     # Android 发布包
```

Android 发布构建需要 `android/key.properties` 与 keystore（原项目 CI 用 GitHub Secrets 注入，仓库中不包含）；debug 构建不需要。若 Gradle 在配置阶段因签名配置报错，请自行补一份 debug 签名配置。

## 目录要点

```
lib/
├── api/            # 登录、课程/课堂、历史课件、图片上传、HTTP 服务
├── models/         # Course / Presentation / Courseware
├── pages/
│   ├── courses/    # 首页（课程入口 + 进行中课程）
│   ├── courseware/ # 课程中心三级下钻 + 顶部测验区
│   ├── presentation/ # 课堂 PPT / 答题（原链路）
│   └── widget/     # 头像、扫码、图片预览
├── session/        # 账号与 Cookie
└── utils/
```

## 边界与声明

- 仅用于个人学习；涉及课堂考勤与课件内容，请遵守所在学校规定，不要传播课件原文。
- 服务端 `Set-Cookie` 的 domain 策略与返回结构以运行时为准；本项目的链路来自静态代码走读与参考项目技术说明。
