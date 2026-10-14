<template>
  <nav class="studio-menu">
    <div v-if="user.user" class="me">
      <el-avatar :size="44" :src="user.user.avatarUrl">{{ user.user.nickname.slice(0, 1) }}</el-avatar>
      <div class="info">
        <div class="name ellipsis">{{ user.user.nickname }}</div>
        <div class="muted">UID {{ user.user.id }}</div>
      </div>
    </div>
    <el-menu :default-active="active" router>
      <el-menu-item v-for="m in items" :key="m.to" :index="m.to">
        <el-icon><component :is="m.icon" /></el-icon>
        <span>{{ m.label }}</span>
        <el-badge v-if="m.to === '/messages' && notification.unread" :value="notification.unread" :max="99" class="count" />
        <el-badge v-if="m.to === '/chats' && chat.unread" :value="chat.unread" :max="99" class="count" />
      </el-menu-item>
    </el-menu>
  </nav>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useNotificationStore } from '@/stores/notification'
import { useChatStore } from '@/stores/chat'

const user = useUserStore()
const notification = useNotificationStore()
const chat = useChatStore()
const route = useRoute()

const items = computed(() => {
  const id = user.user?.id
  return [
    { to: '/me', label: '个人中心', icon: 'House' },
    { to: '/orders', label: '我的订单', icon: 'Tickets' },
    { to: '/chats', label: '私信', icon: 'ChatDotRound' },
    { to: '/messages', label: '消息', icon: 'Bell' },
    { to: '/studio/albums', label: '我的客片', icon: 'Picture' },
    { to: '/studio/profile', label: '接单设置', icon: 'Setting' },
    { to: `/p/${id}/schedule`, label: '我的档期', icon: 'Calendar' },
    { to: '/studio/stats', label: '经营数据', icon: 'DataLine' },
    { to: `/p/${id}`, label: '我的主页', icon: 'User' },
    ...(user.isAdmin ? [{ to: '/admin', label: '管理后台', icon: 'Management' }] : []),
  ]
})

/** 编辑资料归到「个人中心」下高亮 */
const active = computed(() => {
  if (route.path === '/me/edit') return '/me'
  if (route.path.startsWith('/orders/')) return '/orders'
  if (route.path.startsWith('/chats/')) return '/chats'
  return route.path
})
</script>

<style lang="scss" scoped>
.studio-menu { background: #fff; border-radius: var(--radius); padding: 12px 8px; }
.me { display: flex; align-items: center; gap: 10px; padding: 4px 8px 14px; margin-bottom: 6px; border-bottom: 1px solid #f0f0f0; }
.info { min-width: 0; }
.name { font-weight: 700; }
.el-menu { border-right: none; --el-menu-item-height: 44px; }
.count { margin-left: auto; display: flex; }
.el-menu-item { border-radius: 10px; margin: 2px 0; }
.el-menu-item.is-active { background: var(--brand-soft); color: var(--text); font-weight: 700; }
.el-menu-item.is-active::before { content: ''; position: absolute; left: 0; top: 12px; bottom: 12px; width: 3px; border-radius: 2px; background: var(--brand-strong); }
</style>
