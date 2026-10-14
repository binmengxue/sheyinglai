import { ElMessage, ElMessageBox } from 'element-plus'

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
