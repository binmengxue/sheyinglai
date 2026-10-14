<template>
  <nav class="tabbar">
    <RouterLink v-for="t in tabs" :key="t.key" :to="t.to" class="tab" :class="{ active: active === t.key }">
      <el-badge :value="notification.unread" :hidden="t.key !== 'me' || !notification.unread" :max="99">
        <el-icon :size="22"><component :is="t.icon" /></el-icon>
      </el-badge>
      <span>{{ t.label }}</span>
    </RouterLink>
  </nav>
</template>

<script setup lang="ts">
import { useNotificationStore } from '@/stores/notification'

defineProps<{ active: 'home' | 'orders' | 'me' }>()
const notification = useNotificationStore()

const tabs = [
  { key: 'home', to: '/', label: '首页', icon: 'House' },
  { key: 'orders', to: '/orders', label: '订单', icon: 'Tickets' },
  { key: 'me', to: '/me', label: '我', icon: 'User' },
] as const
</script>

<style lang="scss" scoped>
.tabbar {
  position: fixed; bottom: 0; left: 50%; transform: translateX(-50%); z-index: 20;
  display: flex; width: 100%; max-width: var(--app-width);
  height: calc(var(--tab-height) + env(safe-area-inset-bottom)); padding-bottom: env(safe-area-inset-bottom);
  background: rgba(255, 255, 255, 0.96); backdrop-filter: blur(8px); border-top: 1px solid #eee;
}
.tab { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 2px; font-size: 12px; color: #666; }
.tab.active { color: var(--text); font-weight: 700; }
.tab .el-badge { line-height: 1; }
.tab.active .el-icon { background: var(--brand); border-radius: 8px; }
</style>
