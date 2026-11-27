// 超时补答前端 - API 封装与类型定义
// 后端地址：默认 http://localhost:8080/api/v1，可通过 VITE_API_BASE_URL 覆盖
//
// v1.2：凭据统一交给 authStore 管理（多账号 + 原生持久化）。

import {
  activateAccount,
  clearToken as clearStoredToken,
  getAccounts,
  getActiveAccount,
  getSessionId,
  getToken,
  hydrateAuth,
  isHydrated,
  persistAuth,
  removeAccount,
  saveLoginAccount,
  setSessionId,
  setToken,
  updateActiveAccount
} from './authStore';
import type { LoginKind, StoredAccount } from './authStore';

const API_BASE = (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? 'http://localhost:8080/api/v1';

export interface Course {
  courseId: string;
  name: string;
  teacher: string;
  room: string;
  timeRange: string;
  weekday: number;
  status: number; // 1 = 上课中
  totalSlides: number;
  currentSlide: number;
}

export interface CourseListResult {
  allCourses: Course[];
  liveCourses: Course[];
  source: string;
}

export type QuestionType = 'SINGLE_CHOICE' | 'MULTIPLE_CHOICE' | 'FILL_BLANK' | 'IMAGE_UPLOAD';
export type QuizState = 'UNPUBLISHED' | 'PUBLISHED' | 'TIMEOUT_MAKEUP' | 'CLOSED' | 'SUBMITTED';

export interface Quiz {
  quizId: string;
  courseId: string;
  type: string;
  score: number;
  question: string;
  state: QuizState;
  correctAnswer: string;
  questionType: QuestionType;
  options: string[];
  blanks: string[];
}

export interface SubmitResult {
  quizId: string;
  accepted: boolean;
  message: string;
  submitCount: number;
  submittedAt: number;
  makeup: boolean;
}

export interface LoginQr {
  ticket: string;
  qrDataUrl: string;
  qrContent: string;
  expiresAt: number;
  pollTimeoutSeconds: number;
  mockMode: boolean;
}

export interface LoginResult {
  token: string;
  userId: string;
  sessionKey: string;
  expiresAt: number;
  mock: boolean;
}

export interface SessionInfo {
  token: string;
  userId: string;
  expiresAt: number;
  valid: boolean;
  remainingSeconds: number;
  renewable: boolean;
}

export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

// ============ 本地会话/账号管理 ============
// 具体实现见 authStore.ts：原生 SharedPreferences 优先，localStorage 兜底，
// 支持保存多个账号并记住上一次使用的账号。

export {
  activateAccount,
  getAccounts,
  getActiveAccount,
  getSessionId,
  getToken,
  hydrateAuth,
  isHydrated,
  persistAuth,
  removeAccount,
  setSessionId,
  setToken,
  updateActiveAccount
};
export type { StoredAccount, LoginKind };

/** 只清当前账号的 token（保留 sessionid，下次可无感重建会话）。 */
export function clearToken(): void {
  clearStoredToken();
}

async function request<T>(path: string, init?: RequestInit, auth = true, retried = false): Promise<T> {
  const headers: Record<string, string> = {
    ...(init?.body instanceof FormData ? {} : { 'Content-Type': 'application/json' }),
    ...(init?.headers as Record<string, string> | undefined ?? {})
  };
  if (auth) {
    const token = getToken();
    if (token) headers['X-Session-Token'] = token;
  }
  const response = await fetch(`${API_BASE}${path}`, { ...init, headers });
  if (!response.ok) {
    throw new Error(`网络错误：HTTP ${response.status}`);
  }
  const result = (await response.json()) as ApiResponse<T>;
  if (result.code !== 0) {
    // 401 且持有真实 sessionid → 无感续期后重放请求（仅一次）
    if (result.code === 401 && auth && !retried && getSessionId()) {
      try {
        await silentRenew();
        return await request<T>(path, init, auth, true);
      } catch { /* 续期失败则继续抛出原 401 */ }
    }
    const error = new Error(result.message || '请求失败') as Error & { code?: number };
    error.code = result.code;
    throw error;
  }
  return result.data;
}

/** 无感续期：用本地保存的真实 sessionid 重新建立会话（不打断用户）。 */
async function silentRenew(): Promise<void> {
  const sid = getSessionId();
  if (!sid) throw new Error('无可用 sessionid');
  const result = await fetch(`${API_BASE}/auth/import-session`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ sessionId: sid })
  }).then(r => r.json()) as ApiResponse<LoginResult>;
  if (result.code !== 0) throw new Error(result.message);
  updateActiveAccount({
    token: result.data.token,
    sessionId: result.data.sessionKey || sid,
    expiresAt: result.data.expiresAt
  });
  await persistAuth();
}

