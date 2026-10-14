import { createApp } from 'vue'
import { createPinia } from 'pinia'
// Element Plus 组件和样式由 vite 插件按需引入；基础样式先引入，保证 style.css 里的主题变量能覆盖它
import 'element-plus/theme-chalk/base.css'
import 'element-plus/es/components/message/style/css'
import 'element-plus/es/components/message-box/style/css'
import 'element-plus/es/components/notification/style/css'
import App from './App.vue'
import router from './router'
import { registerIcons } from './icons'
import { isHandledError } from './utils/request'
import { toast } from './utils/message'
import './style.css'

const app = createApp(App)
registerIcons(app)

// 兜底：没被处理的错误给个提示，接口错误拦截器已经提示过就不重复
app.config.errorHandler = (err) => {
  console.error(err)
  if (!isHandledError(err)) toast.error('操作失败，请刷新页面后重试')
}
window.addEventListener('unhandledrejection', (e) => {
  if (isHandledError(e.reason)) e.preventDefault()
})
app.use(createPinia()).use(router)
app.mount('#app')
