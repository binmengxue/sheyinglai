import { request } from '@/utils/request'
import type { Notification, PageResult } from '@/types'

export const listNotifications = (page = 1, size = 20) =>
  request<PageResult<Notification>>({ url: '/me/notifications', params: { page, size } })

/** 后台轮询用，失败不弹提示 */
export const unreadCount = () => request<number>({ url: '/me/notifications/unread-count', silent: true })

export const readNotification = (id: number) => request<void>({ url: `/me/notifications/${id}/read`, method: 'post' })

export const readAllNotifications = () => request<void>({ url: '/me/notifications/read-all', method: 'post' })
