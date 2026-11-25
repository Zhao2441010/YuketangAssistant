import React, { useCallback, useEffect, useRef, useState } from 'react';
import {
  ArrowLeft, BookOpen, Check, CheckCircle2, ChevronDown, ChevronRight, Clock,
  FileQuestion, Image as ImageIcon, Layers, Loader2, LogOut, MapPin, Maximize2, Minimize2,
  Monitor, MousePointer2, Plus, RefreshCw, Send, Smartphone, User, Wifi, X
} from 'lucide-react';
import {
  Course, Quiz, SubmitResult, ClassroomView, DiagnosticReport, JoinLessonResult,
  checkSession, clearDiagnostics, clearToken, fetchClassroomRender, fetchCourses,
  fetchAllCourseware, fetchDiagnostics, fetchEntryCookie, fetchInClassQuizzes, getToken, imageUrl, joinLesson,
  type LessonInfoProbe, probeLessonInfo, enterLesson, type LessonEnterResult,
  keepAlive, logout, saveDraft, submitInClassAnswer, uploadImage
} from './api';
import { isNativeApp, openOfficialClassroomSession } from './classroomEntry';
import LoginView from './LoginView';
import CourseCenter, { ProblemCard, parseProblem, type ProblemView } from './CourseCenter';

const LETTERS = ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H'];

interface AnswerState {
  choices: string[];        // 单选/多选：选项字母
  blanks: string[];         // 填空：每个空的内容
  images: string[];         // 图片上传：URL 列表
  submitting: boolean;
  uploaded: boolean;
  uploading: boolean;
  result?: SubmitResult;
}

const emptyAnswer = (): AnswerState => ({
  choices: [], blanks: [], images: [], submitting: false, uploaded: false, uploading: false
});

/** 单/多选题的选项区 */
const ChoiceInput: React.FC<{
  quiz: Quiz;
  multiple: boolean;
  value: string[];
  onChange: (next: string[]) => void;
}> = ({ quiz, multiple, value, onChange }) => (
  <div className="space-y-2">
    {quiz.options.map((text, idx) => {
      const label = LETTERS[idx] ?? String(idx + 1);
      const selected = value.includes(label);
      return (
        <button
          key={label}
          onClick={() => {
            if (multiple) {
              onChange(selected ? value.filter(v => v !== label) : [...value, label].sort());
            } else {
              onChange([label]);
            }
          }}
          className={`w-full flex items-center gap-3 p-3.5 rounded-2xl border text-left transition-all ${
            selected
              ? 'border-[#007AFF] bg-[#007AFF]/[0.06]'
              : 'border-black/[0.08] bg-white hover:bg-[#F9F9FB]'
          }`}
        >
          <span className={`w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold shrink-0 ${
            selected ? 'bg-[#007AFF] text-white' : 'bg-[#F2F2F7] text-[#8E8E93]'
          }`}>{label}</span>
          <span className="text-sm text-[#1C1C1E] leading-relaxed">{text}</span>
          {selected && <Check className="w-4 h-4 text-[#007AFF] ml-auto shrink-0" />}
        </button>
      );
    })}
  </div>
);

/** 填空题：按 blanks 数量渲染输入框 */
const BlankInput: React.FC<{
  quiz: Quiz;
  value: string[];
  onChange: (next: string[]) => void;
}> = ({ quiz, value, onChange }) => {
  const count = Math.max(quiz.blanks.length, 1);
  return (
    <div className="space-y-2.5">
      {Array.from({ length: count }, (_, idx) => (
        <div key={idx} className="flex items-center gap-3">
          <span className="text-xs text-[#8E8E93] shrink-0 w-14">{quiz.blanks[idx] ?? `填空 ${idx + 1}`}</span>
          <input
            value={value[idx] ?? ''}
            onChange={e => {
              const next = [...value];
              next[idx] = e.target.value;
              onChange(next);
            }}
            placeholder="请输入答案"
            className="flex-1 px-3.5 py-2.5 rounded-xl bg-[#F2F2F7] text-sm text-[#1C1C1E] focus:outline-none focus:ring-2 focus:ring-[#007AFF]/40"
          />
        </div>
      ))}
    </div>
  );
};

/** 图片上传题：最多 9 张 */
const ImageInput: React.FC<{
  value: string[];
  uploading: boolean;
  onAdd: (files: FileList) => void;
  onRemove: (idx: number) => void;
}> = ({ value, uploading, onAdd, onRemove }) => (
  <div className="space-y-2.5">
    <div className="flex items-center justify-between">
      <span className="text-xs font-semibold text-[#1C1C1E]">作答配图 ({value.length}/9)</span>
      <span className="text-[11px] text-[#8E8E93]">最多 9 张 · 单张 ≤ 5MB</span>
    </div>
    <div className="grid grid-cols-3 gap-2">
      {value.map((src, idx) => (
        <div key={idx} className="relative aspect-square rounded-xl overflow-hidden bg-[#F2F2F7] border border-black/[0.06]">
          <img src={imageUrl(src)} alt={`草稿 ${idx + 1}`} className="w-full h-full object-cover" />
          <button
            onClick={() => onRemove(idx)}
            className="absolute top-1.5 right-1.5 w-6 h-6 rounded-full bg-black/60 text-white flex items-center justify-center"
          >
            <X className="w-3.5 h-3.5" />
          </button>
        </div>
      ))}
      {value.length < 9 && (
        <label className="aspect-square rounded-xl border border-dashed border-[#C7C7CC] bg-[#FAFAFC] flex flex-col items-center justify-center gap-1 cursor-pointer hover:bg-[#F2F2F7] transition-colors">
          {uploading ? <Loader2 className="w-5 h-5 text-[#007AFF] animate-spin" /> : <Plus className="w-5 h-5 text-[#8E8E93]" />}
          <span className="text-[10px] text-[#8E8E93]">添加图片</span>
          <input
            type="file"
            accept="image/jpeg,image/png,image/webp,image/gif"
            multiple
            className="hidden"
            onChange={e => {
              if (e.target.files) onAdd(e.target.files);
              e.target.value = '';
            }}
          />
        </label>
      )}
    </div>
  </div>
);

