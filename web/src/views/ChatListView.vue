<template>
  <NavBar fallback="/me" />
  <div class="chats">
    <div class="list">
      <RouterLink v-for="c in list" :key="c.id" :to="`/chats/${c.id}`" class="card item">
        <el-badge :value="c.unread" :hidden="!c.unread" :max="99">
          <el-avatar :size="46" :src="c.peer.avatarUrl">{{ c.peer.nickname.slice(0, 1) }}</el-avatar>
        </el-badge>
        <div class="body">
          <div class="top">
            <span class="name ellipsis">{{ c.peer.nickname }}</span>
            <span class="muted time">{{ fmtChatTime(c.lastAt, true) }}</span>
          </div>
          <p class="last ellipsis">{{ c.lastMsg }}</p>
        </div>
      </RouterLink>
    </div>
    <div v-if="loading && !list.length" v-loading="true" style="height: 120px" />
    <el-empty v-else-if="error && !list.length" description="加载失败"><el-button round @click="load">重试</el-button></el-empty>
    <el-empty v-else-if="!list.length" description="还没有私信，去摄影师主页点「聊一聊」吧" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import NavBar from '@/components/NavBar.vue'
import { listChats } from '@/api/chat'
import { useChatStore } from '@/stores/chat'
import { fmtChatTime } from '@/utils/format'
import type { ChatConversation } from '@/types'

const chat = useChatStore()
const list = ref<ChatConversation[]>([])
const loading = ref(false)
const error = ref(false)

async function load() {
  loading.value = true
  error.value = false
  try {
    list.value = await listChats()
  } catch {
    error.value = true
  } finally {
    loading.value = false
  }
}

// 有新消息时把对应会话顶到最前；新会话直接重新拉列表
const off = chat.subscribe((p) => {
  const i = list.value.findIndex((c) => c.id === p.conversationId)
  if (i < 0) return load()
  const [c] = list.value.splice(i, 1)
  c.lastMsg = p.message.type === 'IMAGE' ? '[图片]' : p.message.content
  c.lastAt = p.message.createdAt
  if (p.message.senderId === c.peer.id) c.unread++
  list.value.unshift(c)
})
onUnmounted(off)

onMounted(() => {
  load()
  chat.refresh()
})
</script>

<style lang="scss" scoped>
.list { padding: 12px; }
.item { display: flex; align-items: center; gap: 12px; margin-bottom: 10px; }
.body { flex: 1; min-width: 0; }
.top { display: flex; align-items: baseline; gap: 8px; }
.name { flex: 1; font-weight: 700; }
.time { flex-shrink: 0; font-size: 12px; }
.last { margin: 4px 0 0; color: var(--text-muted); font-size: 14px; }

@media (min-width: 768px) {
  .list { padding: 0; }
  .item:hover { background: #fafafa; }
}
</style>
