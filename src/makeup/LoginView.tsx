import React, { useEffect, useRef, useState } from 'react';
import { CheckCircle2, ChevronRight, Loader2, QrCode, RefreshCw, Smartphone, Wifi, X } from 'lucide-react';
import { fetchLoginQr, LoginQr, mockConfirm, pollLogin } from './api';
import type { StoredAccount } from './authStore';

interface Props {
  onLogin: (userId: string, mock: boolean) => void;
  /** 从"添加账号"进入时可取消，回到原账号。 */
  onCancel?: () => void;
  /** 正在添加新账号（而不是首次登录）。 */
  addingAccount?: boolean;
  /** 本机已保存的账号，点击可一键切换。 */
  savedAccounts?: StoredAccount[];
  activeAccountId?: string | null;
  onSwitchAccount?: (id: string) => void;
  busyAccountId?: string | null;
}

type Phase = 'idle' | 'loading' | 'waiting' | 'success' | 'error';

/** 把时间戳变成"刚刚 / N 分钟前 / 昨天"这种更好读的文案。 */
function formatLastUsed(ts: number): string {
  const diff = Date.now() - ts;
  if (diff < 60_000) return '刚刚';
  if (diff < 3_600_000) return `${Math.floor(diff / 60_000)} 分钟前`;
  if (diff < 86_400_000) return `${Math.floor(diff / 3_600_000)} 小时前`;
  const d = new Date(ts);
  return `${d.getMonth() + 1}月${d.getDate()}日`;
}

