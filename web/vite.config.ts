import { fileURLToPath, URL } from 'node:url'
import { defineConfig, type ProxyOptions } from 'vite'
import vue from '@vitejs/plugin-vue'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import type { ComponentResolver } from 'unplugin-vue-components'
import { elementPlusEntries } from './build/element-plus-entries'

/**
 * Element Plus 的总入口（element-plus/es）里有 `const install = installer.install` 这样的顶层语句，
 * 打包时会把所有组件都留下（约 950KB）。这里把按需引入改成从各组件自己的入口文件引入。
 */
const entries = elementPlusEntries()
const elementPlus = ([] as ComponentResolver[]).concat(ElementPlusResolver({ importStyle: 'css' })).map((r): ComponentResolver => {
  if (typeof r === 'function') return r
  return {
    type: r.type,
    async resolve(name) {
      const out = await r.resolve(name)
      if (!out || typeof out === 'string' || out.from !== 'element-plus/es' || !out.name) return out
      const dir = entries.get(out.name)
      return dir ? { ...out, from: `element-plus/es/components/${dir}/index.mjs` } : out
    },
  }
})

export default defineConfig(({ mode }) => ({
  plugins: [
    vue(),
    // Element Plus 按需引入：模板里用到哪个组件才打包哪个（类型由 tsconfig 的 element-plus/global 提供）
    Components({ resolvers: elementPlus, dts: false }),
  ],
  build: {
    rollupOptions: {
      output: {
        // Vue 全家桶单独成块，业务代码更新时用户不用重新下载；Element Plus 组件跟着用到它的页面按需拆分
        manualChunks(id) {
          // vite 的模块 id 统一用 / 分隔（Windows 下也是）
          if (!id.includes('/node_modules/')) return
          if (/\/(vue|@vue|vue-router|pinia)\//.test(id)) return 'vue'
        },
      },
    },
  },
  resolve: {
    alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) },
  },
  server: {
    port: 5173,
    // 默认：把 /api 转发到本地 Spring Boot，和线上 Nginx 的行为一致，不会有跨域问题
    // test 模式（pnpm test）：/api-dev 转发到测试环境，上传的图片和数据都落在测试环境，不写本地
    // 图片地址形如 /api-dev/files/xxx.jpg，同样经此代理访问
    proxy: (mode === 'test'
      ? { '/api-dev': { target: 'https://sheyinglai.top', changeOrigin: true, secure: true } }
      // 本地后端不在 8080 时可以用环境变量指定，如 API_TARGET=http://localhost:18080 pnpm dev
      : { '/api': { target: process.env.API_TARGET || 'http://localhost:8080', changeOrigin: true } }) as Record<string, ProxyOptions>,
  },
}))
