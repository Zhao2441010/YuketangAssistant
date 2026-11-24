const api = require('../../utils/api');

Page({
  data: { courses: [], loading: true, error: '' },

  onLoad() { this.load(); },

  async load() {
    this.setData({ loading: true, error: '' });
    try {
      const data = await api.fetchCourses();
      this.setData({ courses: data.allCourses || [], loading: false });
    } catch (e) {
      this.setData({ loading: false, error: e.message });
    }
  },

  goActivities(e) {
    const { id, name } = e.currentTarget.dataset;
    wx.navigateTo({ url: '/pages/activities/activities?courseId=' + encodeURIComponent(id) + '&name=' + encodeURIComponent(name) });
  }
});
