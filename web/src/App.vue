<template>
  <el-config-provider :locale="zhCn">
    <!-- 手机：单列 + 底部 Tab -->
    <div v-if="!isDesktop" class="app" :class="{ 'with-tab': route.meta.tab }">
      <CachedRouterView />
      <TabBar v-if="route.meta.tab" :active="route.meta.tab" />
    </div>

    <!-- PC：顶部导航；工作台页面再加左侧菜单（网盘式） -->
    <div v-else class="desk">
      <AppHeader />
      <div v-if="route.meta.studio" class="studio">
        <StudioMenu class="menu" />
        <main class="content"><CachedRouterView /></main>
      </div>
      <main v-else class="wide"><CachedRouterView /></main>
    </div>
  </el-config-provider>
</template>

<script setup lang="ts">
import { watch } from 'vue'
import { useRoute } from 'vue-router'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import TabBar from '@/components/TabBar.vue'
import CachedRouterView from '@/components/CachedRouterView.vue'
import AppHeader from '@/components/AppHeader.vue'
import StudioMenu from '@/components/StudioMenu.vue'
import { useDevice } from '@/composables/useDevice'
import { useUserStore } from '@/stores/user'
import { useNotificationStore } from '@/stores/notification'

const route = useRoute()
const { isDesktop } = useDevice()

// 登录后开始轮询未读消息，退出后停止
const user = useUserStore()
const notification = useNotificationStore()
watch(() => user.loggedIn, (on) => (on ? notification.start() : notification.stop()), { immediate: true })
</script>

<style lang="scss" scoped>
/* 手机版式 */
.app { position: relative; max-width: var(--app-width); min-height: 100vh; min-height: 100dvh; margin: 0 auto; background: var(--bg); }
.app.with-tab { padding-bottom: calc(var(--tab-height) + env(safe-area-inset-bottom)); }

/* PC 版式 */
.desk { min-height: 100vh; background: var(--bg); }
.wide { max-width: var(--page-width); margin: 0 auto; padding: 20px 24px 40px; }
.studio { display: flex; gap: 20px; max-width: var(--page-width); margin: 0 auto; padding: 20px 24px 40px; }
.menu { position: sticky; top: calc(var(--header-height) + 20px); align-self: flex-start; flex-shrink: 0; width: 200px; }
.content { flex: 1; min-width: 0; }
</style>
