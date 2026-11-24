import React, { useEffect, useRef, useState } from 'react';
import {
  ArrowLeft, BookOpen, CheckCircle2, ChevronDown, ChevronRight,
  Clock, FileQuestion, Image as ImageIcon, Layers, Loader2, Maximize2, Minimize2,
  MousePointer2, Presentation as PresentationIcon, Smartphone, Monitor
} from 'lucide-react';
import {
  Activity, Course, Presentation, Slide, fetchActivities, fetchSlides
} from './api';

interface Props {
  courses: Course[];        // 全部课程（来自课表）
  onBack: () => void;
}

type ProblemView = {
  body?: string;
  type?: string;
  problemType?: number;
  options?: { key?: string; value?: string }[] | string[];
  answer?: string | string[];
  score?: number;
  content?: ProblemView;   // 真实结构：题目正文嵌在 content 里
};

/** 课程中心：全部课程 → 课程课时列表 → 课件 PPT 翻页 + 教师题目。 */
export default function CourseCenter({ courses, onBack }: Props) {
  const [view, setView] = useState<'courses' | 'activities' | 'slides'>('courses');
  const [selectedCourse, setSelectedCourse] = useState<Course | null>(null);
  const [selectedLesson, setSelectedLesson] = useState<Activity | null>(null);
  const [activities, setActivities] = useState<Activity[]>([]);
  const [presentations, setPresentations] = useState<Presentation[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [presIndex, setPresIndex] = useState(0);
  const [slideIndex, setSlideIndex] = useState(0);
  const [showProblems, setShowProblems] = useState(true);  // 题目区是否展开（置顶）
  const [pageInput, setPageInput] = useState('');          // 页码输入框
  const pageInputFocused = useRef(false);                  // 输入中不被翻页覆盖
  const wheelLock = useRef(0);                             // 滚轮翻页节流
  // ===== 全屏 / 横竖屏 / 缩放（对齐小程序课件页）=====
  const [fullscreen, setFullscreen] = useState(false);
  const [landscape, setLandscape] = useState(false);
  const [zoom, setZoom] = useState(1);
  const [offset, setOffset] = useState({ x: 0, y: 0 });
  const pinch = useRef<{ dist: number; cx: number; cy: number; zoom: number; ox: number; oy: number } | null>(null);
  const lastTap = useRef(0);
  const slideThumbRef = useRef<HTMLDivElement | null>(null);

  const resetView = () => { setZoom(1); setOffset({ x: 0, y: 0 }); };
  const toggleFullscreen = () => { setFullscreen(v => !v); resetView(); };
  // CSS 旋转模拟横屏（WebView 内比 ScreenOrientation 插件更可靠）
  const toggleOrientation = () => setLandscape(v => !v);

  // 双指缩放 + 平移；单指/滚轮留给翻页
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
  // 双击切换全屏
  const onPptTap = () => {
    const now = Date.now();
    if (now - lastTap.current < 350) { lastTap.current = 0; toggleFullscreen(); }
    else lastTap.current = now;
  };
  // 翻页时重置视图
  const gotoSlide = (i: number) => {
    setSlideIndex(i); resetView();
    // 缩略图滚到可视区
    const el = slideThumbRef.current;
    if (el) {
      const child = el.children[i] as HTMLElement | undefined;
      if (child) child.scrollIntoView({ behavior: 'smooth', block: 'nearest', inline: 'center' });
    }
  };

  const enterCourse = async (course: Course) => {
    setSelectedCourse(course);
    setView('activities');
    setLoading(true); setError(null);
    try {
      setActivities(await fetchActivities(course.courseId));
    } catch (e) {
      setError((e as Error).message); setActivities([]);
    } finally { setLoading(false); }
  };

  const enterLesson = async (activity: Activity) => {
    setSelectedLesson(activity);
    setView('slides');
    setLoading(true); setError(null); setPresIndex(0); setSlideIndex(0); setShowProblems(true);
    try {
      // 历史课件必须用 courseware_id，用 activity.id 会返回 LESSON_DELETED
      setPresentations(await fetchSlides(activity.coursewareId || activity.activityId));
    } catch (e) {
      setError((e as Error).message); setPresentations([]);
    } finally { setLoading(false); }
  };

  const backTo = (target: 'courses' | 'activities') => {
    setError(null);
    setView(target);
  };

  const currentPres = presentations[presIndex];
  const current = currentPres?.slides[slideIndex];

  // 课件与测验解耦：把所有课件里的教师题目抽出来，置顶集中展示（已结束/进行中课堂一致）
  const quizItems: { presIdx: number; slideIdx: number; page: number; presTitle: string; problem: string }[] = [];
  presentations.forEach((p, pi) => p.slides.forEach((s, si) => {
    if (s.problem) quizItems.push({ presIdx: pi, slideIdx: si, page: s.page, presTitle: p.title, problem: s.problem });
  }));

  // 点击题目/缩略图跳转到指定课件页
  const jumpTo = (presIdx: number, slideIdx: number) => {
    setPresIndex(presIdx);
    gotoSlide(slideIdx);
  };

  // 滚轮翻页：用原生监听（passive:false）才能 preventDefault 阻止浏览器页面滚动。
  // 仅在 PPT 主图区域生效，侧边缩略图/测验区/页面其他区域滚动不受影响。
  const pptViewportRef = useRef<HTMLDivElement | null>(null);
  const slideCount = currentPres?.slides.length ?? 0;
  useEffect(() => {
    const el = pptViewportRef.current;
    if (!el) return;
    const onWheel = (e: WheelEvent) => {
      if (slideCount < 2) return;              // 不足 2 页不拦截，保持页面正常滚动
      e.preventDefault();                      // 关键：阻止浏览器页面滚动
      const now = Date.now();
      if (now - wheelLock.current < 260) return; // 节流：一次滚动只翻一页
      if (Math.abs(e.deltaY) < 8) return;
      setSlideIndex(i => {
        const next = Math.max(0, Math.min(slideCount - 1, i + (e.deltaY > 0 ? 1 : -1)));
        return next;
      });
      setZoom(1); setOffset({ x: 0, y: 0 });
      wheelLock.current = now;
    };
    el.addEventListener('wheel', onWheel, { passive: false });
    return () => el.removeEventListener('wheel', onWheel);
  }, [slideCount]);

  // 页码输入框默认显示当前页（用户正在输入时不覆盖）
  useEffect(() => {
    if (!pageInputFocused.current && current) setPageInput(String(current.page));
  }, [current?.page, presIndex]);

  // 输入页码并回车跳转到对应页
  const goToPage = () => {
    if (!currentPres) return;
    const target = Number(pageInput.trim());
    if (!Number.isFinite(target) || target < 1) return;
    const idx = currentPres.slides.findIndex(s => s.page === target);
    if (idx >= 0) setSlideIndex(idx);          // 优先按真实页码匹配
    else if (target <= currentPres.slides.length) setSlideIndex(target - 1); // 退化为按序号
    pageInputFocused.current = false;
  };

  const parseProblem = (raw: string | null): ProblemView | null => {
    if (!raw) return null;
    try {
      const parsed = JSON.parse(raw) as ProblemView;
      // 真实历史课件：problem.content 才是题目正文（含 body/options/answer）
      return parsed.content ? { ...parsed.content, problemType: parsed.content.problemType ?? parsed.problemType } : parsed;
    } catch { return { body: raw }; }
  };

  // ============ 视图 1：全部课程 ============
  if (view === 'courses') {
    return (
      <div className="min-h-screen bg-[#F2F2F7] text-[#1C1C1E]">
        <Header title="课程中心" subtitle="选择课程查看课时与课件" onBack={onBack} />
        <main className="max-w-3xl mx-auto px-5 py-6 space-y-3">
          {courses.length === 0 ? (
            <div className="bg-white rounded-3xl p-10 text-center text-sm text-[#8E8E93]">暂无课程</div>
          ) : courses.map(course => (
            <button key={course.courseId} onClick={() => void enterCourse(course)}
              className="w-full bg-white rounded-3xl border border-black/[0.06] p-5 text-left shadow-[0_1px_3px_rgba(0,0,0,0.02)] hover:bg-gray-50 active:scale-[0.99] transition-all flex items-center gap-4">
              <div className="w-11 h-11 rounded-2xl bg-[#007AFF]/10 flex items-center justify-center shrink-0">
                <BookOpen className="w-5 h-5 text-[#007AFF]" />
              </div>
              <div className="flex-1 min-w-0">
                <h2 className="text-[15px] font-bold truncate">{course.name}</h2>
                <p className="text-xs text-[#8E8E93] mt-0.5 truncate">{course.teacher} · {course.timeRange}</p>
              </div>
              {course.status === 1 && (
                <span className="px-2 py-0.5 rounded-full bg-emerald-50 text-emerald-600 text-[10px] font-semibold shrink-0">上课中</span>
              )}
              <ChevronRight className="w-4 h-4 text-[#C7C7CC] shrink-0" />
            </button>
          ))}
        </main>
      </div>
    );
  }

  // ============ 视图 2：课程课时列表 ============
  if (view === 'activities') {
    return (
      <div className="min-h-screen bg-[#F2F2F7] text-[#1C1C1E]">
        <Header title={selectedCourse?.name ?? '课程'} subtitle="选择课时查看课件与题目" onBack={() => backTo('courses')} />
        <main className="max-w-3xl mx-auto px-5 py-6 space-y-3">
          {error && <ErrorBox message={error} onRetry={() => selectedCourse && void enterCourse(selectedCourse)} />}
          {loading ? (
            <div className="flex justify-center py-16"><Loader2 className="w-6 h-6 animate-spin text-[#007AFF]" /></div>
          ) : activities.length === 0 && !error ? (
            <div className="bg-white rounded-3xl p-10 text-center text-sm text-[#8E8E93]">该课程暂无课时记录</div>
          ) : activities.map(a => (
            <button key={a.activityId} onClick={() => void enterLesson(a)}
              className="w-full bg-white rounded-3xl border border-black/[0.06] p-4 text-left shadow-[0_1px_3px_rgba(0,0,0,0.02)] hover:bg-gray-50 active:scale-[0.99] transition-all flex items-center gap-3">
              <div className={`w-10 h-10 rounded-xl flex items-center justify-center shrink-0 ${
                a.type === 1 ? 'bg-blue-50 text-[#007AFF]' : a.type === 4 ? 'bg-orange-50 text-orange-600' : 'bg-purple-50 text-purple-600'}`}>
                {a.type === 4 ? <FileQuestion className="w-4 h-4" /> : <PresentationIcon className="w-4 h-4" />}
              </div>
              <div className="flex-1 min-w-0">
                <h3 className="text-sm font-semibold truncate">{a.title || a.typeName}</h3>
                <p className="text-[11px] text-[#8E8E93] mt-0.5 flex items-center gap-2">
                  <span>{a.typeName}</span>
                  <span>·</span>
                  <span className="flex items-center gap-1"><Clock className="w-3 h-3" />{new Date(a.createTime).toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })}</span>
                </p>
              </div>
              {a.finished && <CheckCircle2 className="w-4 h-4 text-emerald-500 shrink-0" />}
              <ChevronRight className="w-4 h-4 text-[#C7C7CC] shrink-0" />
            </button>
          ))}
        </main>
      </div>
    );
  }

  // ============ 视图 3：课件 PPT + 教师题目 ============
  return (
    <div className="min-h-screen bg-[#F2F2F7] text-[#1C1C1E] flex flex-col">
      <Header title={selectedLesson?.title || '课件'} subtitle={selectedCourse?.name ?? ''} onBack={() => backTo('activities')} />
      <main className="max-w-4xl mx-auto w-full px-4 sm:px-5 py-4 flex-1">
        {error && <ErrorBox message={error} onRetry={() => selectedLesson && void enterLesson(selectedLesson)} />}
        {loading ? (
          <div className="flex justify-center py-16"><Loader2 className="w-6 h-6 animate-spin text-[#007AFF]" /></div>
          ) : presentations.length === 0 && !error ? (
          <div className="bg-white rounded-3xl p-10 text-center space-y-2">
            <ImageIcon className="w-10 h-10 mx-auto text-[#C7C7CC]" />
            <p className="text-sm text-[#3A3A3C] font-medium">该课时暂无可展示的课件</p>
            <p className="text-xs text-[#8E8E93]">历史课时的课件可能已被雨课堂归档删除，仅进行中的课堂可实时查看</p>
          </div>
        ) : (
          <>
            {/* ===== 测验区：题目集中在最上方（已结束/进行中课堂一致）===== */}
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
                      const pv = parseProblem(q.problem);
                      const active = q.presIdx === presIndex && q.slideIdx === slideIndex;
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
                          <ProblemCard problem={pv!} compact />
                        </button>
                      );
                    })}
                  </div>
                )}
              </section>
            )}

            {/* ===== 课件区：切换栏 + 全屏舞台 + 下方缩略图（对齐小程序）===== */}
            <div className={`flex items-center justify-between gap-2 mb-3 flex-wrap ${fullscreen ? 'hidden' : ''}`}>
              <div className="flex items-center gap-1.5 flex-wrap">
                {presentations.map((p, i) => (
                  <button key={p.id} onClick={() => { setPresIndex(i); gotoSlide(0); }}
                    className={`px-3 py-1.5 rounded-xl text-xs font-medium flex items-center gap-1.5 transition-all ${
                      i === presIndex ? 'bg-[#007AFF] text-white' : 'bg-white text-[#3A3A3C] border border-black/[0.06]'}`}>
                    <Layers className="w-3.5 h-3.5" /> {p.title} ({p.slidesCount}页)
                  </button>
                ))}
              </div>
              <span className="text-[10px] text-[#8E8E93] flex items-center gap-1">
                <MousePointer2 className="w-3 h-3" /> 滑动/滚轮翻页 · 双击全屏
              </span>
            </div>

            {currentPres && current ? (
              <>
              {/* 全屏遮罩 */}
              {fullscreen && <div className="fixed inset-0 bg-black z-[1000]" />}
              <div className={`${fullscreen ? 'fixed z-[1001] bg-black' : 'bg-white rounded-3xl border border-black/[0.06] overflow-hidden shadow-[0_1px_3px_rgba(0,0,0,0.02)]'} ${
                fullscreen ? 'inset-0 flex items-center justify-center' : ''}`}>
                <div className={`flex flex-col w-full ${fullscreen ? 'h-full' : ''} ${
                  landscape && fullscreen ? 'rotate-90 origin-center' : ''}`}
                  style={fullscreen && landscape ? { width: '100vh', height: '100vw', transform: 'rotate(90deg) translate(50%, -50%)', position: 'absolute', top: '50%', left: '50%' } : undefined}>
                  {/* 控制按钮组：全屏/退出、横竖屏（仅全屏显示），缩放按钮已去掉（纯手势） */}
                  <div className={`flex items-center gap-2 ${fullscreen ? 'absolute top-3 right-3 z-20' : 'justify-end p-2'}`}>
                    <button onClick={toggleFullscreen}
                      className={`w-10 h-10 rounded-full flex items-center justify-center ${fullscreen ? 'bg-black/50 text-white' : 'bg-[#007AFF] text-white'}`}
                      title={fullscreen ? '退出全屏' : '全屏'}>
                      {fullscreen ? <Minimize2 className="w-5 h-5" /> : <Maximize2 className="w-5 h-5" />}
                    </button>
                    {fullscreen && (
                      <button onClick={toggleOrientation}
                        className="w-10 h-10 rounded-full flex items-center justify-center bg-black/50 text-white"
                        title={landscape ? '竖屏' : '横屏'}>
                        {landscape ? <Smartphone className="w-5 h-5" /> : <Monitor className="w-5 h-5" />}
                      </button>
                    )}
                  </div>

                  {/* PPT 舞台：双击切换全屏，双指缩放平移，滚轮翻页 */}
                  <div ref={pptViewportRef}
                    onClick={onPptTap}
                    onTouchStart={onTouchStart} onTouchMove={onTouchMove} onTouchEnd={onTouchEnd}
                    className={`w-full bg-slate-50 flex items-center justify-center overflow-hidden select-none ${fullscreen ? 'flex-1' : 'min-h-[300px] cursor-ns-resize'}`}>
                    {current.cover ? (
                      <img src={current.cover} alt={`第 ${current.page} 页`}
                        className="w-full h-auto object-contain max-h-[70vh] pointer-events-none"
                        style={{ transform: `translate(${offset.x}px, ${offset.y}px) scale(${zoom})`, transformOrigin: 'center center' }}
                        referrerPolicy="no-referrer" />
                    ) : (
                      <div className="flex flex-col items-center gap-2 text-[#AEAEB2] py-16">
                        <Maximize2 className="w-10 h-10" />
                        <span className="text-xs">该页暂无图片，可查看课件原文件</span>
                      </div>
                    )}
                  </div>

                  {/* 翻页栏：精简为页码指示 + 少量控制（全屏时底部半透明浮层） */}
                  <div className={`px-4 py-2.5 flex items-center justify-between ${
                    fullscreen ? 'bg-black/55 absolute bottom-0 left-0 right-0 text-white' : 'border-t border-black/[0.06] bg-[#FAFAFA]'}`}>
                    <button onClick={() => gotoSlide(Math.max(0, slideIndex - 1))} disabled={slideIndex === 0}
                      className="px-3 py-1.5 rounded-xl bg-white/90 border border-black/[0.06] text-xs disabled:opacity-40 text-[#1C1C1E]">
                      ‹ 上一页
                    </button>
                    <span className="text-xs opacity-80">{current.page} / {currentPres.slides.length} 页</span>
                    <button onClick={() => gotoSlide(Math.min(currentPres.slides.length - 1, slideIndex + 1))}
                      disabled={slideIndex >= currentPres.slides.length - 1}
                      className="px-3 py-1.5 rounded-xl bg-white/90 border border-black/[0.06] text-xs disabled:opacity-40 text-[#1C1C1E]">
                      下一页 ›
                    </button>
                  </div>
                </div>
              </div>

              {/* 下方缩略图预览条（横向滚动，全屏时隐藏） */}
              {!fullscreen && (
                <div ref={slideThumbRef} className="flex gap-2 overflow-x-auto mt-3 pb-2 px-1">
                  {currentPres.slides.map((s, i) => (
                    <button key={s.id || i} onClick={() => gotoSlide(i)}
                      className={`shrink-0 w-[100px] rounded-lg overflow-hidden border-2 transition-all relative ${
                        i === slideIndex ? 'border-[#007AFF]' : 'border-transparent hover:border-[#C7C7CC]'}`}>
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
          </>
        )}
      </main>
    </div>
  );
}

function Header({ title, subtitle, onBack }: { title: string; subtitle: string; onBack: () => void }) {
  return (
    <header className="bg-white/90 backdrop-blur-md border-b border-black/[0.06] sticky top-0 z-20">
      <div className="max-w-4xl mx-auto px-5 h-14 flex items-center gap-2.5">
        <button onClick={onBack} className="w-8 h-8 rounded-full bg-[#F2F2F7] flex items-center justify-center text-[#1C1C1E] hover:bg-[#E5E5EA]">
          <ArrowLeft className="w-4 h-4" />
        </button>
        <div className="flex-1 min-w-0">
          <h1 className="text-[15px] font-bold truncate">{title}</h1>
          <p className="text-[11px] text-[#8E8E93] truncate">{subtitle}</p>
        </div>
      </div>
    </header>
  );
}

function ProblemCard({ problem, compact = false }: { problem: ProblemView; compact?: boolean }) {
  const strip = (s?: string) => (s ?? '').replace(/<[^>]+>/g, '').trim();
  const typeName = problem.type ?? ({ 1: '单选题', 2: '多选题', 3: '投票题', 4: '填空题', 5: '主观题' } as Record<number, string>)[problem.problemType ?? 1] ?? '题目';
  const opts = problem.options ?? [];
  const answers = Array.isArray(problem.answer) ? problem.answer : (problem.answer ? [problem.answer] : []);
  return (
    <div className={`rounded-2xl ${compact ? 'bg-transparent border-0 p-0' : 'bg-orange-50/60 border border-orange-100 p-4'} space-y-2`}>
      <div className="flex items-center justify-between gap-2 flex-wrap">
        <div className="flex items-center gap-2">
          <span className="px-2 py-0.5 rounded-full bg-orange-100 text-orange-700 text-[10px] font-semibold">{typeName}</span>
          <span className="text-[10px] text-orange-500">课件内嵌题目</span>
        </div>
        {answers.length > 0 && (
          <span className="px-2 py-0.5 rounded-full bg-emerald-100 text-emerald-700 text-[10px] font-semibold">
            参考答案：{answers.join(' / ')}
          </span>
        )}
      </div>
      <p className="text-sm font-medium text-[#1C1C1E] leading-relaxed">{strip(problem.body) || '（题目内容见课件图片）'}</p>
      {opts.length > 0 && (
        <ul className="space-y-1.5 pt-1">
          {opts.map((o, i) => {
            const key = typeof o === 'string' ? String.fromCharCode(65 + i) : ((o as { key?: string }).key ?? String.fromCharCode(65 + i));
            const val = typeof o === 'string' ? o : strip((o as { value?: string }).value);
            const isAnswer = answers.includes(key);
            return <li key={i} className={`text-xs flex items-start gap-2 rounded-lg px-1.5 py-1 ${isAnswer ? 'bg-emerald-50 text-emerald-800' : 'text-[#3A3A3C]'}`}>
              <span className={`w-5 h-5 rounded-md flex items-center justify-center shrink-0 text-[10px] font-semibold ${isAnswer ? 'bg-emerald-500 text-white' : 'bg-white border border-orange-200 text-orange-600'}`}>
                {key}
              </span>
              <span className="pt-0.5">{val}</span>
              {isAnswer && <CheckCircle2 className="w-3.5 h-3.5 text-emerald-500 shrink-0 ml-auto" />}
            </li>;
          })}
        </ul>
      )}
    </div>
  );
}

function ErrorBox({ message, onRetry }: { message: string; onRetry: () => void }) {
  return (
    <div className="mb-3 rounded-2xl bg-red-50 border border-red-100 p-3.5 text-xs text-red-700 flex items-center justify-between gap-3">
      <span>{message}</span>
      <button onClick={onRetry} className="shrink-0 px-2 py-1 rounded-lg bg-red-100 text-red-700 font-medium">重试</button>
    </div>
  );
}
