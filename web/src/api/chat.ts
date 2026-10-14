import { request } from '@/utils/request'
import type { ChatConversation, ChatMessage } from '@/types'

/** 在别人主页点「聊一聊」：找到或创建和 TA 的会话 */
export const openChat = (peerId: number) =>
  request<ChatConversation>({ url: '/me/chats/open', method: 'post', params: { peerId } })

export const listChats = () => request<ChatConversation[]>({ url: '/me/chats' })

export const getChat = (id: number) => request<ChatConversation>({ url: `/me/chats/${id}` })

/** 后台刷新角标用，失败不弹提示 */
export const chatUnreadCount = () => request<number>({ url: '/me/chats/unread-count', silent: true })

/** 历史消息（正序），before 为空时取最新的 */
export const listChatMessages = (id: number, before?: number, size = 30) =>
  request<ChatMessage[]>({ url: `/me/chats/${id}/messages`, params: { before, size } })

export const sendChatText = (id: number, content: string) =>
  request<ChatMessage>({ url: `/me/chats/${id}/messages`, method: 'post', data: { content } })

export function sendChatImage(id: number, file: File) {
  const form = new FormData()
  form.append('file', file)
  return request<ChatMessage>({ url: `/me/chats/${id}/images`, method: 'post', data: form, timeout: 60 * 1000 })
}

export const readChat = (id: number) => request<void>({ url: `/me/chats/${id}/read`, method: 'post', silent: true })
