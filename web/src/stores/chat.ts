import { defineStore } from 'pinia'
import { ref } from 'vue'
import { ElNotification } from 'element-plus/es/components/notification/index.mjs'
import { chatUnreadCount } from '@/api/chat'
import { getToken } from '@/utils/request'
import { useUserStore } from '@/stores/user'
import router from '@/router'
import type { ChatPush } from '@/types'

const PING_MS = 25 * 1000
const MAX_RETRY_MS = 30 * 1000

/** ws://当前域名/api/ws/chat，开发时由 Vite 代理、线上由 Nginx 转发 */
function socketUrl() {
  const base = import.meta.env.VITE_API_BASE || '/api'
  return `${location.protocol === 'https:' ? 'wss' : 'ws'}://${location.host}${base}/ws/chat`
}

/**
 * 私信：登录后保持一条 WebSocket 连接接收新消息，断线自动重连。
 * 发消息走 HTTP 接口；这里负责未读角标、新消息提醒，以及把推送分发给打开的聊天页。
 */
export const useChatStore = defineStore('chat', () => {
  const unread = ref(0)
  /** 正在看的会话，这个会话的新消息不算未读、不弹提醒 */
  const activeId = ref<number | null>(null)
  /** 每次（重新）连上加一，聊天页据此补拉断线期间漏掉的消息 */
  const connectedTimes = ref(0)

  const listeners = new Set<(p: ChatPush) => void>()
  let ws: WebSocket | null = null
  let running = false
  let retryMs = 1000
  let retryTimer: ReturnType<typeof setTimeout> | undefined
  let pingTimer: ReturnType<typeof setInterval> | undefined

  async function refresh() {
    try {
      unread.value = await chatUnreadCount()
    } catch {
      // 网络抖动时等下次
    }
  }

  function connect() {
    const token = getToken()
    if (!running || !token) return
    const sock = new WebSocket(socketUrl())
    ws = sock
    // 不把 token 放进 URL（会进 Nginx 日志），连上后第一条消息发过去
    sock.onopen = () => sock.send(JSON.stringify({ type: 'auth', token }))
    sock.onmessage = (e) => {
      let data: { type: string }
      try {
        data = JSON.parse(e.data)
      } catch {
        return
      }
      if (data.type === 'auth_ok') {
        retryMs = 1000
        connectedTimes.value++
        refresh()
        clearInterval(pingTimer)
        pingTimer = setInterval(() => sock.readyState === WebSocket.OPEN && sock.send('{"type":"ping"}'), PING_MS)
      } else if (data.type === 'message') {
        onPush(data as ChatPush)
      }
    }
    sock.onclose = (e) => {
      clearInterval(pingTimer)
      if (ws !== sock) return
      ws = null
      // 4401：登录已失效，不再重连
      if (!running || e.code === 4401) return
      retryTimer = setTimeout(connect, retryMs)
      retryMs = Math.min(retryMs * 2, MAX_RETRY_MS)
    }
  }

  function onPush(p: ChatPush) {
    listeners.forEach((fn) => fn(p))
    const me = useUserStore().user?.id
    if (p.message.senderId === me || p.conversationId === activeId.value) return
    unread.value++
    const text = p.message.type === 'IMAGE' ? '[图片]' : p.message.content
    const box = ElNotification({
      title: p.sender.nickname,
      message: text.length > 40 ? text.slice(0, 40) + '…' : text,
      type: 'info',
      duration: 5000,
      onClick: () => {
        box.close()
        router.push(`/chats/${p.conversationId}`)
      },
    })
  }

  /** 聊天页订阅新消息，返回取消订阅的函数 */
  function subscribe(fn: (p: ChatPush) => void) {
    listeners.add(fn)
    return () => listeners.delete(fn)
  }

  function start() {
    stop()
    running = true
    refresh()
    connect()
    document.addEventListener('visibilitychange', onVisible)
  }

  function stop() {
    running = false
    clearTimeout(retryTimer)
    clearInterval(pingTimer)
    const sock = ws
    ws = null
    sock?.close()
    document.removeEventListener('visibilitychange', onVisible)
    unread.value = 0
  }

  /** 手机切到后台时连接可能被系统断掉，切回来立即重连并刷新角标 */
  function onVisible() {
    if (document.visibilityState !== 'visible' || !running) return
    refresh()
    if (!ws) {
      clearTimeout(retryTimer)
      retryMs = 1000
      connect()
    }
  }

  /** 本地已读后同步角标 */
  function markRead(n: number) {
    unread.value = Math.max(0, unread.value - n)
  }

  return { unread, activeId, connectedTimes, refresh, start, stop, subscribe, markRead }
})
