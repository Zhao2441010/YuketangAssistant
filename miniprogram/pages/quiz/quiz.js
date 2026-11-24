const api = require('../../utils/api');

function buildState() {
  return { choices: [], blanks: [], subjective: '', submitting: false, msg: '' };
}

Page({
  data: { quizzes: [], answers: {}, courseId: '', loading: true, error: '' },

  onLoad(options) {
    const courseId = decodeURIComponent(options.courseId || '');
    this.setData({ courseId });
    this.load(courseId);
  },

  async load(courseId) {
    this.setData({ loading: true, error: '' });
    try {
      const list = await api.fetchInClassQuizzes(courseId);
      const quizzes = (list || []).map(q => {
        const qt = q.questionType || 'SINGLE_CHOICE';
        const isChoice = qt.indexOf('CHOICE') >= 0 || qt.indexOf('VOTE') >= 0;
        const isBlank = qt.indexOf('BLANK') >= 0;
        let optionList = [];
        (q.options || []).forEach((o, i) => {
          const str = String(o);
          const m = str.match(/^([A-Z])[.、]\s*(.*)$/);
          optionList.push(Object.assign(
            m ? { key: m[1], value: m[2] } : { key: String.fromCharCode(65 + i), value: str },
            { selected: false }
          ));
        });
        const blanks = q.blanks && q.blanks.length ? q.blanks : (isBlank ? [''] : []);
        return Object.assign({}, q, {
          questionType: typeLabel(qt), isChoice, isBlank,
          optionList, blanks, statusText: q.status === 'SUBMITTED' ? '已提交' : ''
        });
      });
      const answers = {};
      quizzes.forEach(q => { answers[q.quizId] = buildState(); });
      this.setData({ quizzes, answers, loading: false });
    } catch (e) {
      this.setData({ loading: false, error: e.message });
    }
  },

  toggleChoice(e) {
    const { qid, key } = e.currentTarget.dataset;
    const q = this.data.quizzes.find(x => x.quizId === qid);
    const multi = q && q.questionType.indexOf('多选') >= 0;
    const st = this.data.answers[qid];
    let choices = st.choices.slice();
    const i = choices.indexOf(key);
    if (multi) { i >= 0 ? choices.splice(i, 1) : choices.push(key); }
    else choices = i >= 0 ? [] : [key];
    const optionList = q.optionList.map(o => Object.assign({}, o, { selected: choices.indexOf(o.key) >= 0 }));
    const idx = this.data.quizzes.findIndex(x => x.quizId === qid);
    this.setData({
      ['answers.' + qid + '.choices']: choices,
      ['quizzes[' + idx + '].optionList']: optionList
    });
  },

  onBlankInput(e) {
    const { qid, index } = e.currentTarget.dataset;
    this.setData({ ['answers.' + qid + '.blanks[' + index + ']']: e.detail.value });
  },

  onSubjectiveInput(e) {
    const qid = e.currentTarget.dataset.qid;
    this.setData({ ['answers.' + qid + '.subjective']: e.detail.value });
  },

  async submitAnswer(e) {
    const qid = e.currentTarget.dataset.qid;
    const q = this.data.quizzes.find(x => x.quizId === qid);
    const st = this.data.answers[qid];
    if (!q) return;
    let answer = '';
    if (q.isChoice) answer = st.choices.slice().sort().join(',');
    else if (q.isBlank) answer = st.blanks.join(',');
    else answer = st.subjective;
    if (!answer) { wx.showToast({ title: '请先作答', icon: 'none' }); return; }

    this.setData({ ['answers.' + qid + '.submitting']: true });
    try {
      const result = await api.submitAnswer({
        courseId: this.data.courseId,
        problemId: qid,
        problemType: q.problemType || 1,
        answer
      });
      this.setData({
        ['answers.' + qid + '.submitting']: false,
        ['answers.' + qid + '.msg']: result.message || '提交成功'
      });
    } catch (err) {
      this.setData({
        ['answers.' + qid + '.submitting']: false,
        ['answers.' + qid + '.msg']: err.message
      });
    }
  }
});

function typeLabel(qt) {
  return ({
    SINGLE_CHOICE: '单选题', MULTIPLE_CHOICE: '多选题', VOTE: '投票题',
    FILL_BLANK: '填空题', SUBJECTIVE: '主观题'
  })[qt] || '题目';
}
