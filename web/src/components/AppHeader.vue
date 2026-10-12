<template>
  <header class="header">
    <div class="inner">
      <RouterLink to="/" class="logo">摄影来</RouterLink>
      <nav class="nav">
        <RouterLink to="/" :class="{ active: route.path === '/' }">首页</RouterLink>
        <RouterLink to="/orders" :class="{ active: route.path === '/orders' }">订单</RouterLink>
        <RouterLink to="/me" :class="{ active: route.meta.studio && route.path !== '/orders' }">工作台</RouterLink>
      </nav>
      <div class="right">
        <el-dropdown v-if="user.loggedIn && user.user" trigger="click" @command="onCommand">
          <span class="who">
            <el-avatar :size="32" :src="user.user.avatarUrl">{{ user.user.nickname.slice(0, 1) }}</el-avatar>
            <span class="name ellipsis">{{ user.user.nickname }}</span>
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="home" :icon="'User'">我的主页</el-dropdown-item>
              <el-dropdown-item command="studio" :icon="'Grid'">工作台</el-dropdown-item>
              <el-dropdown-item command="logout" :icon="'SwitchButton'" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <template v-else>
          <el-button round @click="toLogin('/login')">登录</el-button>
          <el-button round class="cta" @click="toLogin('/register')">注册</el-button>
        </template>
      </div>
    </div>
  </header>
</template>

<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const user = useUserStore()
const route = useRoute()
const router = useRouter()

function toLogin(path: string) {
  // 已经在登录/注册页时不要把自己当成 redirect
  const redirect = ['/login', '/register'].includes(route.path) ? route.query.redirect : route.fullPath
  router.push({ path, query: redirect ? { redirect } : undefined })
}

async function onCommand(cmd: string) {
  if (cmd === 'home') router.push(`/p/${user.user!.id}`)
  else if (cmd === 'studio') router.push('/me')
  else if (cmd === 'logout') {
    await user.logout()
    router.replace('/')
  }
}
</script>

<style lang="scss" scoped>
.header { position: sticky; top: 0; z-index: 30; height: var(--header-height); background: rgba(255, 255, 255, 0.96); backdrop-filter: blur(8px); border-bottom: 1px solid #eceef1; }
.inner { display: flex; align-items: center; gap: 40px; max-width: var(--page-width); height: 100%; margin: 0 auto; padding: 0 24px; }
.logo { font-size: 22px; font-weight: 800; letter-spacing: 3px; }
.nav { display: flex; gap: 28px; flex: 1; }
.nav a { padding: 18px 0 16px; color: #555; border-bottom: 3px solid transparent; }
.nav a:hover { color: var(--text); }
.nav a.active { color: var(--text); font-weight: 700; border-color: var(--brand-strong); }
.right { display: flex; align-items: center; gap: 8px; }
.who { display: flex; align-items: center; gap: 8px; cursor: pointer; outline: none; }
.name { max-width: 8em; font-size: 14px; }
</style>
