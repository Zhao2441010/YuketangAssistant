// 免扫码进课堂会话：调用 Android 原生插件注入官方域 Cookie 并在 App 内 WebView 打开固定入口。
// 这是 APK「进入课堂」的默认路径；Web 端无此插件（isNative=false），由调用方降级到后端自渲染。
import { registerPlugin } from '@capacitor/core';
import { Capacitor } from '@capacitor/core';

export interface ClassroomEntryOpenOptions {
  url: string;       // /lesson/fullscreen/v3/{lessonId}?source=5
  cookie: string;    // 完整 L1 Cookie 串
  domain: string;    // https://changjiang.yuketang.cn
}
export interface ClassroomEntryResult {
  opened: boolean;
  url: string;
}

interface ClassroomEntryPlugin {
  open(options: ClassroomEntryOpenOptions): Promise<ClassroomEntryResult>;
}

const ClassroomEntry = registerPlugin<ClassroomEntryPlugin>('ClassroomEntry');

/** 是否为原生 App（Capacitor Android / iOS）。 */
export const isNativeApp = Capacitor.isNativePlatform();

/**
 * 以在读学生身份直接进入课堂会话：不经过 checkin / 动态码。
 * 仅原生端可用；Web 端抛错由调用方降级。
 */
export async function openOfficialClassroomSession(
  options: ClassroomEntryOpenOptions
): Promise<ClassroomEntryResult> {
  if (!isNativeApp) {
    throw new Error('当前为 Web 端，无法注入官方域 Cookie；请使用 APK 或配置同源反向代理');
  }
  return ClassroomEntry.open(options);
}
