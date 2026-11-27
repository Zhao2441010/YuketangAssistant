// 本地账号凭据仓库（v1.2 持久登录 + 多账号）
//
// 设计目标：
// 1. 关闭 App 再打开、后端重启，都还能恢复上一次的登录态；
// 2. 可以保存多个雨课堂账号，随时一键切换；
// 3. Android WebView 里 localStorage 不可靠，所以优先写原生 SharedPreferences
//    （SessionStore 插件），失败时才退回 localStorage。
//
// 登录凭据只存在本机：token + 真实 sessionid（用于 token 失效后无感重导）。

import { Capacitor, registerPlugin } from '@capacitor/core';

export type LoginKind = 'qr' | 'password' | 'cookie' | 'url' | 'legacy';

export interface StoredAccount {
  id: string;
  userId: string;
  token: string;
  sessionId: string;
  mock: boolean;
  kind: LoginKind;
  createdAt: number;
  lastUsedAt: number;
  expiresAt?: number;
}

export interface AuthSnapshot {
  version: 2;
  activeId: string | null;
  accounts: StoredAccount[];
}

interface SessionStoreBridge {
  get(options: { key: string }): Promise<{ value: string | null }>;
  set(options: { key: string; value: string }): Promise<void>;
  remove(options: { key: string }): Promise<void>;
}

const SessionStore = registerPlugin<SessionStoreBridge>('SessionStore');

const STORE_KEY = 'gxh_auth_v2';
const LEGACY_TOKEN_KEY = 'gxH_sessionToken';
const LEGACY_SESSION_KEY = 'gxH_sessionId';
const MAX_ACCOUNTS = 8;
const isNative = Capacitor.isNativePlatform();

let snapshot: AuthSnapshot = { version: 2, activeId: null, accounts: [] };
let hydrated = false;
let hydratePromise: Promise<void> | null = null;
let pendingToken: string | null = null;
let pendingSessionId: string | null = null;

function newId(): string {
  return `acc-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 8)}`;
}

function normalize(raw: unknown): AuthSnapshot | null {
  if (!raw || typeof raw !== 'object') return null;
  const obj = raw as Partial<AuthSnapshot>;
  if (!Array.isArray(obj.accounts)) return null;
  const accounts: StoredAccount[] = [];
  for (const item of obj.accounts) {
    if (!item || typeof item !== 'object') continue;
    const a = item as Partial<StoredAccount>;
    if (typeof a.token !== 'string' && typeof a.sessionId !== 'string') continue;
    accounts.push({
      id: typeof a.id === 'string' && a.id ? a.id : newId(),
      userId: typeof a.userId === 'string' ? a.userId : '',
      token: typeof a.token === 'string' ? a.token : '',
      sessionId: typeof a.sessionId === 'string' ? a.sessionId : '',
      mock: a.mock === true,
      kind: (a.kind as LoginKind) || 'legacy',
      createdAt: typeof a.createdAt === 'number' ? a.createdAt : Date.now(),
      lastUsedAt: typeof a.lastUsedAt === 'number' ? a.lastUsedAt : Date.now(),
      expiresAt: typeof a.expiresAt === 'number' ? a.expiresAt : undefined
    });
  }
  const activeId = accounts.some(a => a.id === obj.activeId) ? (obj.activeId as string) : (accounts[0]?.id ?? null);
  return { version: 2, activeId, accounts };
}

async function readStore(): Promise<string | null> {
  if (isNative) {
    try {
      const res = await SessionStore.get({ key: STORE_KEY });
      if (res && typeof res.value === 'string' && res.value) return res.value;
    } catch { /* 插件不可用时退回 localStorage */ }
  }
  try {
    return localStorage.getItem(STORE_KEY);
  } catch {
    return null;
  }
}

