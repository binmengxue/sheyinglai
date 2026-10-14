import { request } from '@/utils/request'
import type { Booking, BookingForm, CancelPreview, BookingStatus, BookingView, PageResult, Review, ReviewView, StatsView } from '@/types'

export const createBooking = (data: BookingForm) => request<Booking>({ url: '/bookings', method: 'post', data })

/** role：customer 我约的 / photographer 约我的 */
export const listBookings = (params: { role: 'customer' | 'photographer'; status?: BookingStatus; page?: number; size?: number }) =>
  request<PageResult<BookingView>>({ url: '/bookings', params })

export const getBooking = (id: number) => request<BookingView>({ url: `/bookings/${id}` })

export type BookingAction = 'confirm' | 'reject' | 'cancel' | 'complete'
export const bookingAction = (id: number, action: Exclude<BookingAction, 'cancel'>) =>
  request<BookingView>({ url: `/bookings/${id}/${action}`, method: 'post' })

/** 取消前预览违约金 */
export const cancelPreview = (id: number) => request<CancelPreview>({ url: `/bookings/${id}/cancel-preview` })

/** acceptPenalty：有违约金时需要用户确认后传 true */
export const cancelBooking = (id: number, data: { reason?: string; acceptPenalty?: boolean }) =>
  request<BookingView>({ url: `/bookings/${id}/cancel`, method: 'post', data })

/** 客户改期，改完回到待确认 */
export const rescheduleBooking = (id: number, data: { shootDate: string; startTime: string; hours: number }) =>
  request<BookingView>({ url: `/bookings/${id}/reschedule`, method: 'post', data })

export const reviewBooking = (id: number, score: number, content?: string) =>
  request<Review>({ url: `/bookings/${id}/review`, method: 'post', data: { score, content } })

export const myReviews = (page = 1, size = 20) =>
  request<PageResult<ReviewView>>({ url: '/me/reviews', params: { page, size } })

export const myStats = () => request<StatsView>({ url: '/me/stats' })
