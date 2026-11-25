import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
  appId: 'site.jiclub.gongxiaohu.app',
  appName: 'yuzhushou',
  webDir: 'dist',
  server: {
    // Android 上 WebView 允许访问后端接口
    androidScheme: 'http'
  }
};

export default config;