async function writeStore(value: string): Promise<void> {
  let nativeOk = false;
  if (isNative) {
    try {
      await SessionStore.set({ key: STORE_KEY, value });
      nativeOk = true;
    } catch { /* 退回 localStorage */ }
  }
  if (!nativeOk) {
    try { localStorage.setItem(STORE_KEY, value); } catch { /* 内存里仍有一份 */ }
  }
}

let writeQueue: Promise<void> = Promise.resolve();

/** 把当前内存快照刷到本机（串行化，避免并发写乱序）。 */
export function persistAuth(): Promise<void> {
  const payload = JSON.stringify(snapshot);
  writeQueue = writeQueue.then(() => writeStore(payload)).catch(() => { /* 忽略落盘失败 */ });
  return writeQueue;
}

function saveSnapshot(): void {
  void persistAuth();
}

function migrateLegacy(): void {
  try {
    const token = localStorage.getItem(LEGACY_TOKEN_KEY);
    const sid = localStorage.getItem(LEGACY_SESSION_KEY) || '';
    if (token || sid) {
      const now = Date.now();
      const acc: StoredAccount = {
        id: newId(),
        userId: '',
        token: token || '',
        sessionId: sid,
        mock: false,
        kind: 'legacy',
        createdAt: now,
        lastUsedAt: now
      };
      snapshot.accounts = [acc];
      snapshot.activeId = acc.id;
    }
    localStorage.removeItem(LEGACY_TOKEN_KEY);
    localStorage.removeItem(LEGACY_SESSION_KEY);
  } catch { /* 无旧数据 */ }
}

function applyPending(): void {
  const token = pendingToken || '';
  const sid = pendingSessionId || '';
  pendingToken = null;
  pendingSessionId = null;
  if (!token && !sid) return;
  const now = Date.now();
  const existing = snapshot.accounts.find(a => (token && a.token === token) || (sid && a.sessionId === sid));
  if (existing) {
    if (token) existing.token = token;
    if (sid) existing.sessionId = sid;
    existing.lastUsedAt = now;
    snapshot.activeId = existing.id;
    return;
  }
  const acc: StoredAccount = {
    id: newId(),
    userId: '',
    token,
    sessionId: sid,
    mock: false,
    kind: 'url',
    createdAt: now,
    lastUsedAt: now
  };
  snapshot.accounts.unshift(acc);
  snapshot.activeId = acc.id;
  trimAccounts();
}

async function doHydrate(): Promise<void> {
  const raw = await readStore();
  let loaded: AuthSnapshot | null = null;
  if (raw) {
    try { loaded = normalize(JSON.parse(raw)); } catch { loaded = null; }
  }
  if (loaded) {
    snapshot = loaded;
  } else {
    migrateLegacy();
  }
  applyPending();
  hydrated = true;
  await persistAuth();
}

/** 读取本机账号快照。可重复调用，只真正执行一次。 */
export function hydrateAuth(): Promise<void> {
  if (!hydratePromise) hydratePromise = doHydrate();
  return hydratePromise;
}

export function isHydrated(): boolean {
  return hydrated;
}

function trimAccounts(): void {
  if (snapshot.accounts.length <= MAX_ACCOUNTS) return;
  const active = snapshot.accounts.find(a => a.id === snapshot.activeId);
  const rest = snapshot.accounts
    .filter(a => a.id !== snapshot.activeId)
    .sort((a, b) => b.lastUsedAt - a.lastUsedAt)
    .slice(0, MAX_ACCOUNTS - (active ? 1 : 0));
  snapshot.accounts = active ? [active, ...rest] : rest;
}

export function getAccounts(): StoredAccount[] {
  return snapshot.accounts.map(a => ({ ...a }));
}

export function getActiveAccount(): StoredAccount | null {
  const acc = snapshot.accounts.find(a => a.id === snapshot.activeId);
  return acc ? { ...acc } : null;
}

export function getToken(): string | null {
  if (!hydrated && pendingToken) return pendingToken;
  return getActiveAccount()?.token || null;
}

