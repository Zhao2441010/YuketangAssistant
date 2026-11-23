// 超时补答前端 - API 封装与类型定义
// 后端地址：默认 http://localhost:8080/api/v1，可通过 VITE_API_BASE_URL 覆盖

const API_BASE = (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? 'http://localhost:8080/api/v1';
const TOKEN_KEY = 'gxH_sessionToken';
const SESSION_ID_KEY = 'gxH_sessionId';   // 真实 sessionid，用于 token 过期时无感续期

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

// ============ 本地会话 token 管理 ============
export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}
export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token);
}
export function clearToken(): void {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(SESSION_ID_KEY);
}
export function getSessionId(): string | null {
  return localStorage.getItem(SESSION_ID_KEY);
}
export function setSessionId(sid: string): void {
  localStorage.setItem(SESSION_ID_KEY, sid);
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
  setToken(result.data.token);
}

// ============ 登录链路 ============
/** 1. 获取二维码 */
export function fetchLoginQr(): Promise<LoginQr> {
  return request<LoginQr>('/auth/qr', undefined, false);
}

/** 2. 45 秒阻塞长轮询：扫码确认瞬间返回 token，并保存到本地供业务请求携带 */
export async function pollLogin(ticket: string): Promise<LoginResult> {
  const result = await request<LoginResult>('/auth/poll', {
    method: 'POST',
    body: JSON.stringify({ ticket })
  }, false);
  setToken(result.token);
  setSessionId(result.sessionKey); // 保存真实 sessionid，供 token 过期时无感续期
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
  setToken(result.token);
  setSessionId(result.sessionKey);
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
 * 会话保活：若剩余不足 30 分钟则主动续期；失败则尝试无感重导。
 * 在页面加载与关键操作前调用，实现"静默续签"。
 */
export async function keepAlive(): Promise<boolean> {
  try {
    const info = await checkSession();
    if (!info.valid) return false;
    if (info.remainingSeconds < 30 * 60 && info.renewable) {
      await renewSession();
    }
    return true;
  } catch (e) {
    if ((e as Error & { code?: number }).code === 401 && getSessionId()) {
      try { await silentRenew(); return true; } catch { return false; }
    }
    return false;
  }
}

/** 退出登录 */
export function logout(): Promise<void> {
  return request<void>('/auth/logout', { method: 'POST' });
}

// ============ 业务接口（自动携带 token） ============
export function fetchCourses(): Promise<CourseListResult> {
  return request<CourseListResult>('/courses');
}

export function fetchQuizzes(courseId: string): Promise<Quiz[]> {
  return request<Quiz[]>(`/quizzes?courseId=${encodeURIComponent(courseId)}`);
}

/** 提前获取题目：老师发题前预览课件里已下发的题目（博客第 5 节）。 */
export function fetchPreviewQuizzes(courseId: string): Promise<Quiz[]> {
  return request<Quiz[]>(`/preview/quizzes?courseId=${encodeURIComponent(courseId)}`);
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
