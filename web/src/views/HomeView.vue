<template>
  <div class="home">
    <header class="hero">
      <div class="brand">摄影来</div>
      <div class="slogan">找到附近的摄影师，约一场好看的拍摄</div>
    </header>

    <div class="filters">
      <div class="chips">
        <button class="chip" :class="{ active: !style }" @click="setStyle('')">全部</button>
        <button v-for="s in STYLES" :key="s" class="chip" :class="{ active: style === s }" @click="setStyle(s)">{{ s }}</button>
      </div>
      <div class="sorts">
        <span v-for="s in sorts" :key="s.key" class="sort" :class="{ active: sort === s.key }" @click="setSort(s.key)">{{ s.label }}</span>
      </div>
    </div>

    <div class="list">
      <PhotographerCard v-for="c in list" :key="c.id" :c="c" />
    </div>
    <div v-if="loading" v-loading="true" class="loading" />
    <el-empty v-else-if="error && !list.length" description="加载失败"><el-button round @click="load(true)">重试</el-button></el-empty>
    <el-empty v-else-if="!list.length" description="附近还没有摄影师，快来成为第一个吧">
      <el-button class="cta" round @click="$router.push('/studio/profile')">我是摄影师，去入驻</el-button>
    </el-empty>
    <div v-else class="more">
      <el-button v-if="list.length < total" round @click="load()">加载更多</el-button>
      <span v-else class="muted">没有更多了</span>
    </div>

    <footer class="footer">
      <span>© {{ new Date().getFullYear() }} 摄影来</span>
      <!-- 备案通过后必须在网站底部展示备案号并链接到工信部网站，把下面的文字换成你的备案号 -->
      <a href="https://beian.miit.gov.cn/" target="_blank" rel="noopener">苏ICP备XXXXXXXX号</a>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import PhotographerCard from '@/components/PhotographerCard.vue'
import { listPhotographers, type SortKey } from '@/api/photographer'
import { getLocation, STYLES } from '@/utils/format'
import { toast } from '@/utils/message'
import type { PhotographerCard as Card } from '@/types'

const PAGE_SIZE = 10
const sorts: { key: SortKey; label: string }[] = [
  { key: 'default', label: '综合' },
  { key: 'distance', label: '距离最近' },
  { key: 'rating', label: '评分最高' },
  { key: 'price', label: '价格最低' },
]

const list = ref<Card[]>([])
const total = ref(0)
const page = ref(0)
const loading = ref(false)
const error = ref(false)
const style = ref('')
const sort = ref<SortKey>('default')
let loc: { lat: number; lng: number } | null = null

async function load(reset = false) {
  if (reset) {
    list.value = []
    page.value = 0
  }
  loading.value = true
  error.value = false
  try {
    const res = await listPhotographers({
      style: style.value || undefined, sort: sort.value, ...(loc ?? {}), page: page.value + 1, size: PAGE_SIZE,
    })
    list.value.push(...res.records)
    total.value = res.total
    page.value++
  } catch {
    error.value = true
  } finally {
    loading.value = false
  }
}

function setStyle(s: string) {
  style.value = s
  load(true)
}

async function setSort(s: SortKey) {
  sort.value = s
  if (s === 'distance' && !loc) {
    loc = await getLocation()
    if (!loc) toast.info('未获取到定位，暂时无法按距离排序，请检查浏览器定位权限')
  }
  load(true)
}

onMounted(async () => {
  // 定位最多等几秒，失败也照常展示列表（只是没有距离）
  loc = await getLocation()
  load(true)
})
</script>

<style lang="scss" scoped>
.hero { padding: 28px 16px 56px; background: linear-gradient(160deg, #1f2329 0%, #3a3f47 100%); color: #fff; }
.brand { font-size: 26px; font-weight: 800; letter-spacing: 3px; }
.slogan { margin-top: 6px; font-size: 14px; opacity: 0.8; }
.filters {
  position: sticky; top: 0; z-index: 5; margin-top: -36px; padding: 12px 12px 8px;
  background: var(--bg); border-radius: 18px 18px 0 0;
}
.sorts { display: flex; gap: 18px; margin-top: 10px; font-size: 14px; color: #666; }
.sort { cursor: pointer; }
.sort.active { color: var(--text); font-weight: 700; }
.list { display: flex; flex-direction: column; gap: 10px; padding: 4px 12px; }
.loading { height: 120px; }
.more { text-align: center; padding: 16px; }
.footer { display: flex; justify-content: center; gap: 12px; padding: 16px; font-size: 12px; color: var(--text-muted); }

/* PC：矮横幅 + 两列卡片 */
@media (min-width: 768px) {
  .hero { padding: 28px 32px; border-radius: var(--radius); }
  .brand { font-size: 28px; }
  .filters { position: static; margin-top: 0; padding: 16px 0 8px; border-radius: 0; display: flex; align-items: center; gap: 16px; }
  .filters .chips { flex: 1; }
  .sorts { margin-top: 0; flex-shrink: 0; }
  .list { display: grid; grid-template-columns: repeat(auto-fill, minmax(520px, 1fr)); gap: 16px; padding: 8px 0; }
}
</style>
