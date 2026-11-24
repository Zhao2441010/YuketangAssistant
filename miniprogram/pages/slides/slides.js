const api = require('../../utils/api');

function parseProblem(raw) {
  if (!raw) return null;
  let p;
  try { p = typeof raw === 'string' ? JSON.parse(raw) : raw; } catch (e) { return { body: String(raw) }; }
  const c = p.content || p;
  const opts = c.options || [];
  // 答案可能是数组或单个值，统一成 key 集合用于高亮
  const rawAns = c.answer || [];
  const answers = Array.isArray(rawAns) ? rawAns.map(String) : (rawAns ? [String(rawAns)] : []);
  // 结构化选项：{key, value, isAnswer}，与 Web 版 ProblemCard 一致
  const optionList = opts.map((o, i) => {
    const key = typeof o === 'string'
      ? String.fromCharCode(65 + i)
      : String((o && (o.key || o.label)) || String.fromCharCode(65 + i));
    const value = typeof o === 'string'
      ? strip(o)
      : strip((o && (o.value || o.text || o.body)) || '');
    return { key, value, isAnswer: answers.indexOf(key) >= 0 };
  });
  return {
    body: strip(c.body || ''),
    // 保留原始 HTML，供 rich-text 完整渲染（公式/图片/换行/加粗等格式不丢失）
    bodyHtml: String(c.body || ''),
    typeName: typeName(c.problemType || p.problemType || 1),
    optionList,
    answers,
    answer: answers.join(' / ')
  };
}
function strip(s) { return String(s || '').replace(/<[^>]+>/g, '').trim(); }
function typeName(t) {
  return ({ 1: '单选题', 2: '多选题', 3: '投票题', 4: '填空题', 5: '主观题' })[t] || '题目';
}

