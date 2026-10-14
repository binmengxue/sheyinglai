import type { BookingStatus, CancelPolicy, DayStatus, WorkDays } from '@/types'

/** 常用拍摄类型，摄影师也可以自己输入其它类型 */
export const STYLES = ['随拍', '人像写真', '领证照', '情侣写真', '毕业照', '婚纱照', '亲子', '闺蜜照', '证件照', '活动跟拍']

export const WORK_DAYS: Record<WorkDays, string> = { ALL: '整周', WEEKDAY: '工作日', WEEKEND: '周末' }

/** 头像下方的小徽章：整周 可接 / 周末 只接 */
export const workDaysBadge = (w: WorkDays): [string, string] =>
  w === 'WEEKEND' ? ['周末', '只接'] : [WORK_DAYS[w] ?? '整周', '可接']

export const CANCEL_POLICY: Record<CancelPolicy, { label: string; desc: (p: number) => string }> = {
  LOOSE: { label: '宽松', desc: () => '拍摄前随时可免费取消' },
  NORMAL: { label: '一般', desc: (p) => (p ? `拍摄前 24 小时内取消，扣除 ${p}% 违约金` : '拍摄前 24 小时外可免费取消') },
  STRICT: { label: '非常严格', desc: (p) => `任何时段取消订单，扣除 ${p}% 违约金` },
}

export const BOOKING_STATUS: Record<BookingStatus, { label: string; type: 'warning' | 'success' | 'info' | 'danger' | 'primary' }> = {
  PENDING: { label: '待确认', type: 'warning' },
  CONFIRMED: { label: '已确认', type: 'success' },
  COMPLETED: { label: '已完成', type: 'primary' },
  CANCELLED: { label: '已取消', type: 'info' },
  REJECTED: { label: '已拒绝', type: 'danger' },
  EXPIRED: { label: '已过期', type: 'info' },
}

export const DAY_STATUS: Record<DayStatus, string> = { EXPIRED: '已过期', OFF: '休息', FULL: '约满', AVAILABLE: '可约' }

export function ratingLabel(r: number, count: number) {
  if (!count) return '暂无评分'
  if (r >= 4.8) return '超棒'
  if (r >= 4.5) return '很好'
  if (r >= 4) return '不错'
  return ''
}

export const genderMark = (g?: number) => (g === 2 ? '♀' : g === 1 ? '♂' : '')

/** 10:00:00 -> 10:00 */
export const hm = (t?: string) => (t ? t.slice(0, 5) : '')

/** 本地日期 yyyy-MM-dd（不能用 toISOString，那是 UTC） */
export function ymd(d = new Date()) {
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`
}

/** 2026-10-12T09:54:54.195 -> 2026-10-12 09:54 */
export const fmtDateTime = (s?: string) => (s ? s.replace('T', ' ').slice(0, 16) : '')

/** 私信时间：今天只显示 12:30，今年显示 10-12 12:30，更早显示完整日期；short 时不带时分（会话列表用） */
export function fmtChatTime(s?: string, short = false) {
  if (!s) return ''
  const t = s.replace('T', ' ')
  const today = new Date()
  const pad = (n: number) => String(n).padStart(2, '0')
  const ymd = `${today.getFullYear()}-${pad(today.getMonth() + 1)}-${pad(today.getDate())}`
  if (t.startsWith(ymd)) return t.slice(11, 16)
  if (t.startsWith(String(today.getFullYear()))) return short ? t.slice(5, 10) : t.slice(5, 16)
  return short ? t.slice(0, 10) : t.slice(0, 16)
}

const WEEK = ['日', '一', '二', '三', '四', '五', '六']
/** 2026-10-12 -> 10月12日 周一 */
export function fmtDay(s: string) {
  const [y, m, d] = s.split('-').map(Number)
  return `${m}月${d}日 周${WEEK[new Date(y, m - 1, d).getDay()]}`
}

/** 10:00 + 2 小时 -> 12:00 */
export function addHours(t: string, h: number) {
  const [hh, mm] = t.split(':').map(Number)
  const total = hh * 60 + mm + h * 60
  return `${String(Math.floor(total / 60)).padStart(2, '0')}:${String(total % 60).padStart(2, '0')}`
}

export const toMinutes = (t: string) => {
  const [hh, mm] = t.split(':').map(Number)
  return hh * 60 + mm
}

const LOC_KEY = 'photo_location'

/** 同步读取本次会话已缓存的定位：undefined 表示还没定位过，null 表示定位失败 */
export function peekLocation(): { lat: number; lng: number } | null | undefined {
  try {
    const cached = sessionStorage.getItem(LOC_KEY)
    if (cached) return cached === 'none' ? null : JSON.parse(cached)
  } catch { /* 存储不可用 */ }
  return undefined
}

/**
 * 获取当前位置用于算距离，结果在本次会话内缓存。
 * 浏览器只在 HTTPS 或 localhost 下允许定位，用户拒绝或超时返回 null，不影响正常浏览。
 */
export async function getLocation(): Promise<{ lat: number; lng: number } | null> {
  try {
    const cached = sessionStorage.getItem(LOC_KEY)
    if (cached) return cached === 'none' ? null : JSON.parse(cached)
  } catch { /* 存储不可用时直接定位 */ }
  const loc = await new Promise<{ lat: number; lng: number } | null>((resolve) => {
    if (!navigator.geolocation) return resolve(null)
    navigator.geolocation.getCurrentPosition(
      (p) => resolve({ lat: p.coords.latitude, lng: p.coords.longitude }),
      () => resolve(null),
      { timeout: 6000, maximumAge: 10 * 60 * 1000 },
    )
  })
  try { sessionStorage.setItem(LOC_KEY, loc ? JSON.stringify(loc) : 'none') } catch { /* 忽略 */ }
  return loc
}