export function getSessionId(): string | null {
  if (!hydrated && pendingSessionId) return pendingSessionId;
  return getActiveAccount()?.sessionId || null;
}

export function setToken(token: string): void {
  if (!hydrated) { pendingToken = token; return; }
  const acc = getActiveAccount();
  if (acc) {
    updateAccount(acc.id, { token });
  } else {
    const now = Date.now();
    snapshot.accounts.unshift({
      id: newId(), userId: '', token, sessionId: '', mock: false,
      kind: 'url', createdAt: now, lastUsedAt: now
    });
    snapshot.activeId = snapshot.accounts[0].id;
  }
  saveSnapshot();
}

export function setSessionId(sid: string): void {
  if (!hydrated) { pendingSessionId = sid; return; }
  const acc = getActiveAccount();
  if (acc) {
    updateAccount(acc.id, { sessionId: sid });
  } else {
    const now = Date.now();
    snapshot.accounts.unshift({
      id: newId(), userId: '', token: '', sessionId: sid, mock: false,
      kind: 'url', createdAt: now, lastUsedAt: now
    });
    snapshot.activeId = snapshot.accounts[0].id;
  }
  saveSnapshot();
}

/** 只清掉当前账号的 token（保留 sessionid，下次可无感重建）。 */
export function clearToken(): void {
  const acc = getActiveAccount();
  if (!acc) return;
  const target = snapshot.accounts.find(a => a.id === acc.id);
  if (!target) return;
  target.token = '';
  saveSnapshot();
}

export interface SaveLoginInput {
  userId: string;
  token: string;
  sessionId: string;
  mock: boolean;
  kind: LoginKind;
  expiresAt?: number;
}

/** 登录成功后保存账号；同一 userId 视为同一账号，覆盖凭据并置为当前账号。 */
export function saveLoginAccount(input: SaveLoginInput): StoredAccount {
  const now = Date.now();
  const uid = (input.userId || '').trim();
  let acc = uid ? snapshot.accounts.find(a => a.userId === uid) : undefined;
  if (!acc && !uid) acc = snapshot.accounts.find(a => a.token === input.token);
  if (acc) {
    acc.userId = uid || acc.userId;
    acc.token = input.token;
    acc.sessionId = input.sessionId || acc.sessionId;
    acc.mock = input.mock;
    acc.kind = input.kind;
    acc.lastUsedAt = now;
    acc.expiresAt = input.expiresAt;
  } else {
    acc = {
      id: newId(),
      userId: uid,
      token: input.token,
      sessionId: input.sessionId,
      mock: input.mock,
      kind: input.kind,
      createdAt: now,
      lastUsedAt: now,
      expiresAt: input.expiresAt
    };
    snapshot.accounts.unshift(acc);
  }
  snapshot.activeId = acc.id;
  trimAccounts();
  saveSnapshot();
  return { ...acc };
}

function updateAccount(id: string, patch: Partial<StoredAccount>): void {
  const acc = snapshot.accounts.find(a => a.id === id);
  if (!acc) return;
  Object.assign(acc, patch);
  acc.lastUsedAt = patch.lastUsedAt ?? Date.now();
  saveSnapshot();
}

export function updateActiveAccount(patch: Partial<StoredAccount>): void {
  const acc = getActiveAccount();
  if (!acc) return;
  updateAccount(acc.id, patch);
}

export function activateAccount(id: string): StoredAccount | null {
  const acc = snapshot.accounts.find(a => a.id === id);
  if (!acc) return null;
  acc.lastUsedAt = Date.now();
  snapshot.activeId = id;
  saveSnapshot();
  return { ...acc };
}

export function removeAccount(id: string): void {
  const before = snapshot.accounts.length;
  snapshot.accounts = snapshot.accounts.filter(a => a.id !== id);
  if (snapshot.accounts.length === before) return;
  if (snapshot.activeId === id) snapshot.activeId = null;
  saveSnapshot();
}