/** 单道题目卡片 */
const QuizCard: React.FC<{
  quiz: Quiz;
  state: AnswerState;
  onUpdate: (patch: Partial<AnswerState>) => void;
  onSubmit: () => void;
  onSaveDraft: () => void;
}> = ({ quiz, state, onUpdate, onSubmit, onSaveDraft }) => {
  const isChoice = quiz.questionType === 'SINGLE_CHOICE' || quiz.questionType === 'MULTIPLE_CHOICE';
  const isBlank = quiz.questionType === 'FILL_BLANK';
  const isImage = quiz.questionType === 'IMAGE_UPLOAD';
  const answerText = isChoice ? state.choices.join(',') : isBlank ? state.blanks.join(',') : '';

  const handleAddImages = async (files: FileList) => {
    onUpdate({ uploading: true });
    try {
      const urls: string[] = [];
      for (let i = 0; i < files.length && state.images.length + urls.length < 9; i++) {
        const url = await uploadImage(files[i], state.images.length + urls.length);
        urls.push(url);
      }
      onUpdate({ images: [...state.images, ...urls], uploading: false });
    } catch (e) {
      onUpdate({ uploading: false });
      alert(`图片上传失败：${(e as Error).message}`);
    }
  };

  return (
    <div className="bg-white rounded-3xl border border-black/[0.06] p-4 sm:p-5 space-y-4 shadow-[0_1px_3px_rgba(0,0,0,0.02)]">
      {/* 题头 */}
      <div className="flex items-start justify-between gap-3">
        <div className="flex items-center gap-2 flex-wrap">
          <span className="px-2.5 py-1 rounded-lg bg-[#F2F2F7] text-[#1C1C1E] text-[11px] font-bold">{quiz.type}</span>
          <span className="text-[11px] text-[#8E8E93] font-mono">{quiz.score} 分</span>
          {quiz.state === 'TIMEOUT_MAKEUP' && (
            <span className="px-2.5 py-1 rounded-full bg-amber-50 border border-amber-200 text-amber-700 text-[11px] font-medium flex items-center gap-1">
              <Clock className="w-3 h-3" /> 可补交
            </span>
          )}
        </div>
        {state.result && (
          <span className="flex items-center gap-1 text-[11px] text-emerald-600 font-medium shrink-0">
            <CheckCircle2 className="w-3.5 h-3.5" /> 已提交 ×{state.result.submitCount}
          </span>
        )}
      </div>

      <p className="text-[15px] font-semibold text-[#1C1C1E] leading-relaxed">{quiz.question}</p>

      {/* 输入区 */}
      {isChoice && (
        <ChoiceInput
          quiz={quiz}
          multiple={quiz.questionType === 'MULTIPLE_CHOICE'}
          value={state.choices}
          onChange={choices => onUpdate({ choices })}
        />
      )}
      {isBlank && <BlankInput quiz={quiz} value={state.blanks} onChange={blanks => onUpdate({ blanks })} />}
      {isImage && (
        <ImageInput
          value={state.images}
          uploading={state.uploading}
          onAdd={handleAddImages}
          onRemove={idx => onUpdate({ images: state.images.filter((_, i) => i !== idx) })}
        />
      )}

      {/* 操作区 */}
      <div className="flex items-center gap-2 pt-1">
        <button
          onClick={onSaveDraft}
          disabled={state.submitting}
          className="px-4 py-2.5 rounded-2xl bg-[#F2F2F7] text-[#3A3A3C] text-xs font-semibold hover:bg-[#E5E5EA] disabled:opacity-50"
        >
          存草稿
        </button>
        <button
          onClick={onSubmit}
          disabled={state.submitting || (!answerText && state.images.length === 0)}
          className="flex-1 py-2.5 rounded-2xl bg-[#007AFF] text-white text-xs font-bold hover:bg-[#0066CC] active:scale-[0.99] transition-all disabled:opacity-40 flex items-center justify-center gap-1.5"
        >
          {state.submitting ? <Loader2 className="w-3.5 h-3.5 animate-spin" /> : <Send className="w-3.5 h-3.5" />}
          {state.uploaded ? '重新提交答案' : '提交答案'}
        </button>
      </div>

      {state.result && (
        <div className="rounded-2xl bg-emerald-50 border border-emerald-100 px-3.5 py-2.5 text-[12px] text-emerald-700 flex items-start gap-2">
          <CheckCircle2 className="w-4 h-4 shrink-0 mt-0.5" />
          <span>{state.result.message}（提交序号 {state.result.submitCount}，服务端幂等覆盖）</span>
        </div>
      )}
    </div>
  );
};

