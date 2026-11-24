App({
  globalData: {
    // 后端地址默认值。请在登录页「⚙ 后端地址设置」填入你的服务器地址：
    //   调试：http://<公网IP>:8080   正式：https://<已备案域名>（需 Nginx + SSL）
    apiBase: 'http://localhost:8080/api/v1',
    token: '',
    sessionId: '',
    userId: ''
  },
  onLaunch() {
    const token = wx.getStorageSync('gxH_token') || '';
    const sessionId = wx.getStorageSync('gxH_sid') || '';
    const userId = wx.getStorageSync('gxH_uid') || '';
    if (token) {
      this.globalData.token = token;
      this.globalData.sessionId = sessionId;
      this.globalData.userId = userId;
    }
    const base = wx.getStorageSync('gxH_apiBase');
    if (base) this.globalData.apiBase = base;
  },
  setSession(token, sessionId, userId) {
    this.globalData.token = token;
    this.globalData.sessionId = sessionId || '';
    this.globalData.userId = userId || '';
    wx.setStorageSync('gxH_token', token);
    wx.setStorageSync('gxH_sid', sessionId || '');
    wx.setStorageSync('gxH_uid', userId || '');
  },
  clearSession() {
    this.globalData.token = '';
    this.globalData.sessionId = '';
    this.globalData.userId = '';
    wx.removeStorageSync('gxH_token');
    wx.removeStorageSync('gxH_sid');
    wx.removeStorageSync('gxH_uid');
  },
  setApiBase(url) {
    let base = String(url || '').trim().replace(/\/+$/, '');
    if (base && !/\/api\/v1$/.test(base)) base += '/api/v1';
    this.globalData.apiBase = base;
    wx.setStorageSync('gxH_apiBase', base);
  }
});
