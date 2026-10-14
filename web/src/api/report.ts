import { request } from '@/utils/request'
import type { ReportTargetType } from '@/types'

export const REPORT_REASONS = ['虚假信息', '色情低俗', '广告骚扰', '侵犯版权', '辱骂攻击', '其他']

export const createReport = (data: { targetType: ReportTargetType; targetId: number; reason: string; content?: string }) =>
  request<void>({ url: '/reports', method: 'post', data })
