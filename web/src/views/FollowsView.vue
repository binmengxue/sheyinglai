<template>
  <NavBar :title="tab === 'fans' ? '粉丝' : '关注'" :fallback="`/p/${id}`" />
  <div class="page">
    <div class="tabs">
      <button :class="{ active: tab === 'fans' }" @click="setTab('fans')">粉丝</button>
      <button :class="{ active: tab === 'following' }" @click="setTab('following')">关注</button>
    </div>
    <div class="card list">
      <RouterLink v-for="u in list" :key="u.id ?? 0" :to="u.id ? `/p/${u.id}` : ''" class="row">
        <el-avatar :size="40" :src="u.avatarUrl">{{ u.nickname.slice(0, 1) }}</el-avatar>
        <span class="name ellipsis">{{ u.nickname }}</span>
        <el-icon class="arrow"><ArrowRight /></el-icon>
      </RouterLink>
      <div v-if="loading" v-loading="true" style="height: 80px" />
      <el-empty v-else-if="error && !list.length" description="加载失败"><el-button round @click="load(true)">重试</el-button></el-empty>
      <el-empty v-else-if="!list.length" :description="tab === 'fans' ? '还没有粉丝' : '还没有关注任何人'" />
      <div v-else-if="list.length < total" class="more"><el-button round @click="load()">加载更多</el-button></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import NavBar from '@/components/NavBar.vue'
import { listFans, listFollowing } from '@/api/photographer'
import { usePagedList } from '@/composables/usePagedList'
import type { UserBrief } from '@/types'

type Tab = 'fans' | 'following'
const props = defineProps<{ id: string }>()
const route = useRoute()
const router = useRouter()

const tab = ref<Tab>(route.query.tab === 'following' ? 'following' : 'fans')
const { list, total, loading, error, load } = usePagedList<UserBrief>((page) =>
  tab.value === 'fans' ? listFans(Number(props.id), page) : listFollowing(Number(props.id), page))

function setTab(t: Tab) {
  tab.value = t
  router.replace({ query: { tab: t } })
  load(true)
}

watch(() => props.id, () => load(true), { immediate: true })
</script>

<style lang="scss" scoped>
.page { padding: 12px; }
.tabs { display: flex; gap: 24px; margin-bottom: 12px; }
.tabs button { border: none; background: none; padding: 4px 0; font-size: 16px; color: #888; cursor: pointer; }
.tabs button.active { color: var(--text); font-weight: 700; box-shadow: inset 0 -4px var(--brand); }
.list { padding: 4px 14px; }
.row { display: flex; align-items: center; gap: 12px; padding: 10px 0; border-bottom: 1px solid #f2f2f2; }
.row:last-of-type { border-bottom: none; }
.name { flex: 1; min-width: 0; font-weight: 600; }
.arrow { color: #ccc; }
.more { text-align: center; padding: 12px; }
@media (min-width: 768px) {
  .page { padding: 0; max-width: 560px; }
}
</style>
