import tailwindcss from '@tailwindcss/vite';
import react from '@vitejs/plugin-react';
import {defineConfig} from 'vite';
import {fileURLToPath, URL} from 'node:url';

export default defineConfig(() => {
  return {
    plugins: [react(), tailwindcss()],
    build: {
      // 两个入口均渲染超时补答应用（官网页面已取消）
      // index.html 为根路径入口，makeup.html 为兼容旧链接保留
      rollupOptions: {
        input: {
          main: fileURLToPath(new URL('./index.html', import.meta.url)),
          makeup: fileURLToPath(new URL('./makeup.html', import.meta.url)),
        },
      },
    },
    resolve: {
      alias: {
        '@': fileURLToPath(new URL('.', import.meta.url)),
      },
    },
    server: {
      // HMR is disabled in AI Studio via DISABLE_HMR env var.
      // Do not modify—file watching is disabled to prevent flickering during agent edits.
      hmr: process.env.DISABLE_HMR !== 'true',
      // Disable file watching when DISABLE_HMR is true to save CPU during agent edits.
      watch: process.env.DISABLE_HMR === 'true' ? null : {},
    },
  };
});