/** 扫码登录页：用户端 → 后端 → 雨课堂端 链路的可视化入口。 */
export default function LoginView({
  onLogin,
  onCancel,
  addingAccount = false,
  savedAccounts = [],
  activeAccountId = null,
  onSwitchAccount,
  busyAccountId = null
}: Props) {
  const [qr, setQr] = useState<LoginQr | null>(null);
  const [phase, setPhase] = useState<Phase>('idle');
  const [message, setMessage] = useState('');
  const [countdown, setCountdown] = useState(45);
  // 轮询会话 ID：只有主动刷新二维码/卸载页面才 +1 使旧轮询失效。
  // 不依赖 AbortController.signal.aborted——Android WebView 会主动 abort 连接，
  // 若据此停止轮询，扫码后前端将永远无反应。
  const pollSession = useRef(0);
  const mounted = useRef(true);
  /** 登录成功后的凭据快照：自动跳转被环境拦截时，兜底按钮直接进首页。 */
  const successResult = useRef<{ userId: string; mock: boolean } | null>(null);
  /** 连续轮询失败次数，用于提示"扫码确认可能没到后端"。 */
  const [pollTries, setPollTries] = useState(0);
  /** 最近一次轮询的返回，仅用于页面上的排障提示（不影响登录逻辑）。 */
  const [lastPoll, setLastPoll] = useState('');

  useEffect(() => {
    mounted.current = true;
    return () => {
      mounted.current = false;
      pollSession.current += 1;
    };
  }, []);

  /** 创建二维码并立刻开始短轮询。 */
  const startLogin = async () => {
    pollSession.current += 1;           // 使旧轮询失效
    const session = pollSession.current;
    setPhase('loading');
    setMessage('');
    setLastPoll('');
    setQr(null);
    setPollTries(0);
    successResult.current = null;
    try {
      const loginQr = await fetchLoginQr();
      if (!mounted.current || session !== pollSession.current) return;
      setQr(loginQr);
      setCountdown(Math.max(0, Math.ceil((loginQr.expiresAt - Date.now()) / 1000)));
      setPhase('waiting');
      void beginPoll(loginQr, session);
    } catch (e) {
      if (!mounted.current) return;
      setPhase('error');
      setMessage(`获取二维码失败：${(e as Error).message}。请确认后端服务已启动。`);
    }
  };

  /**
   * 短轮询：每 1.2 秒问一次，单次请求 1 秒内必定返回。
   *
   * 为什么不再用 45 秒长轮询（2026-11-25 实测结论）：
   * Android WebView / 运营商 NAT 会把空闲 30-40 秒的连接静默掐断。后端把"扫码成功"
   * 写回这条已死连接时，前端只收到网络错误并继续重试，而服务端 flow 已被清理，
   * 后续轮询全是"尚未确认"——这就是"后端日志 LOGIN_OK、手机停在二维码页"的真正成因。
   * 短轮询没有长空闲连接；再加上后端按 ticket 幂等返回，某次响应丢了下次也能取回 token。
   */
  const beginPoll = async (loginQr: LoginQr, session: number) => {
    if (!mounted.current || session !== pollSession.current) return;
    setCountdown(Math.max(0, Math.ceil((loginQr.expiresAt - Date.now()) / 1000)));
    try {
      const result = await pollLogin(loginQr.ticket, 0);
      if (!mounted.current) return;
      // 防御：后端返回异常结构时不要把 undefined 学号带进主界面（对齐小程序 loginSuccess）
      if (!result?.token || !result?.userId) {
        throw new Error('登录返回缺少 token/学号');
      }
      successResult.current = { userId: result.userId, mock: result.mock };
      setPollTries(0);
      setLastPoll('已确认');
      setPhase('success');
      setMessage(`登录成功，学号 ${result.userId}`);
      // 立即提交登录态，不做定时器/会话判断：成功响应已经拿到，任何延迟或拦截都可能把它弄丢
      onLogin(result.userId, result.mock);
    } catch (e) {
      // 无论 409、网络抖动还是 WebView 中断：只要二维码未过期就继续轮询
      if (!mounted.current || session !== pollSession.current) return;
      setPollTries(n => n + 1);
      setLastPoll((e as Error).message || '等待扫码确认');
      if (Date.now() < loginQr.expiresAt) {
        setCountdown(Math.max(0, Math.ceil((loginQr.expiresAt - Date.now()) / 1000)));
        window.setTimeout(() => { void beginPoll(loginQr, session); }, 1200);
        return;
      }
      setPhase('error');
      setMessage('二维码已过期，请点击刷新重新获取');
    }
  };

  /** 自动跳转被拦截时的兜底入口（对齐小程序 login.goHome）。 */
  const enterAppManually = () => {
    const r = successResult.current;
    if (!r) { setPhase('error'); setMessage('尚未登录，请重新扫码'); return; }
    onLogin(r.userId, r.mock);
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
      <div className="relative w-full max-w-[420px] bg-white rounded-[32px] border border-black/[0.06] shadow-sm p-6 sm:p-8 space-y-6">
        {onCancel && (
          <button
            onClick={onCancel}
            aria-label="取消"
            className="absolute right-4 top-4 w-8 h-8 rounded-full bg-[#F2F2F7] text-[#8E8E93] flex items-center justify-center hover:bg-[#E5E5EA] active:scale-95 transition-all"
          >
            <X className="w-4 h-4" />
          </button>
        )}

        {/* 头部 */}
        <div className="text-center space-y-1.5">
          <div className="w-12 h-12 mx-auto rounded-2xl bg-[#007AFF] flex items-center justify-center text-white">
            <Smartphone className="w-6 h-6" />
          </div>
          <h1 className="text-xl font-bold tracking-tight">{addingAccount ? '添加雨课堂账号' : '绑定雨课堂账号'}</h1>
          <p className="text-xs text-[#8E8E93]">
            {addingAccount ? '登录后可在多个账号之间随时切换' : '登录一次即保持，重开 App 无需再扫码'}
          </p>
        </div>

        {/* 已保存账号：一键切换 */}
        {savedAccounts.length > 0 && onSwitchAccount && (
          <div className="space-y-2">
            <div className="flex items-center justify-between px-0.5">
              <p className="text-[11px] font-semibold text-[#8E8E93]">本机已保存 {savedAccounts.length} 个账号</p>
              <span className="text-[10px] text-[#AEAEB2]">点击直接登录</span>
            </div>
            <div className="space-y-1.5">
              {savedAccounts.map(acc => {
                const active = acc.id === activeAccountId;
                const busy = acc.id === busyAccountId;
                const initial = (acc.userId || '?').slice(0, 1).toUpperCase();
                return (
                  <button
                    key={acc.id}
                    onClick={() => onSwitchAccount(acc.id)}
                    disabled={busy}
                    className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-2xl border text-left transition-all active:scale-[0.99] disabled:opacity-70 ${
                      active ? 'border-[#007AFF]/30 bg-blue-50/60' : 'border-black/[0.06] bg-[#F9F9FB] hover:bg-[#F2F2F7]'
                    }`}
                  >
                    <span className="w-8 h-8 shrink-0 rounded-full bg-[#007AFF]/10 text-[#007AFF] text-xs font-bold flex items-center justify-center">
                      {initial}
                    </span>
                    <span className="flex-1 min-w-0">
                      <span className="block text-sm font-semibold text-[#1C1C1E] truncate">{acc.userId || '未命名账号'}</span>
                      <span className="block text-[10px] text-[#8E8E93]">
                        {active ? '当前账号 · 点按恢复登录' : `上次使用 ${formatLastUsed(acc.lastUsedAt)}`}
                      </span>
                    </span>
                    {busy
                      ? <Loader2 className="w-4 h-4 animate-spin text-[#007AFF]" />
                      : active
                        ? <span className="text-[10px] font-semibold text-emerald-600">当前</span>
                        : <ChevronRight className="w-4 h-4 text-[#C7C7CC]" />}
                  </button>
                );
              })}
            </div>
            <div className="flex items-center gap-2 pt-1">
              <div className="flex-1 h-px bg-black/[0.06]" />
              <span className="text-[10px] text-[#AEAEB2]">或登录其他账号</span>
              <div className="flex-1 h-px bg-black/[0.06]" />
            </div>
          </div>
        )}

        {/* ============ 扫码登录 ============ */}
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

        {/* 自动跳转失败时的兜底入口：扫码凭据已在本地，直接进首页 */}
        {phase === 'success' && (
          <button
            onClick={enterAppManually}
            className="w-full py-3 rounded-2xl bg-emerald-500 text-white text-sm font-bold hover:bg-emerald-600 active:scale-[0.99] transition-all flex items-center justify-center gap-2"
          >
            <CheckCircle2 className="w-4 h-4" /> 进入首页
          </button>
        )}

        {/* 状态与操作 */}
        {phase === 'waiting' && qr && (
          <div className="space-y-3">
            <div className="flex items-center justify-center gap-2 text-xs text-[#3A3A3C]">
              <Loader2 className="w-3.5 h-3.5 animate-spin text-[#007AFF]" />
              <span>{qr.mockMode ? '等待模拟扫码确认' : '请用微信扫码并在手机上确认'}</span>
              <span className="font-mono text-[#8E8E93]">{countdown}s</span>
            </div>
            <div className="flex items-center justify-center gap-1.5 text-[11px] text-emerald-600">
              <Wifi className="w-3 h-3" /> 每 1.2 秒自动检查一次，确认后立即进入
            </div>
            {pollTries > 0 && (
              <div className="flex items-center justify-center gap-1.5 text-[11px] text-[#8E8E93]">
                <span>已检查 {pollTries} 次</span>
                <span className="max-w-[190px] truncate">· {lastPoll || '等待确认'}</span>
              </div>
            )}
            {pollTries >= 8 && (
              <p className="text-[11px] text-amber-600 text-center leading-relaxed">
                若手机已点「确认登录」，请在后端执行
                <code className="mx-1 px-1 rounded bg-[#F2F2F7]">grep 扫码轮询 /opt/ykt-backend/logs/app.log | tail -3</code>
                看是否出现「扫码轮询成功」。
              </p>
            )}
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

        {phase === 'error' && (
          <div className="rounded-2xl bg-red-50 border border-red-100 p-3.5 text-xs text-red-700 leading-relaxed">
            {message}
          </div>
        )}

        {phase === 'success' && (
          <p className="text-center text-xs text-emerald-600 font-medium">{message}</p>
        )}

        <p className="text-center text-[10px] text-[#AEAEB2] leading-relaxed">
          账号凭据保存在本机；后端仅以加密文件保存会话用于保持登录，不写日志、不回传页面
          <br />构建版本 2026-11-26 · 多账号持久登录版（v1.2）
        </p>
      </div>
    </div>
  );
}
