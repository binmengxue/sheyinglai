<template>
  <AppHeader />
  <main class="container main">
    <RouterView />
  </main>
  <footer class="footer">
    <span>© {{ new Date().getFullYear() }} 摄影来</span>
    <!-- 备案通过后必须在网站底部展示备案号并链接到工信部网站，把下面的文字换成你的备案号 -->
    <a href="https://beian.miit.gov.cn/" target="_blank" rel="noopener">苏ICP备XXXXXXXX号</a>
  </footer>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import AppHeader from '@/components/AppHeader.vue'
import { useUserStore } from '@/stores/user'

const user = useUserStore()
onMounted(() => user.fetchMe().catch(() => undefined))
</script>

<style lang="scss" scoped>
.main { min-height: calc(100vh - 140px); padding-bottom: 40px; }
.footer {
  display: flex; justify-content: center; gap: 16px;
  padding: 24px 16px; font-size: 13px; color: var(--text-muted); border-top: 1px solid #eee;
}
</style>