// ============ 登录链路 ============
/** 1. 获取二维码 */
export function fetchLoginQr(): Promise<LoginQr> {
  return request<LoginQr>('/auth/qr', undefined, false);
}

/**
 * 2. 轮询扫码结果。
 *
 * 2026-11-25：改为「短轮询」。原来是 45 秒长轮询，移动端（Android WebView / 运营商 NAT）
 * 会把空闲 30-40 秒的连接静默掐断，后端把扫码成功写回这条已死连接时前端只收到网络错误，
 * 于是一直重试、永远停在二维码页——但服务端日志里已经有 LOGIN_OK。
 * 现在每次 1 秒内必定返回，且后端按 ticket 幂等返回同一 token，响应丢了下次也能取回。
 *
 * @param waitSeconds 单次最长等待秒数，0 = 立即返回
 */
export async function pollLogin(ticket: string, waitSeconds = 0): Promise<LoginResult> {
  const result = await request<LoginResult>('/auth/poll', {
    method: 'POST',
    body: JSON.stringify({ ticket, wait: String(waitSeconds) })
  }, false);
  saveLoginAccount({
    userId: result.userId,
    token: result.token,
    sessionId: result.sessionKey,
    mock: result.mock,
    kind: 'qr',
    expiresAt: result.expiresAt
  });
  await persistAuth();
  return result;
}

/** 账号密码登录：ticket/randstr 为用户完成腾讯滑块验证码后的结果 */
export async function passwordLogin(
  account: string, password: string, type: 'PP' | 'E',
  ticket: string, randstr: string
): Promise<LoginResult> {
  const result = await request<LoginResult>('/auth/password-login', {
    method: 'POST',
    body: JSON.stringify({ account, password, type, ticket, randstr })
  }, false);
  saveLoginAccount({
    userId: result.userId,
    token: result.token,
    sessionId: result.sessionKey,
    mock: result.mock,
    kind: 'password',
    expiresAt: result.expiresAt
  });
  await persistAuth();
  return result;
}

/** 3. 演示模式：模拟手机端扫码确认 */
export function mockConfirm(ticket: string): Promise<boolean> {
  return request<boolean>('/auth/mock-confirm', {
    method: 'POST',
    body: JSON.stringify({ ticket })
  }, false);
}

/** 校验本地 token 是否有效 */
export function checkSession(): Promise<SessionInfo> {
  return request<SessionInfo>('/auth/session');
}

/** 主动续期：延长会话有效期，返回新的过期信息。 */
export function renewSession(): Promise<SessionInfo> {
  return request<SessionInfo>('/auth/renew', { method: 'POST' });
}

/**
 * 会话保活：
 * 1. 只有 sessionid、没有 token（App 刚启动或上次 token 失效）→ 先无感重建；
 * 2. token 有效则按需续期；
 * 3. 后端返回 valid=false（例如后端重启、token 过期）→ 用 sessionid 重建后再查一次。
 * 这样只要用户没主动退出，重开 App 也能自动回到登录态。
 */
export async function keepAlive(): Promise<boolean> {
  if (!getToken() && getSessionId()) {
    try { await silentRenew(); } catch { return false; }
  }
  try {
    const info = await checkSession();
    if (!info.valid) {
      if (!getSessionId()) return false;
      try { await silentRenew(); } catch { return false; }
      const again = await checkSession();
      if (again.valid && again.userId) updateActiveAccount({ userId: again.userId });
      return again.valid;
    }
    if (info.userId) updateActiveAccount({ userId: info.userId });
    if (info.remainingSeconds < 30 * 60 && info.renewable) {
      try { await renewSession(); } catch { /* 续期失败不影响当前使用 */ }
    }
    return true;
  } catch (e) {
    if ((e as Error & { code?: number }).code === 401 && getSessionId()) {
      try { await silentRenew(); return true; } catch { return false; }
    }
    return false;
  }
}

