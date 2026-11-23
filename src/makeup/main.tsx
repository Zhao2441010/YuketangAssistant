import React from 'react';
import { createRoot } from 'react-dom/client';
import MakeupApp from './MakeupApp';
import { setSessionId, setToken } from './api';
import '../index.css';

// 支持 ?token=...&sid=... 免扫码登录（用于已有会话直接访问 / 调试）
const params = new URLSearchParams(window.location.search);
const urlToken = params.get('token');
const urlSid = params.get('sid');
if (urlToken) setToken(urlToken);
if (urlSid) setSessionId(urlSid);
if (urlToken || urlSid) {
  window.history.replaceState({}, document.title, window.location.pathname);
}

createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <MakeupApp />
  </React.StrictMode>
);
