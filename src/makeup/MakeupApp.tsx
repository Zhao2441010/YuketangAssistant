import React, { useCallback, useEffect, useState } from 'react';
import {
  ArrowLeft, BookOpen, Check, CheckCircle2, ChevronRight, Clock,
  Layers, Loader2, LogOut, MapPin, Plus, RefreshCw, Send, User, Wifi, X
} from 'lucide-react';
import {
  Course, Quiz, SubmitResult, checkSession, clearToken, fetchCourses,
  fetchInClassQuizzes, getToken, imageUrl, joinLesson, keepAlive, logout, saveDraft, submitInClassAnswer, uploadImage
} from './api';
import LoginView from './LoginView';
import CourseCenter from './CourseCenter';

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

  const handleLogin = (id: string, _mock: boolean) => {
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

  /** 一键进班：按博客第 1 节用 session_key 换官方进班链接并打开课堂页。
   *  Web 端跨域无法注入 sessionid cookie——若浏览器已在雨课堂登录则直接进班，否则提示。 */
  const openOfficialClassroom = async (course: Course) => {
    try {
      const result = await joinLesson(course.courseId);
      window.open(result.joinUrl, '_blank', 'noopener,noreferrer');
      showToast('已打开官方课堂页；若未自动进班请先在雨课堂网页登录');
    } catch (e) {
      showToast(`打开课堂失败：${(e as Error).message}`);
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
                    onClick={() => void openOfficialClassroom(course)}
                    title="用 session_key 换取官方进班链接并打开课堂"
                    className="px-4 py-3 rounded-2xl bg-white border border-[#007AFF]/30 text-[#007AFF] text-sm font-semibold hover:bg-[#007AFF]/5 active:scale-[0.99] transition-all flex items-center justify-center gap-1.5"
                  >
                    官方课堂 ↗
                  </button>
                </div>
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
