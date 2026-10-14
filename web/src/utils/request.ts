import axios, { type AxiosRequestConfig } from 'axios'
import { toast } from '@/utils/message'
import type { Result } from '@/types'

declare module 'axios' {
  interface AxiosRequestConfig {
    /** 失败时不弹错误提示，由调用方自己处理（后台轮询、批量上传等） */
    silent?: boolean
  }
}

const TOKEN_KEY = 'photo_token'
export const getToken = () => localStorage.getItem(TOKEN_KEY) || ''
export const setToken = (t: string) => localStorage.setItem(TOKEN_KEY, t)
export const clearToken = () => localStorage.removeItem(TOKEN_KEY)

// 前后端同域，接口统一走 /api（开发时由 Vite 代理，线上由 Nginx 转发）
// `pnpm dev:test` 时通过 .env.test 把 VITE_API_BASE 设为 /api-dev，直连测试环境
const http = axios.create({ baseURL: import.meta.env.VITE_API_BASE || '/api', timeout: 15000 })

/** 接口错误：拦截器已经提示过（或调用方要求静默），全局错误处理不再重复提示 */
export class ApiError extends Error {
  readonly handled = true
  constructor(message: string, readonly code?: number) {
    super(message)
  }
}

export const isHandledError = (e: unknown) => !!(e && typeof e === 'object' && (e as { handled?: boolean }).handled)

/** 并发请求同时 401 时只提示、跳转一次 */
let redirecting = false

http.interceptors.request.use((config) => {
  const token = getToken()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

http.interceptors.response.use(
  (res) => {
    const body = res.data as Result<unknown>
    if (body.code !== 0) {
      if (!res.config.silent) toast.error(body.msg || '请求失败')
      return Promise.reject(new ApiError(body.msg, body.code))
    }
    return res
  },
  (err) => {
    err.handled = true
    const silent = err.config?.silent
    if (err.response?.status === 401) {
      clearToken()
      if (!silent && !redirecting) {
        redirecting = true
        toast.warning('登录已过期，请重新登录')
        location.href = '/login?redirect=' + encodeURIComponent(location.pathname + location.search)
      }
    } else if (silent) {
      // 调用方自己处理
    } else if (err.response?.status === 413) {
      // Nginx 超限时返回的是 HTML，没有 msg
      toast.error('图片不可以超过 1M')
    } else if (err.code === 'ECONNABORTED') {
      toast.error('请求超时，请稍后重试')
    } else {
      toast.error(err.response?.data?.msg || (err.response ? '请求失败' : '网络错误，请检查网络连接'))
    }
    return Promise.reject(err)
  },
)

/** 只返回 data 字段 */
export async function request<T>(config: AxiosRequestConfig): Promise<T> {
  const res = await http.request<Result<T>>(config)
  return res.data.data
}