Page({
  data: {
    presentations: [], presIndex: 0, currentSlides: [], slideIndex: 0,
    quizItems: [], showQuiz: true, pageInput: '', loading: true, error: '',
    fullscreen: false, landscape: false, zoom: 1, offsetX: 0, offsetY: 0, thumbScrollLeft: 0
  },

  onLoad(options) {
    const lessonId = decodeURIComponent(options.lessonId || '');
    const title = decodeURIComponent(options.title || '课件与题目');
    if (title && title !== '课件与题目') wx.setNavigationBarTitle({ title });
    this.load(lessonId);
  },

  onUnload() {
    // 离开页面时恢复竖屏，避免影响其他页面
    if (this.data.landscape && wx.setPageOrientation) {
      try { wx.setPageOrientation({ orientation: 'portrait' }); } catch (e) {}
    }
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
            presTitle: p.title, body: pv.body, bodyHtml: pv.bodyHtml,
            typeName: pv.typeName, optionList: pv.optionList, answers: pv.answers, answer: pv.answer,
            active: false
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

  // ============ 全屏 ============
  /** 全屏 / 退出全屏（按钮与双击共用） */
  toggleFullscreen() {
    const next = !this.data.fullscreen;
    this.setData({ fullscreen: next, zoom: 1, offsetX: 0, offsetY: 0 });
    if (next) {
      wx.showToast({ title: '双击退出全屏', icon: 'none', duration: 1200 });
    } else {
      // 退出全屏时恢复竖屏，避免下次进入仍是横屏
      if (this.data.landscape) this.setOrientation(false);
    }
  },

  // ============ 横竖屏 ============
  /** 横竖屏切换（需页面 json 配置 pageOrientation: auto） */
  toggleOrientation() {
    this.setOrientation(!this.data.landscape);
  },
  setOrientation(landscape) {
    const orientation = landscape ? 'landscape' : 'portrait';
    try {
      if (wx.setPageOrientation) {
        wx.setPageOrientation({ orientation });
        this.setData({ landscape });
      } else {
        wx.showToast({ title: '当前版本不支持横竖屏切换', icon: 'none' });
      }
    } catch (e) {
      wx.showToast({ title: '横竖屏切换失败', icon: 'none' });
    }
  },

  // ============ 缩放 + 平移（仅双指手势） ============
  /** 双指缩放 + 平移：距离比控制缩放，中点位移控制平移；单指交给 swiper 翻页 */
  onTouchStart(e) {
    if (e.touches.length === 2) {
      const a = e.touches[0], b = e.touches[1];
      this._pinch = {
        dist: this._touchDist(a, b),
        cx: (a.clientX + b.clientX) / 2,
        cy: (a.clientY + b.clientY) / 2,
        zoom: this.data.zoom,
        ox: this.data.offsetX,
        oy: this.data.offsetY
      };
    }
  },
  onTouchMove(e) {
    if (e.touches.length === 2 && this._pinch) {
      const a = e.touches[0], b = e.touches[1];
      const dist = this._touchDist(a, b);
      const ratio = dist / (this._pinch.dist || 1);
      let zoom = Math.max(1, Math.min(4, +(this._pinch.zoom * ratio).toFixed(2)));
      // 中点位移作为平移量；缩放到 1 时强制归位
      let ox = this._pinch.ox + ((a.clientX + b.clientX) / 2 - this._pinch.cx);
      let oy = this._pinch.oy + ((a.clientY + b.clientY) / 2 - this._pinch.cy);
      if (zoom <= 1) { ox = 0; oy = 0; }
      this.setData({ zoom, offsetX: ox, offsetY: oy });
    }
  },
  onTouchEnd(e) {
    if (e.touches.length < 2) {
      this._pinch = null;
      // 松开后若缩放接近原尺寸，归位避免图片歪在角落
      if (this.data.zoom <= 1.01 && (this.data.offsetX || this.data.offsetY)) {
        this.setData({ zoom: 1, offsetX: 0, offsetY: 0 });
      }
    }
  },
  _touchDist(a, b) {
    const dx = a.clientX - b.clientX;
    const dy = a.clientY - b.clientY;
    return Math.sqrt(dx * dx + dy * dy);
  },

  /** 双击 PPT 切换全屏：350ms 内两次 tap 视为双击。
   *  swiper 左右滑动不会触发 tap，故与翻页不冲突。 */
  onSlideTap() {
    const now = Date.now();
    if (this._lastTap && now - this._lastTap < 350) {
      this._lastTap = 0;
      this.toggleFullscreen();
    } else {
      this._lastTap = now;
    }
  },

  /** 全屏时点状态栏区域也可退出（预留） */
  onFullscreenTap() {
    if (this.data.fullscreen) this.toggleFullscreen();
  },

  jumpTo(e) {
    const { pi, si } = e.currentTarget.dataset;
    const pres = this.data.presentations[pi];
    if (!pres) return;
    this.setData({
      presIndex: pi, currentSlides: pres.slides, slideIndex: si,
      pageInput: String(pres.slides[si].page),
      quizItems: this.data.quizItems.map((q, i) => Object.assign({}, q, { active: q.presIdx === pi && q.slideIdx === si }))
    });
    this.syncThumbScroll(si);
  },

  switchPres(e) {
    const i = Number(e.currentTarget.dataset.index);
    const pres = this.data.presentations[i];
    if (!pres) return;
    this.setData({ presIndex: i, currentSlides: pres.slides, slideIndex: 0,
      pageInput: pres.slides[0] ? String(pres.slides[0].page) : '' });
    this.syncThumbScroll(0);
  },

  goSlide(e) { this.setSlide(Number(e.currentTarget.dataset.index)); },
  onSwiperChange(e) { this.setSlide(e.detail.current); },

  setSlide(i) {
    const slides = this.data.currentSlides;
    if (!slides.length) return;
    const idx = Math.max(0, Math.min(slides.length - 1, i));
    // 翻页时重置缩放/平移，避免新页仍处于放大偏移状态
    this.setData({ slideIndex: idx, pageInput: String(slides[idx].page), zoom: 1, offsetX: 0, offsetY: 0 });
    this.syncThumbScroll(idx);
  },

  /** 缩略图横条自动滚动到当前页附近（每项约 140rpx） */
  syncThumbScroll(idx) {
    const ITEM = 140;
    const target = Math.max(0, (idx - 2) * ITEM);
    this.setData({ thumbScrollLeft: target });
  }
});
