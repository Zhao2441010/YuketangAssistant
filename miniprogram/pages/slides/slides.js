const api = require('../../utils/api');

function parseProblem(raw) {
  if (!raw) return null;
  let p;
  try { p = typeof raw === 'string' ? JSON.parse(raw) : raw; } catch (e) { return { body: String(raw) }; }
  const c = p.content || p;
  const opts = c.options || [];
  const optionTexts = opts.map((o, i) => {
    if (typeof o === 'string') return String.fromCharCode(65 + i) + '. ' + o;
    return (o.key || String.fromCharCode(65 + i)) + '. ' + (o.value || '');
  });
  const ans = c.answer || [];
  return {
    body: strip(c.body || ''),
    typeName: typeName(c.problemType || p.problemType || 1),
    options: optionTexts,
    answer: Array.isArray(ans) ? ans.join('/') : String(ans || '')
  };
}
function strip(s) { return String(s || '').replace(/<[^>]+>/g, '').trim(); }
function typeName(t) {
  return ({ 1: '单选题', 2: '多选题', 3: '投票题', 4: '填空题', 5: '主观题' })[t] || '题目';
}

Page({
  data: {
    presentations: [], presIndex: 0, currentSlides: [], slideIndex: 0,
    quizItems: [], showQuiz: true, pageInput: '', loading: true, error: ''
  },

  onLoad(options) {
    const lessonId = decodeURIComponent(options.lessonId || '');
    const title = decodeURIComponent(options.title || '课件与题目');
    if (title && title !== '课件与题目') wx.setNavigationBarTitle({ title });
    this.load(lessonId);
  },

  async load(lessonId) {
    this.setData({ loading: true, error: '' });
    try {
      const pres = await api.fetchSlides(lessonId);
      const quizItems = [];
      (pres || []).forEach((p, pi) => {
        (p.slides || []).forEach((s, si) => {
          const pv = s.problem ? parseProblem(s.problem) : null;
          if (pv) quizItems.push({
            idx: quizItems.length, presIdx: pi, slideIdx: si, page: s.page,
            presTitle: p.title, body: pv.body, answer: pv.answer, active: false
          });
          s._problemParsed = pv;
        });
      });
      this.setData({
        presentations: pres || [],
        currentSlides: pres && pres[0] ? pres[0].slides : [],
        quizItems, pageInput: pres && pres[0] && pres[0].slides[0] ? String(pres[0].slides[0].page) : '',
        loading: false
      });
    } catch (e) {
      this.setData({ loading: false, error: e.message });
    }
  },

  toggleQuiz() { this.setData({ showQuiz: !this.data.showQuiz }); },

  jumpTo(e) {
    const { pi, si } = e.currentTarget.dataset;
    const pres = this.data.presentations[pi];
    if (!pres) return;
    this.setData({
      presIndex: pi, currentSlides: pres.slides, slideIndex: si,
      pageInput: String(pres.slides[si].page),
      quizItems: this.data.quizItems.map((q, i) => Object.assign({}, q, { active: q.presIdx === pi && q.slideIdx === si }))
    });
  },

  switchPres(e) {
    const i = Number(e.currentTarget.dataset.index);
    const pres = this.data.presentations[i];
    if (!pres) return;
    this.setData({ presIndex: i, currentSlides: pres.slides, slideIndex: 0,
      pageInput: pres.slides[0] ? String(pres.slides[0].page) : '' });
  },

  goSlide(e) { this.setSlide(Number(e.currentTarget.dataset.index)); },
  prevSlide() { this.setSlide(this.data.slideIndex - 1); },
  nextSlide() { this.setSlide(this.data.slideIndex + 1); },
  onSwiperChange(e) { this.setSlide(e.detail.current); },

  setSlide(i) {
    const slides = this.data.currentSlides;
    if (!slides.length) return;
    const idx = Math.max(0, Math.min(slides.length - 1, i));
    this.setData({ slideIndex: idx, pageInput: String(slides[idx].page) });
  },

  onPageInput(e) { this.setData({ pageInput: e.detail.value }); },
  goToPage() {
    const slides = this.data.currentSlides;
    const target = Number(this.data.pageInput);
    if (!slides.length || !isFinite(target) || target < 1) return;
    let idx = slides.findIndex(s => s.page === target);
    if (idx < 0 && target <= slides.length) idx = target - 1;
    if (idx >= 0) this.setData({ slideIndex: idx });
    else wx.showToast({ title: '页码不存在', icon: 'none' });
  }
});