/** 退出当前账号：通知后端注销，并从本机账号列表移除。 */
export async function logout(): Promise<void> {
  try {
    await request<void>('/auth/logout', { method: 'POST' });
  } catch { /* 后端不可达也要清本地 */ }
  const active = getActiveAccount();
  if (active) removeAccount(active.id);
}

/** 删除指定账号（如果还持有该账号的 token，顺带通知后端注销）。 */
export async function revokeAccount(id: string): Promise<void> {
  const acc = getAccounts().find(a => a.id === id);
  if (acc?.token) {
    try {
      await fetch(`${API_BASE}/auth/logout`, {
        method: 'POST',
        headers: { 'X-Session-Token': acc.token }
      });
    } catch { /* 忽略网络错误，本地仍然删除 */ }
  }
  removeAccount(id);
}

// ============ 业务接口（自动携带 token） ============
export function fetchCourses(): Promise<CourseListResult> {
  return request<CourseListResult>('/courses');
}

export function fetchQuizzes(courseId: string): Promise<Quiz[]> {
  return request<Quiz[]>(`/quizzes?courseId=${encodeURIComponent(courseId)}`);
}

/**
 * 进班（方案④：后端自渲染）。
 * 后端用完整 L1 登录态换取课堂凭证三元组，凭证只留后端内存；
 * 前端仅拿 entryId，再凭它拉课件/题目自绘，无需进入官方课堂页、无需注入 Cookie。
 */
export interface JoinLessonResult {
  lessonId: string;
  courseName: string;
  entryId: string;
  joinUrl: string;          // 官方在读学生固定入口（供用户主动在浏览器打开，非主路径）
  credentialReady: boolean;  // 三元组是否换出成功
  hint: string;              // 降级提示（如动态码签到需本人扫码）
  issuedAt: number;
  expiresAt: number;
  fromCache: boolean;
}
/** enhance=true 时后端才会按需尝试 checkin 换 pptJwt（仅自渲染增强，不影响进课堂）。 */
export function joinLesson(courseId: string, enhance = false): Promise<JoinLessonResult> {
  const sid = getSessionId();
  return request<JoinLessonResult>(`/lessons/join?enhance=${enhance}`, {
    method: 'POST',
    body: JSON.stringify({ courseId, sessionKey: sid || '' })
  });
}

/** 官方域 Cookie 注入信息（供 App 内 WebView 免扫码进入课堂会话）。 */
export interface EntryCookie {
  domain: string;
  cookie: string;
  note: string;
}
export function fetchEntryCookie(): Promise<EntryCookie> {
  return request<EntryCookie>('/lessons/entry-cookie');
}

/** 只读探测：复现官方课堂页的 lesson_info_v2，验证在读学生能否免扫码进班（不含凭据）。 */
export interface LessonInfoProbe {
  lessonId: string;
  courseName: string;
  httpStatus: number;
  code: number;
  msg: string;
  hasUserAuth: boolean;
  presentationCount: number;
  quizCount: number;
  hasClassroom: boolean;
  note: string;
}
export function probeLessonInfo(courseId: string): Promise<LessonInfoProbe> {
  return request<LessonInfoProbe>(`/lessons/lesson-info?courseId=${encodeURIComponent(courseId)}`);
}

/** 方案④自渲染视图：后端持 pptJwt 拉好课件与题目，前端直接绘制。 */
export interface ClassroomView {
  lessonId: string;
  courseName: string;
  presentations: Presentation[];
  quizzes: Quiz[];
  credentialReady: boolean;
  hint: string;
}
export function fetchClassroomRender(entryId: string): Promise<ClassroomView> {
  return request<ClassroomView>(`/lessons/${encodeURIComponent(entryId)}/render`);
}

/** 免扫码进班结果：checkin 换 lessonToken → WSS hello 建立课堂会话。 */
export interface LessonEnterResult {
  lessonId: string;
  courseName: string;
  entered: boolean;          // 是否已在官方侧建立课堂会话
  serverOp: string;          // 服务端回的首帧 op（showpresentation/hello 等，空串=暂无推送）
  code: number;
  msg: string;
  note: string;
}
/** 免扫码进班（不经过 lesson_info_v2，走 checkin 换 lessonToken → WSS hello）。 */
export function enterLesson(courseId: string): Promise<LessonEnterResult> {
  return request<LessonEnterResult>(`/lessons/enter?courseId=${encodeURIComponent(courseId)}`, {
    method: 'POST'
  });
}

