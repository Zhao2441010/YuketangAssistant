import React, { useEffect, useRef, useState } from 'react';
import { CheckCircle2, KeyRound, Loader2, QrCode, RefreshCw, Smartphone, Wifi } from 'lucide-react';
import { fetchLoginQr, LoginQr, mockConfirm, passwordLogin, pollLogin } from './api';

interface Props {
  onLogin: (userId: string, mock: boolean) => void;
}

type Phase = 'idle' | 'loading' | 'waiting' | 'success' | 'error';

/** 扫码登录页：用户端 → 后端 → 雨课堂端 链路的可视化入口。 */
export default function LoginView({ onLogin }: Props) {
  const [qr, setQr] = useState<LoginQr | null>(null);
  const [phase, setPhase] = useState<Phase>('idle');
  const [message, setMessage] = useState('');
  const [countdown, setCountdown] = useState(45);
  const [loginMode, setLoginMode] = useState<'qr' | 'password'>('qr');
  const [account, setAccount] = useState('');
  const [password, setPassword] = useState('');
  const [accountType, setAccountType] = useState<'PP' | 'E'>('PP');
  const [captcha, setCaptcha] = useState<{ ticket: string; randstr: string } | null>(null);
  const [pwdBusy, setPwdBusy] = useState(false);
  const pollAbort = useRef<AbortController | null>(null);
  const mounted = useRef(true);

  useEffect(() => {
    mounted.current = true;
    return () => {
      mounted.current = false;
      pollAbort.current?.abort();
    };
  }, []);

  /** 创建二维码并立刻发起长轮询（与官方一致：单次挂起请求）。 */
  const startLogin = async () => {
    pollAbort.current?.abort();
    setPhase('loading');
    setMessage('');
    setQr(null);
    try {
      const loginQr = await fetchLoginQr();
      if (!mounted.current) return;
      setQr(loginQr);
      setCountdown(loginQr.pollTimeoutSeconds);
      setPhase('waiting');
      void beginPoll(loginQr);
    } catch (e) {
      if (!mounted.current) return;
      setPhase('error');
      setMessage(`获取二维码失败：${(e as Error).message}。请确认后端服务已启动。`);
    }
  };

  /** 长轮询：后端挂起最多 45 秒，扫码确认瞬间返回；超时则在二维码有效期内自动续轮询。 */
  const beginPoll = async (loginQr: LoginQr) => {
    const controller = new AbortController();
    pollAbort.current = controller;
    setCountdown(loginQr.pollTimeoutSeconds);
    try {
      const result = await pollLogin(loginQr.ticket);
      if (!mounted.current) return;
      setPhase('success');
      setMessage(`登录成功，学号 ${result.userId}`);
      window.setTimeout(() => {
        if (mounted.current) onLogin(result.userId, result.mock);
      }, 800);
    } catch (e) {
      if (!mounted.current || controller.signal.aborted) return;
      // 超时（409）且二维码未过期 → 自动续轮询，用户有充足时间扫码
      if (Date.now() < loginQr.expiresAt) {
        setCountdown(loginQr.pollTimeoutSeconds);
        void beginPoll(loginQr);
        return;
      }
      setPhase('error');
      setMessage(`二维码已过期或登录未完成：${(e as Error).message}`);
    }
  };

  /** 腾讯防水墙滑块：加载官方 JS 并让用户完成验证，拿到 ticket/randstr。 */
  const showCaptcha = () => {
    const w = window as unknown as { TencentCaptcha?: new (appid: string, cb: (r: {
      ret: number; ticket: string; randstr: string
    }) => void) => { show: () => void } };
    const start = () => {
      if (!w.TencentCaptcha) { setMessage('滑块组件加载失败，请刷新重试'); return; }
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      const cap = new (w.TencentCaptcha as any)('2091064951', (r: {
        ret: number; ticket: string; randstr: string
      }) => {
        if (r.ret === 0) setCaptcha({ ticket: r.ticket, randstr: r.randstr });
      });
      cap.show();
    };
    if (w.TencentCaptcha) { start(); return; }
    const script = document.createElement('script');
    script.src = 'https://ssl.captcha.qq.com/TCaptcha.js';
    script.onload = start;
    script.onerror = () => setMessage('滑块组件加载失败，请检查网络');
    document.head.appendChild(script);
  };

  /** 账号密码登录（真实环境需先完成滑块验证）。 */
  const handlePasswordLogin = async () => {
    if (!account.trim() || !password) { setMessage('请输入账号和密码'); return; }
    setPwdBusy(true);
    setPhase('loading');
    setMessage('');
    try {
      const result = await passwordLogin(
        account.trim(), password, accountType,
        captcha?.ticket ?? '', captcha?.randstr ?? ''
      );
      if (!mounted.current) return;
      setPhase('success');
      setMessage(`登录成功，学号 ${result.userId}`);
      window.setTimeout(() => {
        if (mounted.current) onLogin(result.userId, result.mock);
      }, 800);
    } catch (e) {
      if (!mounted.current) return;
      setPhase('error');
      setMessage(`登录失败：${(e as Error).message}`);
    } finally {
      setPwdBusy(false);
    }
  };

  /** 演示模式：模拟手机端点击「确认登录」。 */
  const handleMockConfirm = async () => {
    if (!qr) return;
    try {
      const ok = await mockConfirm(qr.ticket);
      if (!ok) setMessage('该二维码已失效，请重新获取');
    } catch (e) {
      setMessage(`模拟确认失败：${(e as Error).message}`);
    }
  };

  // 等待阶段的倒计时展示
  useEffect(() => {
    if (phase !== 'waiting' || !qr) return;
    const timer = window.setInterval(() => {
      setCountdown(c => Math.max(0, c - 1));
    }, 1000);
    return () => window.clearInterval(timer);
  }, [phase, qr]);

  return (
    <div className="min-h-screen bg-[#F2F2F7] text-[#1C1C1E] flex items-center justify-center px-5 py-8">
      <div className="w-full max-w-[420px] bg-white rounded-[32px] border border-black/[0.06] shadow-sm p-6 sm:p-8 space-y-6">
        {/* 头部 */}
        <div className="text-center space-y-1.5">
          <div className="w-12 h-12 mx-auto rounded-2xl bg-[#007AFF] flex items-center justify-center text-white">
            <Smartphone className="w-6 h-6" />
          </div>
          <h1 className="text-xl font-bold tracking-tight">绑定雨课堂账号</h1>
          <p className="text-xs text-[#8E8E93]">登录后即可使用免扫码进班与随堂补答</p>
        </div>

        {/* 登录方式切换 */}
        <div className="flex bg-[#F2F2F7] rounded-xl p-1 gap-1">
          {([['qr', '微信扫码', QrCode], ['password', '账号密码', KeyRound]] as const).map(([key, label, Icon]) => (
            <button
              key={key}
              onClick={() => { setLoginMode(key); setPhase('idle'); setMessage(''); }}
              className={`flex-1 py-2 rounded-lg text-xs font-semibold flex items-center justify-center gap-1.5 transition-all ${
                loginMode === key ? 'bg-white text-[#007AFF] shadow-sm' : 'text-[#8E8E93]'
              }`}
            >
              <Icon className="w-3.5 h-3.5" /> {label}
            </button>
          ))}
        </div>

        {/* ============ 扫码登录 ============ */}
        {loginMode === 'qr' && (
        <div className="space-y-6">
        {/* 链路提示 */}
        <div className="flex items-center justify-center gap-2 text-[11px] text-[#8E8E93]">
          <span className="flex items-center gap-1"><span className="w-1.5 h-1.5 rounded-full bg-[#007AFF]" />用户端</span>
          <span>→</span>
          <span>后端网关</span>
          <span>→</span>
          <span>雨课堂服务端</span>
        </div>

        {/* 二维码区域 */}
        <div className="flex justify-center">
          {phase === 'idle' || phase === 'error' ? (
            <button
              onClick={() => void startLogin()}
              className="w-full py-3.5 rounded-2xl bg-[#007AFF] text-white text-sm font-bold hover:bg-[#0066CC] active:scale-[0.99] transition-all flex items-center justify-center gap-2"
            >
              <QrCode className="w-4 h-4" /> 获取登录二维码
            </button>
          ) : phase === 'loading' ? (
            <div className="flex items-center gap-2 text-sm text-[#8E8E93] py-8">
              <Loader2 className="w-5 h-5 animate-spin text-[#007AFF]" /> 正在申请二维码…
            </div>
          ) : (
            <div className="relative w-[220px] h-[220px] rounded-2xl border border-black/[0.06] bg-white p-3 flex items-center justify-center overflow-hidden">
              {qr?.qrDataUrl.startsWith('data:') ? (
                <img src={qr.qrDataUrl} alt="登录二维码" className="w-full h-full object-contain" />
              ) : qr?.qrDataUrl ? (
                <img src={qr.qrDataUrl} alt="登录二维码" className="w-full h-full object-contain" referrerPolicy="no-referrer" />
              ) : null}
              {phase === 'success' && (
                <div className="absolute inset-0 bg-white/85 backdrop-blur-sm flex flex-col items-center justify-center gap-1.5">
                  <CheckCircle2 className="w-10 h-10 text-emerald-500" />
                  <span className="text-sm font-bold text-emerald-600">登录成功</span>
                </div>
              )}
            </div>
          )}
        </div>

        {/* 状态与操作 */}
        {phase === 'waiting' && qr && (
          <div className="space-y-3">
            <div className="flex items-center justify-center gap-2 text-xs text-[#3A3A3C]">
              <Loader2 className="w-3.5 h-3.5 animate-spin text-[#007AFF]" />
              <span>{qr.mockMode ? '等待模拟扫码确认' : '请用微信扫码并在手机上确认'}</span>
              <span className="font-mono text-[#8E8E93]">{countdown}s</span>
            </div>
            <div className="flex items-center justify-center gap-1.5 text-[11px] text-emerald-600">
              <Wifi className="w-3 h-3" /> 已建立长连接，确认瞬间零延迟跳转
            </div>
            {qr.mockMode && (
              <button
                onClick={() => void handleMockConfirm()}
                className="w-full py-3 rounded-2xl bg-[#F2F2F7] text-[#1C1C1E] text-xs font-semibold hover:bg-[#E5E5EA] active:scale-[0.99] transition-all"
              >
                🧪 演示：模拟手机端点击「确认登录」
              </button>
            )}
            <button
              onClick={() => void startLogin()}
              className="w-full py-2 rounded-2xl text-[#8E8E93] text-xs flex items-center justify-center gap-1.5 hover:bg-[#F2F2F7]"
            >
              <RefreshCw className="w-3.5 h-3.5" /> 重新获取二维码
            </button>
          </div>
        )}

        </div>
        )}

        {/* ============ 账号密码登录 ============ */}
        {loginMode === 'password' && (
        <div className="space-y-4">
          <div className="flex rounded-xl bg-[#F2F2F7] p-1">
            {([['PP', '手机号'], ['E', '邮箱']] as const).map(([v, label]) => (
              <button key={v} onClick={() => setAccountType(v)}
                className={`flex-1 py-1.5 rounded-lg text-xs font-medium transition-all ${
                  accountType === v ? 'bg-white text-[#007AFF] shadow-sm' : 'text-[#8E8E93]'}`}>
                {label}
              </button>
            ))}
          </div>
          <input
            value={account}
            onChange={e => setAccount(e.target.value)}
            placeholder={accountType === 'PP' ? '手机号' : '邮箱'}
            className="w-full px-4 py-3 rounded-xl bg-[#F2F2F7] text-sm outline-none focus:ring-2 focus:ring-[#007AFF]/30"
          />
          <input
            type="password"
            value={password}
            onChange={e => setPassword(e.target.value)}
            onKeyDown={e => { if (e.key === 'Enter') void handlePasswordLogin(); }}
            placeholder="密码"
            className="w-full px-4 py-3 rounded-xl bg-[#F2F2F7] text-sm outline-none focus:ring-2 focus:ring-[#007AFF]/30"
          />
          <button onClick={showCaptcha}
            className={`w-full py-2.5 rounded-xl text-xs font-semibold border ${
              captcha ? 'border-emerald-300 bg-emerald-50 text-emerald-700' : 'border-[#007AFF]/30 bg-blue-50 text-[#007AFF]'}`}>
            {captcha ? '✓ 滑块验证已完成' : '点击完成滑块安全验证'}
          </button>
          <p className="text-[10px] text-[#AEAEB2] leading-relaxed">
            雨课堂要求滑块验证，密码将用官方 RSA 公钥加密后传输，本项目不留存明文密码。
          </p>
          <button
            onClick={() => void handlePasswordLogin()}
            disabled={pwdBusy}
            className="w-full py-3.5 rounded-2xl bg-[#007AFF] text-white text-sm font-bold hover:bg-[#0066CC] active:scale-[0.99] transition-all flex items-center justify-center gap-2 disabled:opacity-60"
          >
            {pwdBusy ? <Loader2 className="w-4 h-4 animate-spin" /> : <KeyRound className="w-4 h-4" />}
            登录
          </button>
        </div>
        )}

        {phase === 'error' && (
          <div className="rounded-2xl bg-red-50 border border-red-100 p-3.5 text-xs text-red-700 leading-relaxed">
            {message}
          </div>
        )}

        {phase === 'success' && (
          <p className="text-center text-xs text-emerald-600 font-medium">{message}</p>
        )}

        <p className="text-center text-[10px] text-[#AEAEB2] leading-relaxed">
          账号凭据仅保存在本地并加密，工小湖后端不留存隐私数据
        </p>
      </div>
    </div>
  );
}