export default function MakeupApp() {
  // 登录守卫：启动校验本地 token，未登录显示扫码页
  const [authChecking, setAuthChecking] = useState(true);
  const [loggedIn, setLoggedIn] = useState(false);
  const [userId, setUserId] = useState<string>('');

  const [courses, setCourses] = useState<Course[]>([]);
  const [allCourses, setAllCourses] = useState<Course[]>([]);
  const [showCourseCenter, setShowCourseCenter] = useState(false);
  const [loadingCourses, setLoadingCourses] = useState(true);
  const [activeCourse, setActiveCourse] = useState<Course | null>(null);
  const [quizzes, setQuizzes] = useState<Quiz[]>([]);
  const [loadingQuizzes, setLoadingQuizzes] = useState(false);
  const [answers, setAnswers] = useState<Record<string, AnswerState>>({});
  const [toast, setToast] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  // 方案④：后端自渲染的课堂视图（凭证留后端，前端只拿 entryId 与渲染数据）
  const [classroom, setClassroom] = useState<ClassroomView | null>(null);
  const [classroomJoin, setClassroomJoin] = useState<JoinLessonResult | null>(null);
  const [classroomLoading, setClassroomLoading] = useState(false);
  // 免扫码进班：在读学生凭登录态建立官方课堂会话的结果（按课程缓存展示）
  const [enterResult, setEnterResult] = useState<Record<string, LessonEnterResult>>({});
  const [enteringId, setEnteringId] = useState<string | null>(null);

  const showToast = (msg: string) => {
    setToast(msg);
    window.setTimeout(() => setToast(null), 2500);
  };

  // 启动时校验本地会话；无 token 或失效 → 展示登录页
  useEffect(() => {
    let active = true;
    (async () => {
      if (!getToken()) {
        if (active) { setAuthChecking(false); setLoggedIn(false); }
        return;
      }
      try {
        // 静默续签：临近过期自动续期，token 失效但有 sessionid 则无感重导
        const alive = await keepAlive();
        if (!active) return;
        if (alive) {
          const info = await checkSession();
          setUserId(info.userId);
          setLoggedIn(true);
        } else {
          clearToken();
          setLoggedIn(false);
        }
      } catch {
        if (active) setLoggedIn(false);
      } finally {
        if (active) setAuthChecking(false);
      }
    })();
    return () => { active = false; };
  }, []);

  // 刚登录的时间：短时间内业务请求若返回 401，先重试而不是立刻把用户踢回二维码页
  const loggedInAt = useRef(0);
  const handleLogin = (id: string, _mock: boolean) => {
    loggedInAt.current = Date.now();
    setUserId(id);
    setLoggedIn(true);
  };

  const handleLogout = async () => {
    try { await logout(); } catch { /* 忽略网络错误，本地清理即可 */ }
    clearToken();
    setLoggedIn(false);
    setUserId('');
    setActiveCourse(null);
    setQuizzes([]);
    setClassroom(null);
    setClassroomJoin(null);
    showToast('已退出登录');
  };

  const loadCourses = useCallback(async (silent = false) => {
    if (!silent) { setLoadingCourses(true); setError(null); }
    try {
      const data = await fetchCourses();
      setCourses(data.liveCourses);
      setAllCourses(data.allCourses);
    } catch (e) {
      const code = (e as Error & { code?: number }).code;
      if (code === 401) {
        // 登录成功后 10 秒内的 401 多为会话状态尚未同步；重试一次，
        // 避免"刚登录成功就被 loadCourses 弹回二维码页"（用户感知为扫码无反应）
        if (!silent && Date.now() - loggedInAt.current < 10_000) {
          window.setTimeout(() => { if (getToken()) void loadCourses(true); }, 800);
          return;
        }
        clearToken();
        setLoggedIn(false);
        return;
      }
      // 静默轮询失败不打断用户，也不覆盖已有课程列表
      if (!silent) setError(`无法连接后端：${(e as Error).message}。请确认后端服务可访问。`);
    } finally {
      if (!silent) setLoadingCourses(false);
    }
  }, []);

  useEffect(() => { if (loggedIn) void loadCourses(); }, [loggedIn, loadCourses]);

  // 每分钟自动询问一次上课状态（对齐小程序）
  useEffect(() => {
    if (!loggedIn) return;
    const timer = window.setInterval(() => { void loadCourses(true); }, 60 * 1000);
    return () => window.clearInterval(timer);
  }, [loggedIn, loadCourses]);

  // 鉴权校验中
  if (authChecking) {
    return (
      <div className="min-h-screen bg-[#F2F2F7] flex items-center justify-center">
        <Loader2 className="w-6 h-6 animate-spin text-[#007AFF]" />
      </div>
    );
  }

  // 未登录 → 扫码页
  if (!loggedIn) {
    return <LoginView onLogin={handleLogin} />;
  }

  const enterCourse = async (course: Course) => {
    setActiveCourse(course);
    setLoadingQuizzes(true);
    setError(null);
    try {
      // 随堂题：课件里已下发，倒计时结束老师未收题仍可补交
      const list = await fetchInClassQuizzes(course.courseId);
      setQuizzes(list);
      setAnswers(Object.fromEntries(list.map(q => [q.quizId, emptyAnswer()])));
      showToast(list.length ? `已获取 ${list.length} 道随堂题` : '当前课堂暂无题目');
    } catch (e) {
      setError(`加载题目失败：${(e as Error).message}`);
      setQuizzes([]);
    } finally {
      setLoadingQuizzes(false);
    }
  };

  /**
   * 进入课堂（技术说明 §7.2 ③）：原生端默认走官方在读学生固定入口 + 官方域 Cookie 注入
   * + App 内 WebView，真正建立官方课堂会话（无 checkin / 动态码）。
   * 打不开时降级为后端自渲染；Web 端无法注入官方域 Cookie，直接自渲染。
   */
  const openOfficialClassroom = async (course: Course) => {
    setClassroomLoading(true);
    try {
      const joined = await joinLesson(course.courseId, false);   // 不签到
      if (isNativeApp) {
        try {
          const ck = await fetchEntryCookie();
          await openOfficialClassroomSession({ url: joined.joinUrl, cookie: ck.cookie, domain: ck.domain });
          return;
        } catch (e) {
          showToast(`官方课堂页打开失败，已降级为自渲染：${(e as Error).message}`);
        }
      }
      const view = await fetchClassroomRender(joined.entryId);    // 零签到自渲染
      // 以历史课堂为目标：用全量课件覆盖实时渲染的少量页，未播放页也能翻到
      try {
        const full = await fetchAllCourseware(course.courseId, false);
        const chosen = full.lessons.find(l => l.activityId.startsWith('LIVE-')) ?? full.lessons[0];
        if (chosen?.presentations.length && chosen.presentations.some(p => p.slides.length > 0)) {
          view.presentations = chosen.presentations.map(p => ({
            id: p.id, title: p.title, cover: '', slidesCount: p.declaredCount,
            slides: p.slides.map(s => ({
              id: `p-${s.page}`, page: s.page, cover: s.imageUrl, problem: s.problem
            }))
          }));
          view.hint = `${view.hint} · 已按历史课堂方式载入全量课件：${chosen.title}，共 ${chosen.fetchedSlides} 页（含未播放）`
            + (chosen.warning ? `；${chosen.warning}` : '');
        }
      } catch (e) {
        view.hint = `${view.hint} · 全量课件暂不可用：${(e as Error).message}`;
      }
      setClassroomJoin(joined);
      setClassroom(view);
    } catch (e) {
      showToast(`进入课堂失败：${(e as Error).message}`);
    } finally {
      setClassroomLoading(false);
    }
  };

  /** 免扫码进班：在读学生凭登录态直接建立官方课堂会话（lesson_info_v2 → userAuth → WSS hello）。 */
  const enterClassroom = async (course: Course) => {
    setEnteringId(course.courseId);
    try {
      const result = await enterLesson(course.courseId);
      setEnterResult(prev => ({ ...prev, [course.courseId]: result }));
      showToast(result.entered ? '已免扫码进入课堂会话' : (result.note || '免扫码进班未成功'));
    } catch (e) {
      showToast(`免扫码进班失败：${(e as Error).message}`);
    } finally {
      setEnteringId(null);
    }
  };

  const patchAnswer = (quizId: string, patch: Partial<AnswerState>) => {
    setAnswers(prev => ({ ...prev, [quizId]: { ...(prev[quizId] ?? emptyAnswer()), ...patch } }));
  };

  const handleSubmit = async (quiz: Quiz) => {
    const state = answers[quiz.quizId] ?? emptyAnswer();
    const answer = quiz.questionType === 'SINGLE_CHOICE' || quiz.questionType === 'MULTIPLE_CHOICE'
      ? state.choices.join(',')
      : quiz.questionType === 'FILL_BLANK'
        ? state.blanks.join(',')
        : '';
    patchAnswer(quiz.quizId, { submitting: true });
    try {
      const result = activeCourse
        ? await submitInClassAnswer(activeCourse.courseId, quiz, answer)
        : await Promise.reject(new Error('课程上下文丢失，请返回重试'));
      patchAnswer(quiz.quizId, { submitting: false, uploaded: true, result });
      showToast(result.message);
    } catch (e) {
      patchAnswer(quiz.quizId, { submitting: false });
      showToast(`提交失败：${(e as Error).message}`);
    }
  };

  const handleSaveDraft = async (quiz: Quiz) => {
    const state = answers[quiz.quizId] ?? emptyAnswer();
    const answer = quiz.questionType === 'FILL_BLANK'
      ? state.blanks.join(',')
      : state.choices.join(',');
    try {
      await saveDraft(quiz.quizId, answer, state.images);
      showToast('草稿已保存（服务端暂存，正式提交前可修改）');
    } catch (e) {
      showToast(`草稿保存失败：${(e as Error).message}`);
    }
  };

  // ============ 课程中心（全部课程 → 课时 → 课件/题目）============
  if (showCourseCenter) {
    return <CourseCenter courses={allCourses} onBack={() => setShowCourseCenter(false)} />;
  }

  // ============ 方案④：后端自渲染课堂（不进官方页，无跨域 Cookie）============
  if (classroom) {
    return (
      <SelfRenderedClassroom
        view={classroom}
        join={classroomJoin}
        courses={allCourses}
        showToast={showToast}
        onBack={() => { setClassroom(null); setClassroomJoin(null); }}
      />
    );
  }

  // ============ 视图 1：当前上课中的课程 ============
  if (!activeCourse) {
    return (
      <div className="min-h-screen bg-[#F2F2F7] text-[#1C1C1E]">
        {toast && <Toast message={toast} />}
        <header className="bg-white/90 backdrop-blur-md border-b border-black/[0.06] sticky top-0 z-20">
          <div className="max-w-3xl mx-auto px-5 h-14 flex items-center gap-3">
            <div className="w-8 h-8 rounded-xl bg-[#007AFF] flex items-center justify-center text-white">
              <BookOpen className="w-4 h-4" />
            </div>
            <div className="flex-1 min-w-0">
              <h1 className="text-[15px] font-bold leading-tight">当前上课中</h1>
              <p className="text-[11px] text-[#8E8E93]">工小湖 · 超时补答</p>
            </div>
            <div className="flex items-center gap-1.5 text-[11px] text-[#8E8E93] max-w-[120px] truncate" title={userId}>
              <User className="w-3.5 h-3.5 shrink-0" />
              <span className="truncate">{userId}</span>
            </div>
            <button onClick={() => setShowCourseCenter(true)} title="课程中心" className="w-8 h-8 rounded-full bg-[#007AFF]/10 flex items-center justify-center text-[#007AFF] hover:bg-[#007AFF]/20">
              <Layers className="w-4 h-4" />
            </button>
            <button onClick={() => void loadCourses()} className="w-8 h-8 rounded-full bg-[#F2F2F7] flex items-center justify-center text-[#8E8E93] hover:bg-[#E5E5EA]">
              <RefreshCw className={`w-4 h-4 ${loadingCourses ? 'animate-spin' : ''}`} />
            </button>
            <button onClick={() => void handleLogout()} title="退出登录" className="w-8 h-8 rounded-full bg-[#F2F2F7] flex items-center justify-center text-[#8E8E93] hover:bg-[#E5E5EA]">
              <LogOut className="w-4 h-4" />
            </button>
          </div>
        </header>

        <main className="max-w-3xl mx-auto px-5 py-6 space-y-4">
          {/* 课程中心入口 */}
          <button onClick={() => setShowCourseCenter(true)}
            className="w-full bg-gradient-to-r from-[#007AFF] to-[#5AC8FA] rounded-3xl p-4 text-left text-white shadow-sm active:scale-[0.99] transition-all flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-white/20 flex items-center justify-center shrink-0">
              <Layers className="w-5 h-5" />
            </div>
            <div className="flex-1 min-w-0">
              <h2 className="text-sm font-bold">课程中心</h2>
              <p className="text-[11px] text-white/80 mt-0.5">查看全部课程 · 课时课件 · 教师题目</p>
            </div>
            <ChevronRight className="w-4 h-4 shrink-0" />
          </button>

          <div className="flex items-center gap-2 text-xs text-[#8E8E93]">
            <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
            实时检测开课状态 · 共 {courses.length} 门进行中
          </div>

          {error && <ErrorBox message={error} onRetry={() => void loadCourses()} />}

          {loadingCourses ? (
            <div className="flex justify-center py-16"><Loader2 className="w-6 h-6 animate-spin text-[#007AFF]" /></div>
          ) : courses.length === 0 && !error ? (
            <div className="bg-white rounded-3xl p-10 text-center text-sm text-[#8E8E93]">当前没有正在上课的课程</div>
          ) : (
            courses.map(course => (
              <div key={course.courseId} className="bg-white rounded-3xl border border-black/[0.06] p-5 space-y-4 shadow-[0_1px_3px_rgba(0,0,0,0.02)]">
                <div className="flex items-start justify-between gap-3">
                  <div className="min-w-0">
                    <h2 className="text-lg font-bold text-[#1C1C1E]">{course.name}</h2>
                    <p className="text-xs text-[#8E8E93] mt-1">{course.teacher}</p>
                  </div>
                  <span className="px-2.5 py-1 rounded-full bg-emerald-50 text-emerald-600 text-[11px] font-semibold shrink-0">
                    上课中
                  </span>
                </div>
                <div className="flex flex-wrap gap-x-4 gap-y-1.5 text-xs text-[#3A3A3C]">
                  <span className="flex items-center gap-1"><MapPin className="w-3.5 h-3.5 text-[#8E8E93]" />{course.room}</span>
                  <span className="flex items-center gap-1"><Clock className="w-3.5 h-3.5 text-[#8E8E93]" />{course.timeRange}</span>
                  <span className="font-mono text-[#8E8E93]">放映至 P{course.currentSlide}/{course.totalSlides}</span>
                </div>
                <div className="flex gap-2">
                  <button
                    onClick={() => void enterCourse(course)}
                    className="flex-1 py-3 rounded-2xl bg-[#007AFF] text-white text-sm font-bold hover:bg-[#0066CC] active:scale-[0.99] transition-all flex items-center justify-center gap-1.5"
                  >
                    进入补答 <ChevronRight className="w-4 h-4" />
                  </button>
                  <button
                    onClick={() => void enterClassroom(course)}
                    disabled={enteringId === course.courseId}
                    title="在读学生凭登录态直接建立官方课堂会话（lesson_info_v2 → userAuth → WSS hello，无需扫码）"
                    className="px-4 py-3 rounded-2xl bg-emerald-50 border border-emerald-200 text-emerald-700 text-sm font-semibold hover:bg-emerald-100 active:scale-[0.99] transition-all flex items-center justify-center gap-1.5 disabled:opacity-50"
                  >
                    {enteringId === course.courseId ? <Loader2 className="w-4 h-4 animate-spin" /> : '免扫码进班'}
                  </button>
                  <button
                    onClick={() => void openOfficialClassroom(course)}
                    disabled={classroomLoading}
                    title={isNativeApp
                      ? '以在读学生身份进入官方课堂会话（App 内 WebView 注入 Cookie，无需扫码）'
                      : '读取课件与题目并自绘（Web 端无法注入官方域 Cookie）'}
                    className="px-4 py-3 rounded-2xl bg-white border border-[#007AFF]/30 text-[#007AFF] text-sm font-semibold hover:bg-[#007AFF]/5 active:scale-[0.99] transition-all flex items-center justify-center gap-1.5 disabled:opacity-50"
                  >
                    {classroomLoading ? <Loader2 className="w-4 h-4 animate-spin" /> : '进入课堂'}
                  </button>
                </div>
                {enterResult[course.courseId] && (
                  <div className={`rounded-2xl px-4 py-3 text-xs leading-relaxed border ${
                    enterResult[course.courseId].entered
                      ? 'bg-emerald-50 border-emerald-200 text-emerald-800'
                      : 'bg-amber-50 border-amber-200 text-amber-800'
                  }`}>
                    {enterResult[course.courseId].entered
                      ? <span className="flex items-center gap-1.5"><CheckCircle2 className="w-4 h-4" />{enterResult[course.courseId].note}</span>
                      : <span>{enterResult[course.courseId].note}</span>}
                  </div>
                )}
              </div>
            ))
          )}
        </main>
      </div>
    );
  }

  // ============ 视图 2：仅超时补答 ============
  return (
    <div className="min-h-screen bg-[#F2F2F7] text-[#1C1C1E]">
      {toast && <Toast message={toast} />}
      <header className="bg-white/90 backdrop-blur-md border-b border-black/[0.06] sticky top-0 z-20">
        <div className="max-w-3xl mx-auto px-4 h-14 flex items-center gap-2.5">
          <button
            onClick={() => { setActiveCourse(null); setQuizzes([]); }}
            className="w-8 h-8 rounded-full bg-[#F2F2F7] flex items-center justify-center text-[#1C1C1E] hover:bg-[#E5E5EA]"
          >
            <ArrowLeft className="w-4 h-4" />
          </button>
          <div className="flex-1 min-w-0">
            <h1 className="text-[15px] font-bold truncate">{activeCourse.name} · 随堂补答</h1>
            <p className="text-[11px] text-[#8E8E93]">随堂题已随课件下发，超时后老师未收题仍可提交</p>
          </div>
          <Wifi className="w-4 h-4 text-emerald-500" />
        </div>
      </header>

      <main className="max-w-3xl mx-auto px-4 sm:px-5 py-5 space-y-4">
        <div className="flex items-center justify-between">
          <span className="text-xs text-[#8E8E93]">随堂题目 {quizzes.length} 道</span>
          <span className="text-[11px] px-2.5 py-1 rounded-full font-medium text-amber-600 bg-amber-50">可补交</span>
        </div>

        <div className="rounded-2xl bg-blue-50 border border-blue-100 px-3.5 py-2.5 text-[12px] text-blue-700 leading-relaxed">
          这些是老师随课件下发的随堂题。倒计时结束但老师未收题时仍可提交，重复提交会覆盖此前答案。
        </div>

        {error && <ErrorBox message={error} onRetry={() => void enterCourse(activeCourse)} />}
        {loadingQuizzes ? (
          <div className="flex justify-center py-16"><Loader2 className="w-6 h-6 animate-spin text-[#007AFF]" /></div>
        ) : quizzes.length === 0 && !error ? (
          <div className="bg-white rounded-3xl p-10 text-center text-sm text-[#8E8E93]">该课程暂无可补交题目</div>
        ) : (
          quizzes.map(quiz => (
            <QuizCard
              key={quiz.quizId}
              quiz={quiz}
              state={answers[quiz.quizId] ?? emptyAnswer()}
              onUpdate={patch => patchAnswer(quiz.quizId, patch)}
              onSubmit={() => void handleSubmit(quiz)}
              onSaveDraft={() => void handleSaveDraft(quiz)}
            />
          ))
        )}
      </main>
    </div>
  );
}

