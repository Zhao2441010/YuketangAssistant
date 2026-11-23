import React, { useEffect, useState } from 'react';
import {
  ArrowLeft, BookOpen, CheckCircle2, ChevronLeft, ChevronRight,
  Clock, FileQuestion, Image as ImageIcon, Layers, Loader2, Presentation as PresentationIcon
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
  const [showProblemOnly, setShowProblemOnly] = useState(false);

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
    setLoading(true); setError(null); setPresIndex(0); setSlideIndex(0); setShowProblemOnly(false);
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
  const allSlides: { slide: Slide; presTitle: string }[] = currentPres
    ? currentPres.slides.map(s => ({ slide: s, presTitle: currentPres.title }))
    : [];
  const visibleSlides = showProblemOnly ? allSlides.filter(x => x.slide.problem) : allSlides;
  const current = visibleSlides[slideIndex];

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
            {/* 课件切换 + 仅看题目 */}
            <div className="flex items-center justify-between gap-2 mb-3 flex-wrap">
              <div className="flex items-center gap-1.5 flex-wrap">
                {presentations.map((p, i) => (
                  <button key={p.id} onClick={() => { setPresIndex(i); setSlideIndex(0); }}
                    className={`px-3 py-1.5 rounded-xl text-xs font-medium flex items-center gap-1.5 transition-all ${
                      i === presIndex ? 'bg-[#007AFF] text-white' : 'bg-white text-[#3A3A3C] border border-black/[0.06]'}`}>
                    <Layers className="w-3.5 h-3.5" /> {p.title} ({p.slidesCount}页)
                  </button>
                ))}
              </div>
              <button onClick={() => { setShowProblemOnly(v => !v); setSlideIndex(0); }}
                className={`px-3 py-1.5 rounded-xl text-xs font-medium flex items-center gap-1.5 border transition-all ${
                  showProblemOnly ? 'bg-orange-50 text-orange-600 border-orange-200' : 'bg-white text-[#8E8E93] border-black/[0.06]'}`}>
                <FileQuestion className="w-3.5 h-3.5" /> {showProblemOnly ? '仅看题目' : '全部页面'}
              </button>
            </div>

            {current ? (
              <div className="bg-white rounded-3xl border border-black/[0.06] overflow-hidden shadow-[0_1px_3px_rgba(0,0,0,0.02)]">
                {/* 幻灯片图片：cover 为直链，无需 Cookie */}
                <div className="w-full bg-slate-50 flex items-center justify-center min-h-[300px]">
                  {current.slide.cover ? (
                    <img src={current.slide.cover} alt={`第 ${current.slide.page} 页`}
                      className="w-full h-auto object-contain" referrerPolicy="no-referrer" />
                  ) : (
                    <div className="flex flex-col items-center gap-2 text-[#AEAEB2] py-16">
                      <ImageIcon className="w-10 h-10" />
                      <span className="text-xs">该页暂无图片</span>
                    </div>
                  )}
                </div>
                {/* 页码与题目 */}
                <div className="px-5 py-4 border-t border-black/[0.06] space-y-3">
                  <div className="flex items-center justify-between">
                    <span className="text-xs text-[#8E8E93]">第 {current.slide.page} 页 / 共 {currentPres?.slidesCount ?? 0} 页</span>
                    {current.slide.problem && (
                      <span className="px-2 py-0.5 rounded-full bg-orange-50 text-orange-600 text-[10px] font-semibold">含教师题目</span>
                    )}
                  </div>
                  {parseProblem(current.slide.problem) && <ProblemCard problem={parseProblem(current.slide.problem)!} />}
                </div>
                {/* 翻页控制 */}
                <div className="px-5 py-3 border-t border-black/[0.06] flex items-center justify-between bg-[#FAFAFA]">
                  <button onClick={() => setSlideIndex(i => Math.max(0, i - 1))} disabled={slideIndex === 0}
                    className="px-3 py-1.5 rounded-xl bg-white border border-black/[0.06] text-xs flex items-center gap-1 disabled:opacity-40">
                    <ChevronLeft className="w-3.5 h-3.5" /> 上一页
                  </button>
                  <span className="text-xs text-[#8E8E93]">{slideIndex + 1} / {visibleSlides.length}</span>
                  <button onClick={() => setSlideIndex(i => Math.min(visibleSlides.length - 1, i + 1))}
                    disabled={slideIndex >= visibleSlides.length - 1}
                    className="px-3 py-1.5 rounded-xl bg-white border border-black/[0.06] text-xs flex items-center gap-1 disabled:opacity-40">
                    下一页 <ChevronRight className="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>
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

function ProblemCard({ problem }: { problem: ProblemView }) {
  const strip = (s?: string) => (s ?? '').replace(/<[^>]+>/g, '').trim();
  const typeName = problem.type ?? ({ 1: '单选题', 2: '多选题', 3: '投票题', 4: '填空题', 5: '主观题' } as Record<number, string>)[problem.problemType ?? 1] ?? '题目';
  const opts = problem.options ?? [];
  const answers = Array.isArray(problem.answer) ? problem.answer : (problem.answer ? [problem.answer] : []);
  return (
    <div className="rounded-2xl bg-orange-50/60 border border-orange-100 p-4 space-y-2">
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
