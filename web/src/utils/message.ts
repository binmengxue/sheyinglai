// 从组件自己的入口引入，不经过 element-plus 总入口（见 vite.config.ts）
import { ElMessage } from 'element-plus/es/components/message/index.mjs'
import { ElMessageBox } from 'element-plus/es/components/message-box/index.mjs'

/** 轻提示：同一句话连续出现时合并显示，避免刷屏 */
const show = (type: 'success' | 'error' | 'warning' | 'info') => (message: string) =>
  ElMessage({ type, message, grouping: true, duration: type === 'error' ? 3000 : 2000 })

export const toast = {
  success: show('success'),
  error: show('error'),
  warning: show('warning'),
  info: show('info'),
}

/** 确认框：点确定返回 true，取消或关闭返回 false */
export function confirm(
  message: string,
  { title = '提示', type = 'warning', confirmText = '确定' }: { title?: string; type?: 'info' | 'warning'; confirmText?: string } = {},
) {
  return ElMessageBox.confirm(message, title, { type, confirmButtonText: confirmText, cancelButtonText: '取消' })
    .then(() => true)
    .catch(() => false)
}

/** 包一层异步操作：上一次还没结束时再次点击直接忽略，防止重复提交 */
export function singleFlight<A extends unknown[]>(fn: (...args: A) => Promise<unknown>) {
  let running = false
  return async (...args: A) => {
    if (running) return
    running = true
    try {
      await fn(...args)
    } finally {
      running = false
    }
  }
}

/** 输入框确认：点确定返回输入的文字（可能为空字符串），取消返回 null */
export function prompt(
  message: string,
  { title = '提示', placeholder = '', confirmText = '确定', type, inputType = 'textarea' }: {
    title?: string; placeholder?: string; confirmText?: string; type?: 'info' | 'warning'; inputType?: 'textarea' | 'password'
  } = {},
) {
  return ElMessageBox.prompt(message, title, {
    type, inputPlaceholder: placeholder, confirmButtonText: confirmText, cancelButtonText: '取消', inputType,
    inputValidator: (v) => !v || v.length <= 200 || '最多 200 字',
  })
    .then(({ value }) => (value ?? '').trim())
    .catch(() => null)
}