/**
 * 方案④自渲染课堂：后端持 pptJwt 拉好课件与题目，前端自绘。
 * 全流程不进入官方课堂页、不注入 Cookie、不下发任何凭证。
 */
const SelfRenderedClassroom: React.FC<{
  view: ClassroomView;
  join: JoinLessonResult | null;
  courses: Course[];
  showToast: (msg: string) => void;
  onBack: () => void;
}> = ({ view, join, courses, showToast, onBack }) => {
  const [presIdx, setPresIdx] = React.useState(0);
  const [slideIdx, setSlideIdx] = React.useState(0);
  const [diagOpen, setDiagOpen] = React.useState(false);
  const [diag, setDiag] = React.useState<DiagnosticReport | null>(null);
  const [diagErr, setDiagErr] = React.useState('');
  const [probe, setProbe] = React.useState<LessonInfoProbe | null>(null);
  const [probeCourseId, setProbeCourseId] = React.useState('');
  const [probeErr, setProbeErr] = React.useState('');
  const [probing, setProbing] = React.useState(false);
  const runProbe = async (courseId: string) => {
    setProbing(true); setProbeErr(''); setProbe(null);
    try { setProbe(await probeLessonInfo(courseId)); }
    catch (e) { setProbeErr((e as Error).message); }
    finally { setProbing(false); }
  };
  const pres = view.presentations[presIdx];
  const slide = pres?.slides[slideIdx];

  // ===== 与历史课堂页一致的课件浏览能力（全屏/横竖屏/缩放/滚轮翻页/题目置顶）=====
  const [fullscreen, setFullscreen] = React.useState(false);
  const [landscape, setLandscape] = React.useState(false);
  const [zoom, setZoom] = React.useState(1);
  const [offset, setOffset] = React.useState({ x: 0, y: 0 });
  const [showProblems, setShowProblems] = React.useState(true);
  const [pageInput, setPageInput] = React.useState('');
  const pageInputFocused = React.useRef(false);
  const wheelLock = React.useRef(0);
  const pinch = React.useRef<{ dist: number; cx: number; cy: number; zoom: number; ox: number; oy: number } | null>(null);
  const lastTap = React.useRef(0);
  const slideThumbRef = React.useRef<HTMLDivElement | null>(null);
  const pptViewportRef = React.useRef<HTMLDivElement | null>(null);
  const slideCount = pres?.slides.length ?? 0;

  const quizItems: { presIdx: number; slideIdx: number; page: number; presTitle: string; problem: string }[] = [];
  view.presentations.forEach((p, pi) => p.slides.forEach((s, si) => {
    if (s.problem) quizItems.push({
      presIdx: pi, slideIdx: si, page: s.page, presTitle: p.title, problem: s.problem
    });
  }));

  const resetView = () => { setZoom(1); setOffset({ x: 0, y: 0 }); };
  const toggleFullscreen = () => { setFullscreen(v => !v); resetView(); };
  const gotoSlide = (i: number) => {
    setSlideIdx(i); resetView();
    const el = slideThumbRef.current;
    const child = el?.children[i] as HTMLElement | undefined;
    if (child) child.scrollIntoView({ behavior: 'smooth', block: 'nearest', inline: 'center' });
  };
  const jumpTo = (pi: number, si: number) => { setPresIdx(pi); gotoSlide(si); };
  const goToPage = () => {
    const target = Number(pageInput);
    if (!Number.isFinite(target) || !pres) { pageInputFocused.current = false; return; }
    const idx = pres.slides.findIndex(s => s.page === target);
    if (idx >= 0) gotoSlide(idx);
    else if (target >= 1 && target <= pres.slides.length) gotoSlide(target - 1);
    pageInputFocused.current = false;
  };
  React.useEffect(() => {
    if (!pageInputFocused.current) setPageInput(String(slide?.page ?? ''));
  }, [slide?.page]);
  React.useEffect(() => {
    const el = pptViewportRef.current;
    if (!el || slideCount < 2) return;
    const handler = (e: WheelEvent) => {
      e.preventDefault();
      const now = Date.now();
      if (now - wheelLock.current < 260 || Math.abs(e.deltaY) < 8) return;
      wheelLock.current = now;
      setSlideIdx(i => Math.max(0, Math.min(slideCount - 1, i + (e.deltaY > 0 ? 1 : -1))));
    };
    el.addEventListener('wheel', handler, { passive: false });
    return () => el.removeEventListener('wheel', handler);
  }, [slideCount, fullscreen]);
  const onTouchStart = (e: React.TouchEvent) => {
    if (e.touches.length === 2) {
      const a = e.touches[0], b = e.touches[1];
      pinch.current = {
        dist: Math.hypot(a.clientX - b.clientX, a.clientY - b.clientY),
        cx: (a.clientX + b.clientX) / 2, cy: (a.clientY + b.clientY) / 2,
        zoom, ox: offset.x, oy: offset.y
      };
    }
  };
  const onTouchMove = (e: React.TouchEvent) => {
    if (e.touches.length === 2 && pinch.current) {
      const a = e.touches[0], b = e.touches[1];
      const ratio = Math.hypot(a.clientX - b.clientX, a.clientY - b.clientY) / (pinch.current.dist || 1);
      let z = Math.max(1, Math.min(4, pinch.current.zoom * ratio));
      let ox = pinch.current.ox + ((a.clientX + b.clientX) / 2 - pinch.current.cx);
      let oy = pinch.current.oy + ((a.clientY + b.clientY) / 2 - pinch.current.cy);
      if (z <= 1) { ox = 0; oy = 0; }
      setZoom(z); setOffset({ x: ox, y: oy });
    }
  };
  const onTouchEnd = (e: React.TouchEvent) => {
    if (e.touches.length < 2) {
      pinch.current = null;
      if (zoom <= 1.01 && (offset.x || offset.y)) resetView();
    }
  };
  const onPptTap = () => {
    const now = Date.now();
    if (now - lastTap.current < 350) { lastTap.current = 0; toggleFullscreen(); }
    else lastTap.current = now;
  };

  const loadDiag = async () => {
    setDiagErr('');
    try { setDiag(await fetchDiagnostics()); }
    catch (e) { setDiagErr((e as Error).message); }
  };
  React.useEffect(() => { if (diagOpen) void loadDiag(); }, [diagOpen]);

  return (
    <div className="min-h-screen bg-[#F2F2F7] text-[#1C1C1E]">
      <header className="bg-white/90 backdrop-blur-md border-b border-black/[0.06] sticky top-0 z-20">
        <div className="max-w-4xl mx-auto px-5 h-14 flex items-center gap-3">
          <button onClick={onBack} className="w-8 h-8 rounded-full bg-[#F2F2F7] flex items-center justify-center text-[#8E8E93]">
            <ArrowLeft className="w-4 h-4" />
          </button>
          <div className="flex-1 min-w-0">
            <h1 className="text-[15px] font-bold leading-tight truncate">{view.courseName || '课堂'}</h1>
            <p className="text-[11px] text-[#8E8E93]">工小湖 · 课堂自渲染</p>
          </div>
          <span className={`px-2 py-1 rounded-full text-[10px] font-semibold shrink-0 ${
            view.credentialReady ? 'bg-emerald-50 text-emerald-600' : 'bg-amber-50 text-amber-600'}`}>
            {view.credentialReady ? '凭证就绪' : '降级模式'}
          </span>
        </div>
      </header>

      <main className="max-w-4xl mx-auto px-4 py-5 space-y-4">
        <div className="bg-white rounded-2xl px-4 py-3 text-xs text-[#3A3A3C] flex items-start gap-2">
          <CheckCircle2 className="w-4 h-4 text-emerald-500 shrink-0 mt-0.5" />
          <span>{view.hint}</span>
        </div>

        {/* ===== 测验区：与历史课堂一致，课件内嵌题目置顶且可跳页 ===== */}
        {quizItems.length > 0 && !fullscreen && (
          <section className="bg-white rounded-3xl border border-orange-100 overflow-hidden shadow-[0_1px_3px_rgba(0,0,0,0.02)] mb-4">
            <button onClick={() => setShowProblems(v => !v)}
              className="w-full px-4 py-3 flex items-center justify-between bg-orange-50/70 hover:bg-orange-50 transition-colors">
              <div className="flex items-center gap-2">
                <span className="w-7 h-7 rounded-xl bg-orange-100 flex items-center justify-center">
                  <FileQuestion className="w-4 h-4 text-orange-600" />
                </span>
                <div className="text-left">
                  <h2 className="text-sm font-bold text-[#1C1C1E]">教师题目 · 测验区</h2>
                  <p className="text-[10px] text-orange-600">共 {quizItems.length} 道 · 点击题目跳转对应课件页</p>
                </div>
              </div>
              <ChevronDown className={`w-4 h-4 text-orange-500 transition-transform ${showProblems ? '' : '-rotate-90'}`} />
            </button>
            {showProblems && (
              <div className="p-3 space-y-2.5 max-h-[420px] overflow-y-auto">
                {quizItems.map((q, i) => {
                  const pv: ProblemView | null = parseProblem(q.problem);
                  const active = q.presIdx === presIdx && q.slideIdx === slideIdx;
                  return (
                    <button key={i} onClick={() => jumpTo(q.presIdx, q.slideIdx)}
                      className={`w-full text-left rounded-2xl border p-3 transition-all ${
                        active ? 'border-orange-300 bg-orange-50/40' : 'border-black/[0.06] hover:bg-gray-50'}`}>
                      <div className="flex items-center gap-2 mb-1.5">
                        <span className="px-2 py-0.5 rounded-full bg-orange-100 text-orange-700 text-[10px] font-semibold">
                          题 {i + 1}
                        </span>
                        <span className="text-[10px] text-[#8E8E93]">{q.presTitle} · P{q.page}</span>
                      </div>
                      {pv && <ProblemCard problem={pv} compact />}
                    </button>
                  );
                })}
              </div>
            )}
          </section>
        )}

        {/* ===== 课件区：与历史课堂同款切换栏 + 全屏舞台 + 下方缩略图 ===== */}
        <div className={`flex items-center justify-between gap-2 mb-3 flex-wrap ${fullscreen ? 'hidden' : ''}`}>
          <div className="flex items-center gap-1.5 flex-wrap">
            {view.presentations.map((p, i) => (
              <button key={p.id || i} onClick={() => { setPresIdx(i); gotoSlide(0); }}
                className={`px-3 py-1.5 rounded-xl text-xs font-medium flex items-center gap-1.5 transition-all ${
                  i === presIdx ? 'bg-[#007AFF] text-white' : 'bg-white text-[#3A3A3C] border border-black/[0.06]'}`}>
                <Layers className="w-3.5 h-3.5" /> {p.title} ({p.slidesCount}页)
              </button>
            ))}
          </div>
          <span className="text-[10px] text-[#8E8E93] flex items-center gap-1">
            <MousePointer2 className="w-3 h-3" /> 滑动/滚轮翻页 · 双击全屏
          </span>
        </div>

        {pres && slide ? (
          <>
            {fullscreen && <div className="fixed inset-0 bg-black z-[1000]" />}
            <div className={`${fullscreen ? 'fixed z-[1001] bg-black' : 'bg-white rounded-3xl border border-black/[0.06] overflow-hidden shadow-[0_1px_3px_rgba(0,0,0,0.02)]'} ${
              fullscreen ? 'inset-0 flex items-center justify-center' : ''}`}>
              <div className={`flex flex-col w-full ${fullscreen ? 'h-full' : ''} ${
                landscape && fullscreen ? 'rotate-90 origin-center' : ''}`}
                style={fullscreen && landscape
                  ? { width: '100vh', height: '100vw', transform: 'rotate(90deg) translate(50%, -50%)', position: 'absolute', top: '50%', left: '50%' }
                  : undefined}>
                <div className={`flex items-center gap-2 ${fullscreen ? 'absolute top-3 right-3 z-20' : 'justify-end p-2'}`}>
                  <button onClick={toggleFullscreen}
                    className={`w-10 h-10 rounded-full flex items-center justify-center ${fullscreen ? 'bg-black/50 text-white' : 'bg-[#007AFF] text-white'}`}
                    title={fullscreen ? '退出全屏' : '全屏'}>
                    {fullscreen ? <Minimize2 className="w-5 h-5" /> : <Maximize2 className="w-5 h-5" />}
                  </button>
                  {fullscreen && (
                    <button onClick={() => setLandscape(v => !v)}
                      className="w-10 h-10 rounded-full flex items-center justify-center bg-black/50 text-white"
                      title={landscape ? '竖屏' : '横屏'}>
                      {landscape ? <Smartphone className="w-5 h-5" /> : <Monitor className="w-5 h-5" />}
                    </button>
                  )}
                </div>

                <div ref={pptViewportRef}
                  onClick={onPptTap}
                  onTouchStart={onTouchStart} onTouchMove={onTouchMove} onTouchEnd={onTouchEnd}
                  className={`w-full bg-slate-50 flex items-center justify-center overflow-hidden select-none ${fullscreen ? 'flex-1' : 'min-h-[300px] cursor-ns-resize'}`}>
                  {slide.cover ? (
                    <img src={slide.cover} alt={`第 ${slide.page} 页`}
                      className="w-full h-auto object-contain max-h-[70vh] pointer-events-none"
                      style={{ transform: `translate(${offset.x}px, ${offset.y}px) scale(${zoom})`, transformOrigin: 'center center' }}
                      referrerPolicy="no-referrer" />
                  ) : (
                    <div className="flex flex-col items-center gap-2 text-[#AEAEB2] py-16">
                      <Maximize2 className="w-10 h-10" />
                      <span className="text-xs">该页暂无图片（可能尚未同步），可翻到其他页</span>
                    </div>
                  )}
                </div>

                <div className={`px-4 py-2.5 flex items-center justify-between gap-2 ${
                  fullscreen ? 'bg-black/55 absolute bottom-0 left-0 right-0 text-white' : 'border-t border-black/[0.06] bg-[#FAFAFA]'}`}>
                  <button onClick={() => gotoSlide(Math.max(0, slideIdx - 1))} disabled={slideIdx === 0}
                    className="px-3 py-1.5 rounded-xl bg-white/90 border border-black/[0.06] text-xs disabled:opacity-40 text-[#1C1C1E]">
                    ‹ 上一页
                  </button>
                  <div className="flex items-center gap-1.5">
                    <input type="number" value={pageInput}
                      onFocus={() => { pageInputFocused.current = true; }}
                      onChange={e => setPageInput(e.target.value)}
                      onBlur={() => { goToPage(); }}
                      onKeyDown={e => { if (e.key === 'Enter') (e.target as HTMLInputElement).blur(); }}
                      className="w-14 text-center text-xs rounded-lg border border-black/10 bg-white/90 text-[#1C1C1E] px-1 py-1" />
                    <span className="text-xs opacity-80">/ {pres.slides.length} 页</span>
                  </div>
                  <button onClick={() => gotoSlide(Math.min(pres.slides.length - 1, slideIdx + 1))}
                    disabled={slideIdx >= pres.slides.length - 1}
                    className="px-3 py-1.5 rounded-xl bg-white/90 border border-black/[0.06] text-xs disabled:opacity-40 text-[#1C1C1E]">
                    下一页 ›
                  </button>
                </div>
              </div>
            </div>

            {!fullscreen && (
              <div ref={slideThumbRef} className="flex gap-2 overflow-x-auto mt-3 pb-2 px-1">
                {pres.slides.map((s, i) => (
                  <button key={s.id || i} onClick={() => gotoSlide(i)}
                    className={`shrink-0 w-[100px] rounded-lg overflow-hidden border-2 transition-all relative ${
                      i === slideIdx ? 'border-[#007AFF]' : 'border-transparent hover:border-[#C7C7CC]'}`}>
                    {s.cover ? (
                      <img src={s.cover} alt={`P${s.page}`} className="w-full h-[56px] object-cover bg-white" loading="lazy" referrerPolicy="no-referrer" />
                    ) : (
                      <div className="w-full h-[56px] bg-slate-100 flex items-center justify-center">
                        <ImageIcon className="w-4 h-4 text-[#C7C7CC]" />
                      </div>
                    )}
                    <span className="absolute bottom-0 left-0 right-0 bg-black/55 text-white text-[9px] py-0.5 text-center">{s.page}</span>
                    {s.problem && <span className="absolute top-0 right-0 w-2 h-2 rounded-full bg-orange-500" title="含题目" />}
                  </button>
                ))}
              </div>
            )}
          </>
        ) : (
          <div className="bg-white rounded-3xl p-10 text-center text-sm text-[#8E8E93]">该课件暂无页面</div>
        )}

        {/* 实时随堂题（后端接口下发，非课件内嵌题） */}
        {view.quizzes.length > 0 && (
          <section className="space-y-2">
            <h2 className="text-sm font-bold px-1">实时随堂题（{view.quizzes.length}）</h2>
            {view.quizzes.map(q => (
              <div key={q.quizId} className="bg-white rounded-2xl border border-black/[0.06] p-4 space-y-2">
                <div className="flex items-center gap-2">
                  <span className="px-2 py-0.5 rounded-full bg-orange-100 text-orange-700 text-[10px] font-semibold">{q.type}</span>
                  <span className="text-[10px] text-[#8E8E93]">{q.state}</span>
                </div>
                <p className="text-sm leading-relaxed">{q.question}</p>
                {q.options.length > 0 && (
                  <ul className="space-y-1">
                    {q.options.map((o, i) => (
                      <li key={i} className="text-xs text-[#3A3A3C] bg-[#F9F9FB] rounded-lg px-2.5 py-1.5">{o}</li>
                    ))}
                  </ul>
                )}
              </div>
            ))}
          </section>
        )}

        {/* 验证诊断（§6.7）：真实开课时展开即可看到 checkin 全套证据，无需抓包 */}
        <section className="bg-white rounded-2xl border border-black/[0.06] overflow-hidden">
          <button onClick={() => setDiagOpen(v => !v)}
            className="w-full px-4 py-3 flex items-center justify-between text-xs font-semibold hover:bg-[#F9F9FB]">
            <span className="flex items-center gap-2">
              <Wifi className="w-4 h-4 text-[#007AFF]" /> 验证诊断 · checkin 证据（§6.7）
            </span>
            <span className="text-[#8E8E93]">{diagOpen ? '收起' : '展开'}</span>
          </button>
          {diagOpen && (
            <div className="px-4 pb-4 space-y-3 border-t border-black/[0.06] pt-3">
              <div className="flex items-center justify-between">
                <span className="text-[10px] text-[#8E8E93]">
                  {diag ? `共 ${diag.totalAttempts} 次尝试` : '加载中…'}
                </span>
                <div className="flex gap-2">
                  <button onClick={() => void loadDiag()} className="text-[10px] text-[#007AFF]">刷新</button>
                  <button onClick={async () => { await clearDiagnostics(); await loadDiag(); }}
                    className="text-[10px] text-[#8E8E93]">清空</button>
                </div>
              </div>
              {diagErr && <p className="text-[11px] text-red-600">{diagErr}</p>}
              <div className="bg-[#F2F2F7] rounded-xl p-3 space-y-1.5">
                <p className="text-[11px] font-semibold text-[#1C1C1E]">进班探测 · lesson_info_v2（官方课堂页自己发的请求）</p>
                <p className="text-[10px] text-[#3A3A3C] leading-relaxed">
                  在真实开课时选一门课探测。只读，不调用 checkin，返回业务码与字段有无，不含任何凭据。
                </p>
                <select
                  className="w-full text-[11px] rounded-lg border border-black/[0.08] bg-white px-2 py-1.5"
                  value={probeCourseId}
                  onChange={e => setProbeCourseId(e.target.value)}>
                  <option value="">选择课程…</option>
                  {(courses ?? []).map(c => (
                    <option key={c.courseId} value={c.courseId}>{c.name}</option>
                  ))}
                </select>
                <button onClick={() => void runProbe(probeCourseId)} disabled={!probeCourseId || probing}
                  className="w-full py-2 rounded-xl bg-[#007AFF] text-white text-[11px] font-semibold disabled:opacity-40">
                  {probing ? '探测中…' : '开始探测'}
                </button>
                {probeErr && <p className="text-[11px] text-red-600">{probeErr}</p>}
                {probe && (
                  <div className="text-[11px] space-y-0.5">
                    <p>HTTP {probe.httpStatus} · code {probe.code}{probe.msg ? ` · ${probe.msg}` : ''}</p>
                    <p>课堂凭据 userAuth：<b className={probe.hasUserAuth ? 'text-emerald-600' : 'text-amber-600'}>
                      {probe.hasUserAuth ? '已返回' : '无'}</b></p>
                    <p>课件 {probe.presentationCount} 个 · 题目 {probe.quizCount} 道</p>
                    <p className="text-[#3A3A3C]">{probe.note}</p>
                  </div>
                )}
              </div>
              {diag && diag.totalAttempts === 0 && (
                <p className="text-[11px] text-[#8E8E93] leading-relaxed">
                  自渲染主方案不调用 checkin，因此没有诊断记录是正常且符合预期的。
                  诊断面板保留用于排查可选的实时题增强链路。
                </p>
              )}
              {diag && (
                <>
                  <div className="bg-[#F2F2F7] rounded-xl p-3 space-y-1.5">
                    {([
                      ['① 请求头清单', diag.checklist.checkinHeadersCaptured],
                      ['② Set-Auth 证据', diag.checklist.setAuthCaptured],
                      ['③ 三元组齐全', diag.checklist.tripleCaptured],
                      ['④ socketJwt 存在', diag.checklist.websocketHintCaptured],
                      ['⑤ get-token 归属', diag.checklist.getTokenKnown]
                    ] as const).map(([label, ok]) => (
                      <div key={label} className="flex items-center gap-2 text-[11px]">
                        <span className={ok ? 'text-emerald-600' : 'text-[#AEAEB2]'}>{ok ? '✓' : '○'}</span>
                        <span className={ok ? 'text-[#1C1C1E]' : 'text-[#8E8E93]'}>{label}</span>
                      </div>
                    ))}
                    <p className="text-[10px] text-[#3A3A3C] pt-1 leading-relaxed">{diag.checklist.note}</p>
                  </div>
                  <p className="text-[10px] text-[#AEAEB2]">{diag.privacyNote}</p>
                  {diag.records.length > 0 && (
                    <div className="space-y-2 max-h-[300px] overflow-y-auto">
                      {[...diag.records].reverse().map((r, i) => (
                        <div key={i} className="bg-[#FAFAFA] rounded-xl p-2.5 space-y-1">
                          <div className="flex items-center justify-between text-[10px]">
                            <span className="font-mono">{r.time} · source={r.source}</span>
                            <span className={r.outcome === 'SUCCESS'
                              ? 'text-emerald-600 font-semibold' : 'text-amber-600 font-semibold'}>{r.outcome}</span>
                          </div>
                          <p className="text-[10px] text-[#3A3A3C] break-all">
                            HTTP {r.httpStatus} · code {r.bizCode} · {r.bizMessage || '—'} · {r.elapsedMs}ms
                          </p>
                          <p className="text-[10px] text-[#3A3A3C]">
                            Set-Auth: {r.setAuthPresent ? r.setAuthFingerprint : '无'} ·
                            socketJwt: {r.socketJwtPresent ? '有' : '无'} ·
                            identityId: {r.identityIdPresent ? '有' : '无'}
                          </p>
                          <p className="text-[10px] text-[#8E8E93] break-all">
                            头 {Object.entries(r.requestHeaders).map(([k, v]) => `${k}=${v}`).join(' · ')}
                          </p>
                          {r.missingHeaders.length > 0 && (
                            <p className="text-[10px] text-amber-700">缺失头：{r.missingHeaders.join(', ')}</p>
                          )}
                        </div>
                      ))}
                    </div>
                  )}
                </>
              )}
            </div>
          )}
        </section>

        {/* 降级后仍可重试：在读学生固定入口（无 sign/ts/动态码），需官方域 Cookie */}
        {join?.joinUrl && (
          <>
            {isNativeApp && (
              <button onClick={async () => {
                try {
                  const ck = await fetchEntryCookie();
                  await openOfficialClassroomSession({
                    url: join.joinUrl, cookie: ck.cookie, domain: ck.domain
                  });
                } catch (e) { showToast(`打开官方课堂失败：${(e as Error).message}`); }
              }}
                className="w-full py-3 rounded-2xl bg-[#007AFF] text-white text-xs font-semibold">
                重试官方课堂页（App 内 WebView 注入 Cookie）
              </button>
            )}
            <button onClick={() => window.open(join.joinUrl, '_blank', 'noopener,noreferrer')}
              className="w-full py-3 rounded-2xl bg-white border border-[#007AFF]/30 text-[#007AFF] text-xs font-semibold">
              在浏览器打开官方课堂页 ↗（需该浏览器已登录雨课堂）
            </button>
          </>
        )}
      </main>
    </div>
  );
};

const Toast: React.FC<{ message: string }> = ({ message }) => (
  <div className="fixed top-16 left-1/2 -translate-x-1/2 z-50 bg-[#1C1C1E]/90 text-white text-xs px-4 py-2.5 rounded-full shadow-lg flex items-center gap-1.5 whitespace-nowrap backdrop-blur-md">
    <CheckCircle2 className="w-3.5 h-3.5 text-emerald-400" />{message}
  </div>
);

const ErrorBox: React.FC<{ message: string; onRetry: () => void }> = ({ message, onRetry }) => (
  <div className="bg-red-50 border border-red-100 rounded-3xl p-4 space-y-2">
    <p className="text-xs text-red-700 leading-relaxed">{message}</p>
    <button onClick={onRetry} className="text-xs font-semibold text-red-600 flex items-center gap-1">
      <RefreshCw className="w-3.5 h-3.5" /> 重试
    </button>
  </div>
);
