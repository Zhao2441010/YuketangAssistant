const api = require('../../utils/api');

Page({
  data: {
    mode: 'qr',
    qr: null,
    countdown: 45,
    viewState: 'idle',
    loading: false,
    error: '',
    sessionId: '',
    userId: '',
    showSettings: false,
    apiBase: '',
    apiBaseInput: ''
  },

  onLoad() {
    this.pollTimer = null;
    this.countTimer = null;
    this.setData({
      apiBase: getApp().globalData.apiBase,
      apiBaseInput: getApp().globalData.apiBase
    });
  },

  // ============ 后端地址设置 ============
  toggleSettings() { this.setData({ showSettings: !this.data.showSettings }); },
  onApiBaseInput(e) { this.setData({ apiBaseInput: e.detail.value }); },
  saveApiBase() {
    if (!this.data.apiBaseInput.trim()) { wx.showToast({ title: '请输入地址', icon: 'none' }); return; }
    getApp().setApiBase(this.data.apiBaseInput);
    this.setData({ apiBase: getApp().globalData.apiBase, error: '' });
    wx.showToast({ title: '后端地址已保存', icon: 'success' });
  },

  onUnload() { this.stopTimers(); },

  stopTimers() {
    if (this.pollTimer) { clearTimeout(this.pollTimer); this.pollTimer = null; }
    if (this.countTimer) { clearInterval(this.countTimer); this.countTimer = null; }
  },

  switchMode(e) {
    this.stopTimers();
    this.setData({ mode: e.currentTarget.dataset.mode, error: '', qr: null, viewState: 'idle' });
  },

  // ============ 扫码登录 ============
  async startQr() {
    this.stopTimers();
    this.setData({ loading: true, error: '', qr: null, viewState: 'loading', countdown: 45 });
    try {
      const qr = await api.fetchQr();
      this.setData({ qr, loading: false, viewState: 'waiting' });
      this.startPoll(qr.ticket);
      this.countTimer = setInterval(() => {
        const n = this.data.countdown - 1;
        if (n <= 0) { clearInterval(this.countTimer); return; }
        this.setData({ countdown: n });
      }, 1000);
    } catch (e) {
      this.setData({ loading: false, viewState: 'error', error: e.message });
    }
  },

  startPoll(ticket) {
    const tick = async () => {
      if (this.data.viewState !== 'waiting') return;
      try {
        const result = await api.pollLogin(ticket);
        if (result && result.token) {
          this.loginSuccess(result);
          return;
        }
      } catch (e) {
        // 超时属正常，继续轮询
      }
      if (this.data.viewState === 'waiting') {
        this.pollTimer = setTimeout(tick, 1000);
      }
    };
    this.pollTimer = setTimeout(tick, 500);
  },

  async mockConfirm() {
    if (!this.data.qr) return;
    try {
      // mock-confirm 只返回 true/false（触发确认），真正的登录凭据由后台轮询 pollLogin 拿到。
      // 不能把它的返回值当 LoginResult 传给 loginSuccess，否则 token 为 undefined 会跳回登录页。
      await api.mockConfirm(this.data.qr.ticket);
      wx.showToast({ title: '已模拟确认，等待登录…', icon: 'none' });
      // 确保轮询在运行（startQr 已启动；这里兜底一次）
      if (!this.pollTimer) this.startPoll(this.data.qr.ticket);
    } catch (e) {
      this.setData({ error: e.message });
    }
  },

  /** 手动进入首页（自动 reLaunch 失败时的兜底入口） */
  goHome() {
    const app = getApp();
    if (!app.globalData.token) {
      const cached = wx.getStorageSync('gxH_token');
      if (cached) {
        app.globalData.token = cached;
        app.globalData.sessionId = wx.getStorageSync('gxH_sid') || '';
        app.globalData.userId = wx.getStorageSync('gxH_uid') || '';
      }
    }
    if (!app.globalData.token) {
      wx.showToast({ title: '尚未登录，请重新扫码', icon: 'none' });
      return;
    }
    wx.reLaunch({
      url: '/pages/index/index',
      fail: (err) => wx.showToast({ title: (err && err.errMsg) || '跳转失败', icon: 'none' })
    });
  },

  // ============ 会话导入 ============
  onSidInput(e) { this.setData({ sessionId: e.detail.value }); },
  onUidInput(e) { this.setData({ userId: e.detail.value }); },

  async doImport() {
    const sid = this.data.sessionId.trim();
    if (!sid) { this.setData({ error: '请输入 sessionid' }); return; }
    this.setData({ loading: true, error: '' });
    try {
      const result = await api.importSession(sid, this.data.userId.trim());
      this.loginSuccess(result);
    } catch (e) {
      this.setData({ loading: false, error: e.message });
    }
  },

  loginSuccess(result) {
    // 防御：只有拿到有效 token 才算登录成功，避免 undefined 跳转后被首页弹回
    const token = result && typeof result === 'object' ? result.token : '';
    if (!token) {
      this.setData({ viewState: 'error', error: '登录返回异常（未拿到 token），请重试' });
      return;
    }
    this.stopTimers();
    getApp().setSession(token, result.sessionKey || result.sessionId || '', result.userId || '');
    this.setData({ viewState: 'success', loading: false });
    wx.showToast({ title: '登录成功', icon: 'success' });
    const app = getApp();
    console.log('[login] 准备跳转 token=', app.globalData.token ? '有' : '无',
                'uid=', app.globalData.userId);
    // 先切到 success 态让 UI 稳定，再 reLaunch；带 fail 回调避免静默失败
    setTimeout(() => {
      wx.reLaunch({
        url: '/pages/index/index',
        success: () => console.log('[login] reLaunch 成功'),
        fail: (err) => {
          console.error('[login] reLaunch 失败', err);
          wx.showModal({
            title: '页面跳转失败',
            content: (err && err.errMsg) || '请重新进入小程序',
            showCancel: false
          });
        }
      });
    }, 1000);
  }
});
