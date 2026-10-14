import type { AxiosRequestConfig } from 'axios'
import { request } from '@/utils/request'
import type { Me, User } from '@/types'

interface LoginResult { token: string; me: Me }

export const login = (phone: string, password: string) =>
  request<LoginResult>({ url: '/auth/login', method: 'post', data: { phone, password } })

export const register = (data: { phone: string; password: string; nickname?: string; agree: boolean }) =>
  request<LoginResult>({ url: '/auth/register', method: 'post', data })

export const me = (config?: AxiosRequestConfig) => request<Me>({ url: '/auth/me', ...config })

export const logout = (config?: AxiosRequestConfig) => request<void>({ url: '/auth/logout', method: 'post', ...config })

export const updateProfile = (data: Pick<User, 'nickname' | 'gender' | 'bio' | 'city' | 'wechat'>) =>
  request<User>({ url: '/users/me', method: 'put', data })

export const changePassword = (oldPassword: string, newPassword: string) =>
  request<void>({ url: '/users/me/password', method: 'put', data: { oldPassword, newPassword } })

export function uploadAvatar(file: File) {
  const form = new FormData()
  form.append('file', file)
  return request<User>({ url: '/users/me/avatar', method: 'post', data: form, timeout: 60 * 1000 })
}

/** 注销账号，需要密码确认 */
export const deleteAccount = (password: string) =>
  request<void>({ url: '/users/me/delete', method: 'post', data: { password } })