/** 验证诊断（§6.7）：后端把 checkin 的验证证据结构化返回，无需手工抓包。全部脱敏。 */
export interface DiagnosticRecord {
  time: string;
  lessonId: string;
  source: number;
  url: string;
  requestHeaders: Record<string, string>;
  missingHeaders: string[];
  httpStatus: number;
  bizCode: number;
  bizMessage: string;
  setAuthPresent: boolean;
  setAuthFingerprint: string;
  socketJwtPresent: boolean;
  identityIdPresent: boolean;
  outcome: string;
  elapsedMs: number;
}
export interface DiagnosticChecklist {
  checkinHeadersCaptured: boolean;
  setAuthCaptured: boolean;
  tripleCaptured: boolean;
  websocketHintCaptured: boolean;
  getTokenKnown: boolean;
  note: string;
}
export interface DiagnosticReport {
  totalAttempts: number;
  records: DiagnosticRecord[];
  checklist: DiagnosticChecklist;
  privacyNote: string;
}
export function fetchDiagnostics(): Promise<DiagnosticReport> {
  return request<DiagnosticReport>('/diagnostics');
}
export function clearDiagnostics(): Promise<void> {
  return request<void>('/diagnostics', { method: 'DELETE' });
}

/**
 * 导入整串 Cookie 补齐 L1 登录态（csrftoken / X-UID / university-id / uv-id）。
 * 课堂凭证换取需要这些字段；仅 sessionid 会 401。Cookie 只进后端内存，不落前端。
 */
export async function importCookie(cookie: string, userId?: string): Promise<LoginResult> {
  const res = await fetch(`${API_BASE}/auth/import-cookie`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ cookie, userId: userId || '' })
  }).then(r => r.json()) as ApiResponse<LoginResult>;
  if (res.code !== 0) throw new Error(res.message || 'Cookie 导入失败');
  saveLoginAccount({
    userId: res.data.userId,
    token: res.data.token,
    sessionId: res.data.sessionKey,
    mock: res.data.mock,
    kind: 'cookie',
    expiresAt: res.data.expiresAt
  });
  await persistAuth();
  return res.data;
}

/** 提前获取题目：老师发题前预览课件里已下发的题目（博客第 5 节）。 */
export function fetchPreviewQuizzes(courseId: string): Promise<Quiz[]> {
  return request<Quiz[]>(`/preview/quizzes?courseId=${encodeURIComponent(courseId)}`);
}

// ============ 课程中心：课程 → 课时 → 课件/题目 ============
export interface Activity {
  activityId: string;
  coursewareId: string;   // 历史课件必须用此 id 作 lesson_id
  title: string;
  type: number;
  typeName: string;
  createTime: number;
  finished: boolean;
  attended: boolean;
}
export interface Lesson {
  lessonId: string;
  lessonName: string;
  courseName: string;
  classroomName: string;
  teacherName: string;
  presentationIds: string[];
}
export interface Slide {
  id: string;
  page: number;
  cover: string;
  problem: string | null;
}
export interface Presentation {
  id: string;
  title: string;
  cover: string;
  slidesCount: number;
  slides: Slide[];
}

/** 拉课程的全部活动/课时（courseId 用课表返回的 courseId） */
export function fetchActivities(courseId: string): Promise<Activity[]> {
  return request<Activity[]>(`/content/activities?courseId=${encodeURIComponent(courseId)}`);
}

/** 拉课件页（PPT 图片 + 教师题目） */
export function fetchSlides(lessonId: string): Promise<Presentation[]> {
  return request<Presentation[]>(`/content/slides?lessonId=${encodeURIComponent(lessonId)}`);
}

