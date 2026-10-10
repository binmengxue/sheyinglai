import { request } from '@/utils/request'

export const login = (username: string, password: string) =>
  request<{ token: string; username: string }>({ url: '/auth/login', method: 'post', data: { username, password } })

export const me = () => request<{ loggedIn: boolean; username: string }>({ url: '/auth/me' })

export const logout = () => request<void>({ url: '/auth/logout', method: 'post' })
