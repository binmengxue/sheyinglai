<template>
  <!-- 首页和订单列表返回时保留数据和滚动位置；换账号时 key 变化，缓存整体丢弃 -->
  <RouterView v-slot="{ Component }">
    <KeepAlive :key="user.user?.id ?? 0" :include="CACHED">
      <component :is="Component" />
    </KeepAlive>
  </RouterView>
</template>

<script setup lang="ts">
import { useUserStore } from '@/stores/user'

/** 按组件名匹配（<script setup> 组件名取文件名） */
const CACHED = ['HomeView', 'OrdersView']
const user = useUserStore()
</script>
