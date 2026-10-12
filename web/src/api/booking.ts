import { request } from '@/utils/request'
import type { Booking, BookingForm, BookingStatus, BookingView, PageResult, Review, ReviewView, StatsView } from '@/types'

export const createBooking = (data: BookingForm) => request<Booking>({ url: '/bookings', method: 'post', data })

/** role：customer 我约的 / photographer 约我的 */
export const listBookings = (params: { role: 'customer' | 'photographer'; status?: BookingStatus; page?: number; size?: number }) =>
  request<PageResult<BookingView>>({ url: '/bookings', params })

export type BookingAction = 'confirm' | 'reject' | 'cancel' | 'complete'
export const bookingAction = (id: number, action: BookingAction) =>
  request<BookingView>({ url: `/bookings/${id}/${action}`, method: 'post' })

export const reviewBooking = (id: number, score: number, content?: string) =>
  request<Review>({ url: `/bookings/${id}/review`, method: 'post', data: { score, content } })

export const myReviews = (page = 1, size = 20) =>
  request<PageResult<ReviewView>>({ url: '/me/reviews', params: { page, size } })

export const myStats = () => request<StatsView>({ url: '/me/stats' })
