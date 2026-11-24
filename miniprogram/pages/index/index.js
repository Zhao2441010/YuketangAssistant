const api = require('../../utils/api');

Page({
  data: {
    liveCourses: [],
    loading: true,
    error: '',
    userId: ''
  },

  onShow() {
    const app = getApp();
    // globalData 为空时用本地 storage 兜底恢复（冷启动/reLaunch 时序）
    if (!app.globalData.token) {
      const cached = wx.getStorageSync('gxH_token');
      if (cached) {
        app.globalData.token = cached;
        app.globalData.sessionId = wx.getStorageSync('gxH_sid') || '';
        app.globalData.userId = wx.getStorageSync('gxH_uid') || '';
      }
    }
    console.log('[index onShow] token=', app.globalData.token ? '有' : '无',
                'apiBase=', app.globalData.apiBase);
    if (!app.globalData.token) {
      wx.redirectTo({ url: '/pages/login/login' });
      return;
    }
    this.setData({ userId: app.globalData.userId });
    this.loadCourses();
    this.startPolling();
  },

  onHide() { this.stopPolling(); },
  onUnload() { this.stopPolling(); },

  /** 每分钟自动询问一次上课状态（避免持续请求） */
  startPolling() {
    this.stopPolling();
    this.pollTimer = setInterval(() => {
      if (getApp().globalData.token) this.loadCourses(true);
    }, 60 * 1000);
  },
  stopPolling() {
    if (this.pollTimer) { clearInterval(this.pollTimer); this.pollTimer = null; }
  },

  async loadCourses(silent) {
    if (!silent) this.setData({ loading: true, error: '' });
    try {
      const data = await api.fetchCourses();
      this.setData({ liveCourses: data.liveCourses || [], loading: false });
    } catch (e) {
      // 静默轮询失败不打断用户，显示轻量错误
      this.setData({ loading: false, error: silent ? '自动刷新失败，下拉可重试' : e.message });
    }
  },

  goCourses() { wx.navigateTo({ url: '/pages/courses/courses' }); },

  enterCourse(e) {
    const cid = e.currentTarget.dataset.cid;
    wx.navigateTo({ url: '/pages/quiz/quiz?courseId=' + encodeURIComponent(cid) });
  },

  async openClassroom(e) {
    const cid = e.currentTarget.dataset.cid;
    wx.showLoading({ title: '生成进班链接' });
    try {
      const result = await api.joinLesson(cid);
      wx.hideLoading();
      wx.setClipboardData({
        data: result.joinUrl,
        success: () => wx.showModal({
          title: '官方课堂链接已复制',
          content: '请在已登录雨课堂的浏览器中打开该链接进班',
          showCancel: false
        })
      });
    } catch (err) {
      wx.hideLoading();
      wx.showToast({ title: err.message, icon: 'none' });
    }
  },

  /** 退出登录：清除后端会话 + 本地凭据，回登录页 */
  async doLogout() {
    const confirm = await new Promise(resolve => {
      wx.showModal({
        title: '退出登录',
        content: '确定要退出当前账号吗？',
        success: (r) => resolve(r.confirm)
      });
    });
    if (!confirm) return;
    wx.showLoading({ title: '正在退出' });
    await api.logout();
    wx.hideLoading();
    this.stopPolling();
    getApp().clearSession();
    wx.reLaunch({ url: '/pages/login/login' });
  },

  onPullDownRefresh() {
    this.loadCourses().then(() => wx.stopPullDownRefresh());
  }
});
