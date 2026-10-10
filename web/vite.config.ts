import { fileURLToPath, URL } from 'node:url'
import { defineConfig, type ProxyOptions } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig(({ mode }) => ({
  plugins: [vue()],
  resolve: {
    alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) },
  },
  server: {
    port: 5173,
    // 默认：把 /api 转发到本地 Spring Boot，和线上 Nginx 的行为一致，不会有跨域问题
    // test 模式（pnpm dev:test）：/api-dev 转发到测试环境，上传的图片和数据都落在测试环境，不写本地
    // 图片地址形如 /api-dev/files/xxx.jpg，同样经此代理访问
    proxy: (mode === 'test'
      ? { '/api-dev': { target: 'https://sheyinglai.top', changeOrigin: true, secure: true } }
      : { '/api': { target: 'http://localhost:8080', changeOrigin: true } }) as Record<string, ProxyOptions>,
  },
}))