// ===== 全量课件：整门课 + 尚未播放的课时 + 进行中课堂未放映的页 =====
export interface CoursewareSlide {
  page: number;
  imageUrl: string;
  problem: string | null;
  source: string;          // summary | jwt | summary+jwt
}
export interface CoursewarePresentation {
  id: string;
  title: string;
  declaredCount: number;   // 官方声明总页数
  fetchedCount: number;    // 实际拿到页数
  slides: CoursewareSlide[];
  source: string;
}
export interface CoursewareLesson {
  activityId: string;
  coursewareId: string;
  title: string;
  finished: boolean;
  attended: boolean;
  declaredSlides: number;
  fetchedSlides: number;
  presentations: CoursewarePresentation[];
  warning: string;
}
export interface CoursewareBundle {
  courseId: string;
  lessonCount: number;
  presentationCount: number;
  slideCount: number;
  missingPageCount: number;
  complete: boolean;
  lessons: CoursewareLesson[];
  warnings: string[];
}

/**
 * 抓取一门课的全部课件（含尚未播放的课时）。
 * useJwt=true 时后端会在缺页时尝试用课堂凭证补全（会触发 checkin，可能被动态码拦截）。
 */
export function fetchAllCourseware(courseId: string, useJwt = false): Promise<CoursewareBundle> {
  return request<CoursewareBundle>(
    `/content/course-all?courseId=${encodeURIComponent(courseId)}&useJwt=${useJwt ? 'true' : 'false'}`
  );
}

/** 下载全量课件 ZIP。带 X-Session-Token，不能用普通 <a href>。 */
export async function downloadCoursewareZip(courseId: string, useJwt = false): Promise<void> {
  const headers: Record<string, string> = {};
  const token = getToken();
  if (token) headers['X-Session-Token'] = token;
  const resp = await fetch(
    `${API_BASE}/content/course-all.zip?courseId=${encodeURIComponent(courseId)}&useJwt=${useJwt ? 'true' : 'false'}`,
    { headers }
  );
  if (!resp.ok) throw new Error(`下载失败：HTTP ${resp.status}`);
  const blob = await resp.blob();
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `courseware-${courseId}.zip`;
  document.body.appendChild(a);
  a.click();
  a.remove();
  window.setTimeout(() => URL.revokeObjectURL(url), 1000);
}

// ============ 随堂超时补答（真实随堂题，倒计时结束老师未收题仍可提交） ============
/** 拉取某门课的随堂题（含未发布/已超时的题）。传 courseId，后端自动定位进行中课堂。 */
export function fetchInClassQuizzes(courseId: string): Promise<Quiz[]> {
  return request<Quiz[]>(`/inclass/quizzes?courseId=${encodeURIComponent(courseId)}`);
}

/** 题型前端枚举 → 雨课堂随堂题题型码：1单选 2多选 3投票 4填空 5主观。 */
function toProblemType(q: Quiz): number {
  switch (q.questionType) {
    case 'MULTIPLE_CHOICE': return 2;
    case 'FILL_BLANK': return q.type.includes('主观') || q.type.includes('简答') ? 5 : 4;
    case 'IMAGE_UPLOAD': return 5;
    default: return 1;
  }
}

/** 提交/超时补交随堂答案（重复提交幂等覆盖）。 */
export function submitInClassAnswer(
  courseId: string, quiz: Quiz, answer: string
): Promise<SubmitResult> {
  return request<SubmitResult>('/inclass/answers', {
    method: 'POST',
    body: JSON.stringify({
      courseId,
      problemId: quiz.quizId,
      problemType: toProblemType(quiz),
      answer
    })
  });
}

export function submitAnswer(quizId: string, answer: string, imageUrls: string[]): Promise<SubmitResult> {
  return request<SubmitResult>(`/quizzes/${encodeURIComponent(quizId)}/answers`, {
    method: 'POST',
    body: JSON.stringify({ answer, imageUrls, draftOnly: false })
  });
}

export function saveDraft(quizId: string, answer: string, imageUrls: string[]): Promise<void> {
  return request<void>(`/quizzes/${encodeURIComponent(quizId)}/draft`, {
    method: 'PUT',
    body: JSON.stringify({ answer, imageUrls, draftOnly: true })
  });
}

export async function uploadImage(file: File, existingCount: number): Promise<string> {
  const form = new FormData();
  form.append('file', file);
  form.append('existing', String(existingCount));
  const result = await request<{ url: string; filename: string; size: number }>(
    `/uploads/draft-image?existing=${existingCount}`,
    { method: 'POST', body: form }
  );
  return result.url;
}

export function imageUrl(url: string): string {
  return url.startsWith('http') || url.startsWith('data:') ? url : `${API_BASE.replace(/\/api\/v1$/, '')}${url}`;
}
