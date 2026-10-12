export interface Result<T> {
  code: number
  msg: string
  data: T
}

export interface PageResult<T> {
  records: T[]
  total: number
  page: number
  size: number
}

/** 相册 = 摄影师的一组客片 */
export interface Album {
  id: number
  userId: number
  title: string
  description?: string
  style?: string
  location?: string
  coverUrl?: string
  visible: boolean
  sort: number
  likeCount: number
  createdAt: string
  photoCount?: number
  liked?: boolean
}

export interface Photo {
  id: number
  albumId: number
  userId: number
  title?: string
  url: string
  thumbUrl: string
  width?: number
  height?: number
  size?: number
  createdAt: string
}

export type AlbumForm = Pick<Album, 'title' | 'description' | 'style' | 'location' | 'visible' | 'sort'>

export interface User {
  id: number
  phone: string
  nickname: string
  avatarUrl?: string
  /** 0 未知，1 男，2 女 */
  gender: number
  bio?: string
  city?: string
  wechat?: string
  role: 'USER' | 'ADMIN'
  status: 'NORMAL' | 'BANNED'
  createdAt: string
}

export type WorkDays = 'ALL' | 'WEEKDAY' | 'WEEKEND'
export type CancelPolicy = 'LOOSE' | 'NORMAL' | 'STRICT'

/** 摄影师接单资料，金额单位：元 */
export interface Photographer {
  userId: number
  tag?: '陪拍' | '专摄'
  pricePerHour?: number
  extraPersonPrice: number
  minHours: number
  freeKm: number
  kmFee: number
  rawCount: number
  retouchCount: number
  retouchExtraPrice: number
  retouchRule?: string
  makeupNote?: string
  equipment?: string
  cancelPolicy: CancelPolicy
  penaltyPercent: number
  refundPromise: boolean
  /** 逗号分隔 */
  styles?: string
  workDays: WorkDays
  /** HH:mm:ss */
  workStart: string
  workEnd: string
  address?: string
  /** 是否设置过接单位置（坐标本身不返回） */
  hasLocation: boolean
  ratingAvg: number
  ratingCount: number
  dealCount: number
  visible: boolean
}

export type PhotographerForm = Omit<Photographer,
  'userId' | 'styles' | 'hasLocation' | 'ratingAvg' | 'ratingCount' | 'dealCount' | 'visible'> & {
  styles: string[]
  lat?: number
  lng?: number
}

export interface Me {
  loggedIn: boolean
  user: User | null
  photographer: Photographer | null
  admin: boolean
}

export interface UserBrief {
  id: number
  nickname: string
  avatarUrl?: string
  gender: number
}

export interface AlbumBrief {
  id: number
  coverUrl: string
  photoCount: number
}

export interface PhotographerCard {
  id: number
  nickname: string
  avatarUrl?: string
  gender: number
  tag?: string
  ratingAvg: number
  ratingCount: number
  dealCount: number
  pricePerHour?: number
  minHours: number
  freeKm: number
  rawCount: number
  retouchCount: number
  styles: string[]
  workDays: WorkDays
  refundPromise: boolean
  distanceKm?: number
  recentConsult: number
  albums: AlbumBrief[]
}

export interface PhotographerDetail {
  card: PhotographerCard
  profile: Photographer
  bio?: string
  city?: string
  fans: number
  following: number
  followed: boolean
  self: boolean
  rank: number
  styleCounts: { style: string; count: number }[]
}

export interface Slot {
  start: string
  end: string
}

export type DayStatus = 'EXPIRED' | 'OFF' | 'FULL' | 'AVAILABLE'

export interface DayInfo {
  date: string
  status: DayStatus
  free: Slot[]
}

export interface ScheduleView {
  minHours: number
  workStart: string
  workEnd: string
  days: DayInfo[]
}

export type BookingStatus = 'PENDING' | 'CONFIRMED' | 'REJECTED' | 'CANCELLED' | 'COMPLETED'

export interface Booking {
  id: number
  customerId: number
  photographerId: number
  shootDate: string
  startTime: string
  hours: number
  people: number
  address?: string
  style?: string
  remark?: string
  customerContact: string
  priceEstimate: number
  status: BookingStatus
  cancelledBy?: number
  reviewed: boolean
  respondedAt?: string
  createdAt: string
}

export interface BookingView {
  booking: Booking
  customer: UserBrief
  photographer: UserBrief
  /** 摄影师确认后客户才能看到 */
  photographerContact?: string
}

export interface BookingForm {
  photographerId: number
  shootDate: string
  startTime: string
  hours: number
  people: number
  address?: string
  style?: string
  remark?: string
  contact: string
}

export interface Review {
  id: number
  bookingId: number
  customerId: number
  photographerId: number
  score: number
  content?: string
  createdAt: string
}

export interface ReviewView {
  review: Review
  user: UserBrief
}

export interface Metric {
  value: number
  dayChange?: number
  weekChange?: number
  monthChange?: number
}

export interface StatsView {
  avgPrice?: number
  maxPrice?: number
  dealRate?: number
  acceptRate?: number
  cancelRate?: number
  avgReplyMinutes?: number
  exposure: Metric
  profileView: Metric
  consult: Metric
  exposure30: number
  profileView30: number
  consult30: number
}

export interface AdminUserView {
  user: User
  photographer?: Photographer
}
