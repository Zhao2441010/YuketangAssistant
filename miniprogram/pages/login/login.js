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
      const result = await api.mockConfirm(this.data.qr.ticket);
      this.loginSuccess(result);
    } catch (e) {
      this.setData({ error: e.message });
    }
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
    this.stopTimers();
    getApp().setSession(result.token, result.sessionKey || result.sessionId || '', result.userId || '');
    this.setData({ viewState: 'success', loading: false });
    wx.showToast({ title: '登录成功', icon: 'success' });
    setTimeout(() => wx.reLaunch({ url: '/pages/index/index' }), 800);
  }
});
