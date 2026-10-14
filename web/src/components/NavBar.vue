<template>
  <!-- PC 工作台：左侧菜单负责导航，这里只是页面标题 -->
  <header v-if="isDesktop && route.meta.studio" class="desk-title">
    <h1>{{ title ?? route.meta.title }}</h1>
    <div class="right"><slot name="right" /></div>
  </header>
  <header v-else class="navbar" :class="{ dark, desk: isDesktop }">
    <button class="back" aria-label="返回" @click="back"><el-icon :size="20"><ArrowLeft /></el-icon></button>
    <h1 class="title ellipsis">{{ title ?? route.meta.title }}</h1>
    <div class="right"><slot name="right" /></div>
  </header>
</template>

<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { useDevice } from '@/composables/useDevice'

const props = defineProps<{ title?: string; fallback?: string; dark?: boolean }>()
const route = useRoute()
const router = useRouter()
const { isDesktop } = useDevice()

/** 直接打开的页面（没有站内上一页）返回到 fallback */
function back() {
  if (window.history.state?.back) router.back()
  else router.replace(props.fallback ?? '/')
}
</script>

<style lang="scss" scoped>
.navbar {
  position: sticky; top: 0; z-index: 10; display: flex; align-items: center;
  /* 刘海屏顶部安全区 */
  height: calc(50px + env(safe-area-inset-top)); padding: env(safe-area-inset-top) 8px 0;
  background: rgba(246, 247, 249, 0.94); backdrop-filter: blur(8px);
}
.navbar.dark { background: #111; color: #fff; }
.back { width: 40px; height: 40px; border: none; background: none; color: inherit; cursor: pointer; display: flex; align-items: center; justify-content: center; }
.title { flex: 1; margin: 0; text-align: center; font-size: 17px; font-weight: 700; }
.right { min-width: 40px; display: flex; justify-content: flex-end; }

/* PC 浏览页：顶部已有 AppHeader，这里只做「返回 + 标题」的面包屑，左对齐、不吸顶 */
.navbar.desk { position: static; height: auto; padding: 0 0 12px; background: none; backdrop-filter: none; }
.navbar.desk.dark { color: inherit; }
.navbar.desk .back { width: 32px; height: 32px; border-radius: 50%; background: #fff; color: var(--text); }
.navbar.desk .title { text-align: left; margin-left: 10px; font-size: 18px; }

/* PC 工作台标题 */
.desk-title { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.desk-title h1 { margin: 0; font-size: 22px; }
</style>
