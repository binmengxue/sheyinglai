import { ref } from 'vue'
import { bookingAction, cancelBooking, cancelPreview, type BookingAction } from '@/api/booking'
import { confirm, prompt, toast } from '@/utils/message'
import type { BookingView } from '@/types'

const ACTION_TEXT: Record<Exclude<BookingAction, 'cancel'>, string> = {
  confirm: '确认接这个单？确认后客户可以看到你的联系方式',
  reject: '拒绝这个预约？',
  complete: '确认已完成拍摄？完成后客户可以评价',
}

const ACTION_DONE: Record<BookingAction, string> = {
  confirm: '已接单，客户会收到通知',
  reject: '已拒绝该预约',
  cancel: '预约已取消',
  complete: '已标记拍摄完成',
}

/** 订单列表和订单详情共用的操作：确认、拒绝、完成、取消（带原因和违约金确认） */
export function useBookingActions() {
  /** 正在处理的订单 id，按钮显示 loading，防止重复点击 */
  const acting = ref<number>()

  async function act(v: BookingView, action: BookingAction) {
    if (acting.value) return
    const id = v.booking.id
    let reason: string | undefined
    let acceptPenalty = false
    if (action === 'cancel') {
      const pre = await cancelPreview(id)
      acceptPenalty = pre.penalty > 0
      const msg = acceptPenalty
        ? `按摄影师的取消规则，现在取消需支付违约金 ¥${pre.penalty}（线下结算）。确定要取消吗？`
        : '取消这个预约？建议先和对方沟通好'
      const input = await prompt(msg, { title: '取消预约', placeholder: '取消原因（选填，会告诉对方）', confirmText: '确定取消', type: 'warning' })
      if (input === null) return
      reason = input || undefined
    } else if (!(await confirm(ACTION_TEXT[action], { type: action === 'reject' ? 'warning' : 'info' }))) {
      return
    }
    acting.value = id
    try {
      const fresh = action === 'cancel' ? await cancelBooking(id, { reason, acceptPenalty }) : await bookingAction(id, action)
      Object.assign(v, fresh)
      toast.success(ACTION_DONE[action])
    } finally {
      acting.value = undefined
    }
  }

  return { acting, act }
}
