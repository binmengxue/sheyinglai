import { request } from '@/utils/request'
import type {
  PageResult, Photographer, UserBrief, PhotographerCard, PhotographerDetail, PhotographerForm, ReviewView, ScheduleView,
} from '@/types'

export type SortKey = 'default' | 'distance' | 'rating' | 'price'

export const listPhotographers = (params: {
  style?: string; sort?: SortKey; keyword?: string; city?: string; minPrice?: number; maxPrice?: number
  lat?: number; lng?: number; page?: number; size?: number
}) => request<PageResult<PhotographerCard>>({ url: '/photographers', params })

/** track=false：不计入主页浏览量 */
export const getPhotographer = (id: number, loc?: { lat: number; lng: number } | null, track = true) =>
  request<PhotographerDetail>({ url: `/photographers/${id}`, params: { ...(loc ?? {}), track } })

export const updateMyPhotographer = (data: PhotographerForm) =>
  request<Photographer>({ url: '/photographers/me', method: 'put', data })

/** month 形如 2026-10 */
export const getSchedule = (id: number, month: string) =>
  request<ScheduleView>({ url: `/photographers/${id}/schedule`, params: { month } })

export const setDayOff = (date: string, off: boolean) =>
  request<void>({ url: '/photographers/me/off', method: 'put', data: { date, off } })

export const listReviews = (id: number, page = 1, size = 20) =>
  request<PageResult<ReviewView>>({ url: `/photographers/${id}/reviews`, params: { page, size } })

export const follow = (id: number) => request<void>({ url: `/photographers/${id}/follow`, method: 'post' })
export const unfollow = (id: number) => request<void>({ url: `/photographers/${id}/follow`, method: 'delete' })

export const listFans = (id: number, page = 1, size = 20) =>
  request<PageResult<UserBrief>>({ url: `/photographers/${id}/fans`, params: { page, size } })

export const listFollowing = (id: number, page = 1, size = 20) =>
  request<PageResult<UserBrief>>({ url: `/photographers/${id}/following`, params: { page, size } })
