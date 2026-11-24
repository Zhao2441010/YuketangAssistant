// 后端 API 封装：自动携带 token，401 时用 sessionid 无感重导后重试一次。
const app = () => getApp();

function request(path, options = {}, retry = true) {
  const { method = 'GET', data = {} } = options;
  return new Promise((resolve, reject) => {
    wx.request({
      url: app().globalData.apiBase + path,
      method,
      data,
      header: {
        'Content-Type': 'application/json',
        'X-Session-Token': app().globalData.token || ''
      },
      success: (res) => {
        const body = res.data || {};
        if (res.statusCode === 200 && body.code === 0) {
          resolve(body.data);
          return;
        }
        // 401 且有 sessionid → 用 import-session 重建会话后重放
        if ((res.statusCode === 401 || body.code === 401) && retry && app().globalData.sessionId) {
          wx.request({
            url: app().globalData.apiBase + '/auth/import-session',
            method: 'POST',
            data: { sessionId: app().globalData.sessionId, userId: app().globalData.userId || undefined },
            header: { 'Content-Type': 'application/json' },
            success: (r) => {
              const d = r.data || {};
              if (d.code === 0 && d.data && d.data.token) {
                app().setSession(d.data.token, d.data.sessionKey, d.data.userId);
                request(path, options, false).then(resolve).catch(reject);
              } else {
                reject(new Error(d.message || '会话已失效，请重新登录'));
              }
            },
            fail: () => reject(new Error('网络异常，请检查后端地址'))
          });
          return;
        }
        reject(new Error(body.message || ('请求失败 ' + res.statusCode)));
      },
      fail: () => reject(new Error('无法连接后端，请检查地址与网络'))
    });
  });
}

// ============ 认证 ============
function fetchQr() { return request('/auth/qr'); }
function pollLogin(ticket) { return request('/auth/poll', { method: 'POST', data: { ticket } }); }
function mockConfirm(ticket) { return request('/auth/mock-confirm', { method: 'POST', data: { ticket } }); }
function passwordLogin(account, password, type, ticket, randstr) {
  return request('/auth/password-login', {
    method: 'POST',
    data: { account, password, type, ticket, randstr }
  });
}
function importSession(sessionId, userId) {
  return request('/auth/import-session', { method: 'POST', data: { sessionId, userId } });
}
function checkSession() { return request('/auth/session'); }

// ============ 业务 ============
function fetchCourses() { return request('/courses'); }
function fetchInClassQuizzes(courseId) { return request('/inclass/quizzes?courseId=' + encodeURIComponent(courseId)); }
function submitAnswer(payload) { return request('/inclass/answers', { method: 'POST', data: payload }); }
function joinLesson(courseId) {
  return request('/lessons/join', {
    method: 'POST',
    data: { courseId, sessionKey: app().globalData.sessionId || '' }
  });
}
function fetchActivities(classroomId) {
  return request('/content/activities?courseId=' + encodeURIComponent(classroomId));
}
function fetchSlides(lessonId) {
  return request('/content/slides?lessonId=' + encodeURIComponent(lessonId));
}

module.exports = {
  request, fetchQr, pollLogin, mockConfirm, passwordLogin, importSession, checkSession,
  fetchCourses, fetchInClassQuizzes, submitAnswer, joinLesson, fetchActivities, fetchSlides
};
