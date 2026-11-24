const api = require('../../utils/api');

function fmtTime(ms) {
  if (!ms) return '';
  const d = new Date(ms);
  const p = (n) => (n < 10 ? '0' + n : '' + n);
  return (d.getMonth() + 1) + '/' + d.getDate() + ' ' + p(d.getHours()) + ':' + p(d.getMinutes());
}

Page({
  data: { activities: [], courseName: '', courseId: '', loading: true, error: '' },

  onLoad(options) {
    const courseId = decodeURIComponent(options.courseId || '');
    const courseName = decodeURIComponent(options.name || '课程');
    this.setData({ courseId, courseName });
    wx.setNavigationBarTitle({ title: courseName });
    this.load(courseId);
  },

  async load(courseId) {
    this.setData({ loading: true, error: '' });
    try {
      const list = await api.fetchActivities(courseId);
      const activities = (list || []).map(a => Object.assign({}, a, { timeText: fmtTime(a.createTime) }));
      this.setData({ activities, loading: false });
    } catch (e) {
      this.setData({ loading: false, error: e.message });
    }
  },

  goSlides(e) {
    const { id, cw, title } = e.currentTarget.dataset;
    // 历史课件必须用 courseware_id；缺失时回退 activityId
    const lessonId = cw || id;
    wx.navigateTo({
      url: '/pages/slides/slides?lessonId=' + encodeURIComponent(lessonId) + '&title=' + encodeURIComponent(title || '')
    });
  }
});
