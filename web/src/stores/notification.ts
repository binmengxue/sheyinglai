import { defineStore } from 'pinia'
import { ref } from 'vue'
import { isAxiosError } from 'axios'
import { ElNotification } from 'element-plus'
import { unreadCount } from '@/api/notification'
import router from '@/router'

const POLL_MS = 60 * 1000

/** 未读消息数：登录后定时轮询，数量变多时右上角弹出提醒 */
export const useNotificationStore = defineStore('notification', () => {
  const unread = ref(0)
  let timer: ReturnType<typeof setInterval> | undefined
  /** 第一次取到数量时不弹提醒，只在之后新增时提醒 */
  let initialized = false

  async function refresh() {
    try {
      const n = await unreadCount()
      if (initialized && n > unread.value) {
        const added = n - unread.value
        const box = ElNotification({
          title: '新消息',
          message: `你有 ${added} 条新消息，点击查看`,
          type: 'info',
          duration: 5000,
          onClick: () => {
            box.close()
            router.push('/messages')
          },
        })
      }
      unread.value = n
      initialized = true
    } catch (e) {
      // 登录失效就不再轮询；网络抖动时等下次
      if (isAxiosError(e) && e.response?.status === 401) stop()
    }
  }

  function start() {
    stop()
    initialized = false
    refresh()
    timer = setInterval(() => {
      if (document.visibilityState === 'visible') refresh()
    }, POLL_MS)
    document.addEventListener('visibilitychange', onVisible)
  }

  function stop() {
    if (timer) clearInterval(timer)
    timer = undefined
    document.removeEventListener('visibilitychange', onVisible)
    unread.value = 0
  }

  /** 切回页面时立即刷新一次 */
  function onVisible() {
    if (document.visibilityState === 'visible') refresh()
  }

  /** 本地已读后同步角标，不等下次轮询 */
  function markRead(n = 1) {
    unread.value = Math.max(0, unread.value - n)
  }

  function clear() {
    unread.value = 0
  }

  return { unread, refresh, start, stop, markRead, clear }
})
