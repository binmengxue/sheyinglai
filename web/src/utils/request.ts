import axios, { type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import type { Result } from '@/types'

const TOKEN_KEY = 'photo_token'
export const getToken = () => localStorage.getItem(TOKEN_KEY) || ''
export const setToken = (t: string) => localStorage.setItem(TOKEN_KEY, t)
export const clearToken = () => localStorage.removeItem(TOKEN_KEY)

// 前后端同域，接口统一走 /api（开发时由 Vite 代理，线上由 Nginx 转发）
// `pnpm dev:test` 时通过 .env.test 把 VITE_API_BASE 设为 /api-dev，直连测试环境
const http = axios.create({ baseURL: import.meta.env.VITE_API_BASE || '/api', timeout: 15000 })

http.interceptors.request.use((config) => {
  const token = getToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

http.interceptors.response.use(
  (res) => {
    const body = res.data as Result<unknown>
    if (body.code !== 0) {
      ElMessage.error(body.msg || '请求失败')
      return Promise.reject(new Error(body.msg))
    }
    return res
  },
  (err) => {
    if (err.response?.status === 401) {
      clearToken()
      ElMessage.warning('登录已过期，请重新登录')
      location.href = '/login?redirect=' + encodeURIComponent(location.pathname)
    } else if (err.response?.status === 413) {
      // Nginx 超限时返回的是 HTML，没有 msg
      ElMessage.error('图片不可以超过 1M')
    } else {
      ElMessage.error(err.response?.data?.msg || err.message || '网络错误')
    }
    return Promise.reject(err)
  },
)

/** 只返回 data 字段 */
export async function request<T>(config: AxiosRequestConfig): Promise<T> {
  const res = await http.request<Result<T>>(config)
  return res.data.data
}
