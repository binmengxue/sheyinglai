<template>
  <header class="header">
    <div class="container inner">
      <RouterLink to="/" class="logo">摄影来</RouterLink>
      <nav class="nav">
        <RouterLink to="/" exact-active-class="active">最新作品</RouterLink>
        <RouterLink to="/albums" :class="{ active: $route.path.startsWith('/albums') }">相册</RouterLink>
      </nav>
      <div class="right">
        <el-dropdown v-if="user.loggedIn" @command="onLogout">
          <span class="user"><el-icon><User /></el-icon> 管理员</span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <RouterLink v-else to="/login" class="muted">登录</RouterLink>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { useUserStore } from '@/stores/user'

const user = useUserStore()

async function onLogout() {
  await user.logout()
  location.href = '/' // 整页刷新，确保不再显示未公开相册的内容
}
</script>

<style lang="scss" scoped>
.header { position: sticky; top: 0; z-index: 10; background: rgba(255, 255, 255, 0.92); backdrop-filter: blur(8px); border-bottom: 1px solid #eee; }
.inner { display: flex; align-items: center; height: 60px; gap: 32px; }
.logo { font-size: 20px; font-weight: 700; letter-spacing: 2px; }
.nav { display: flex; gap: 24px; flex: 1; }
.nav a { color: #555; padding: 4px 0; border-bottom: 2px solid transparent; }
.nav a.active { color: #111; border-color: #111; }
.user { display: inline-flex; align-items: center; gap: 4px; cursor: pointer; outline: none; }
@media (max-width: 600px) { .inner { gap: 16px; } .nav { gap: 14px; } }
</style>
