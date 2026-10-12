import { request } from '@/utils/request'
import type { AdminUserView, PageResult } from '@/types'

export const adminUsers = (keyword: string, page = 1, size = 20) =>
  request<PageResult<AdminUserView>>({ url: '/admin/users', params: { keyword: keyword || undefined, page, size } })

export const setBanned = (id: number, value: boolean) =>
  request<void>({ url: `/admin/users/${id}/banned`, method: 'put', params: { value } })

export const setPhotographerVisible = (id: number, value: boolean) =>
  request<void>({ url: `/admin/photographers/${id}/visible`, method: 'put', params: { value } })

export const deleteReview = (id: number) => request<void>({ url: `/admin/reviews/${id}`, method: 'delete' })
