<template>
  <NavBar fallback="/me">
    <template #right>
      <el-button v-if="notification.unread" link :loading="readingAll" @click="readAll">全部已读</el-button>
    </template>
  </NavBar>
  <div class="messages">

    <div class="list">
      <div v-for="n in list" :key="n.id" class="card item" :class="{ unread: !n.isRead }" @click="open(n)">
        <el-icon class="icon" :size="20"><component :is="ICONS[n.type]" /></el-icon>
        <div class="body">
          <div class="title">{{ n.title }}<i v-if="!n.isRead" class="dot" /></div>
          <p v-if="n.content" class="content">{{ n.content }}</p>
          <span class="muted time">{{ fmtDateTime(n.createdAt) }}</span>
        </div>
        <el-icon v-if="n.link" class="arrow"><ArrowRight /></el-icon>
      </div>
    </div>
    <div v-if="loading" v-loading="true" style="height: 120px" />
    <el-empty v-else-if="error && !list.length" description="加载失败"><el-button round @click="load(true)">重试</el-button></el-empty>
    <el-empty v-else-if="!list.length" description="暂时没有消息" />
    <div v-else-if="list.length < total" class="more"><el-button round @click="load()">加载更多</el-button></div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import NavBar from '@/components/NavBar.vue'
import { listNotifications, readAllNotifications, readNotification } from '@/api/notification'
import { useNotificationStore } from '@/stores/notification'
import { fmtDateTime } from '@/utils/format'
import { toast } from '@/utils/message'
import type { Notification } from '@/types'

const ICONS: Record<Notification['type'], string> = { BOOKING: 'Tickets', REVIEW: 'ChatDotRound', FOLLOW: 'User' }
const PAGE_SIZE = 20
const router = useRouter()
const notification = useNotificationStore()

const list = ref<Notification[]>([])
const total = ref(0)
const loading = ref(false)
const error = ref(false)
let page = 0

async function load(reset = false) {
  if (reset) {
    list.value = []
    page = 0
  }
  loading.value = true
  error.value = false
  try {
    const res = await listNotifications(page + 1, PAGE_SIZE)
    list.value.push(...res.records)
    total.value = res.total
    page++
  } catch {
    error.value = true
  } finally {
    loading.value = false
  }
}

/** 点击：标记已读并跳到相关页面 */
async function open(n: Notification) {
  if (!n.isRead) {
    n.isRead = true
    notification.markRead()
    readNotification(n.id).catch(() => undefined)
  }
  if (n.link) router.push(n.link)
}

const readingAll = ref(false)
async function readAll() {
  readingAll.value = true
  try {
    await readAllNotifications()
    list.value.forEach((n) => (n.isRead = true))
    notification.clear()
    toast.success('已全部标记为已读')
  } finally {
    readingAll.value = false
  }
}

onMounted(() => {
  load(true)
  notification.refresh()
})
</script>

<style lang="scss" scoped>
.list { padding: 12px; }
.item { display: flex; align-items: flex-start; gap: 10px; margin-bottom: 10px; cursor: pointer; }
.icon { flex-shrink: 0; margin-top: 2px; color: var(--text-muted); }
.unread .icon { color: var(--brand-strong); }
.body { flex: 1; min-width: 0; }
.title { display: flex; align-items: center; gap: 6px; font-weight: 700; }
.dot { width: 8px; height: 8px; border-radius: 50%; background: #e5483d; }
.content { margin: 4px 0; font-size: 14px; line-height: 1.5; word-break: break-all; }
.time { font-size: 12px; }
.arrow { align-self: center; color: #ccc; }
.more { text-align: center; padding: 12px; }

@media (min-width: 768px) {
  .list { padding: 0; }
}
</style>
