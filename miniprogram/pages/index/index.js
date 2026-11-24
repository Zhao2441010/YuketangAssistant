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
    if (!app.globalData.token) {
      wx.redirectTo({ url: '/pages/login/login' });
      return;
    }
    this.setData({ userId: app.globalData.userId });
    this.loadCourses();
  },

  async loadCourses() {
    this.setData({ loading: true, error: '' });
    try {
      const data = await api.fetchCourses();
      this.setData({ liveCourses: data.liveCourses || [], loading: false });
    } catch (e) {
      this.setData({ loading: false, error: e.message });
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

  onPullDownRefresh() {
    this.loadCourses().then(() => wx.stopPullDownRefresh());
  }
});
